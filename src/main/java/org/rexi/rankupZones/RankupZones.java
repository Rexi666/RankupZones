package org.rexi.rankupZones;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RankupZones extends JavaPlugin implements Listener {

    private Set<String> disabledWorlds;
    private Map<String, Map<String, RegionConfig>> regionConfigs;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnMobsInRegions();
            }
        }.runTaskTimer(this, 20L, 20L * 15);
    }

    private void loadConfig() {
        disabledWorlds = new HashSet<>(getConfig().getStringList("disabled-worlds"));

        regionConfigs = new HashMap<>();
        ConfigurationSection worldSection = getConfig().getConfigurationSection("region-whitelist");
        if (worldSection != null) {
            for (String worldName : worldSection.getKeys(false)) {
                ConfigurationSection regionsSection = worldSection.getConfigurationSection(worldName);
                Map<String, RegionConfig> regions = new HashMap<>();

                for (String regionName : regionsSection.getKeys(false)) {
                    ConfigurationSection regionData = regionsSection.getConfigurationSection(regionName);
                    List<EntityType> entityTypes = new ArrayList<>();
                    for (String mob : regionData.getStringList("mobs")) {
                        try {
                            entityTypes.add(EntityType.valueOf(mob.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("Entidad inválida en la configuración: " + mob);
                        }
                    }
                    int maxCount = regionData.getInt("max-count", 20);
                    String requiredPermission = regionData.getString("required-permission", null);
                    ConfigurationSection dropsSection = regionData.getConfigurationSection("custom-drops");
                    Map<EntityType, List<ItemStack>> customDrops = new HashMap<>();

                    if (dropsSection != null) {
                        for (String mobName : dropsSection.getKeys(false)) {
                            EntityType type;
                            try {
                                type = EntityType.valueOf(mobName.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                getLogger().warning("Entidad inválida en custom-drops: " + mobName);
                                continue;
                            }

                            List<ItemStack> drops = new ArrayList<>();
                            List<Map<?, ?>> itemList = dropsSection.getMapList(mobName);
                            for (Map<?, ?> itemDataRaw : itemList) {
                                Map<String, Object> itemData = (Map<String, Object>) itemDataRaw;

                                Material mat = Material.getMaterial(((String) itemData.get("material")).toUpperCase());
                                if (mat == null) continue;

                                int amount = (int) itemData.getOrDefault("amount", 1);
                                ItemStack item = new ItemStack(mat, amount);
                                ItemMeta meta = item.getItemMeta();

                                if (itemData.containsKey("name")) {
                                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', (String) itemData.get("name")));
                                }

                                if (itemData.containsKey("lore")) {
                                    List<String> loreRaw = (List<String>) itemData.get("lore");
                                    List<String> lore = new ArrayList<>();
                                    for (String line : loreRaw) {
                                        lore.add(ChatColor.translateAlternateColorCodes('&', line));
                                    }
                                    meta.setLore(lore);
                                }

                                if ((boolean) itemData.getOrDefault("enchanted", false)) {
                                    meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                                }

                                item.setItemMeta(meta);
                                drops.add(item);
                            }
                            customDrops.put(type, drops);
                        }
                    }
                    regions.put(regionName, new RegionConfig(entityTypes, maxCount, requiredPermission, customDrops));
                }
                regionConfigs.put(worldName, regions);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        Location location = event.getEntity().getLocation();
        String regionId = getRegionAt(location);

        if (regionId != null) {
            String worldName = location.getWorld().getName();
            RegionConfig config = regionConfigs.getOrDefault(worldName, Collections.emptyMap()).get(regionId);

            if (config != null && config.requiredPermission != null && !player.hasPermission(config.requiredPermission)) {
                player.sendMessage(ChatColor.RED + "No tienes permiso para dañar entidades en esta zona.");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        String worldName = entity.getWorld().getName();

        // Solo controlar el spawn si el mundo está en la lista de mundos controlados
        if (!disabledWorlds.contains(worldName)) {
            return;
        }

        String region = getRegionAt(entity.getLocation());
        if (region == null) {
            event.setCancelled(true);
            return;
        }

        RegionConfig config = regionConfigs.getOrDefault(worldName, Collections.emptyMap()).get(region);
        if (config != null && config.allowedMobs.contains(entity.getType())) {
            entity.setAI(false);
            return;
        }

        event.setCancelled(true);
    }

    private void spawnMobsInRegions() {
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            Map<String, RegionConfig> configs = regionConfigs.get(worldName);
            boolean isDisabledWorld = disabledWorlds.contains(worldName);

            if (isDisabledWorld) {
                // Si es un mundo deshabilitado, solo spawnear en regiones whitelist
                if (configs == null || configs.isEmpty()) continue;

                RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                if (regionManager == null) continue;

                for (Map.Entry<String, RegionConfig> entry : configs.entrySet()) {
                    String regionId = entry.getKey();
                    RegionConfig config = entry.getValue();

                    ProtectedRegion region = regionManager.getRegion(regionId);
                    if (region == null) continue;

                    int currentCount = countEntitiesInRegion(world, regionId, config.allowedMobs);
                    int toSpawn = Math.min(5, config.maxCount - currentCount);
                    if (toSpawn <= 0) continue;

                    spawnMobsInRegion(world, region, config, toSpawn);
                }
            } else {
                // Si NO es un mundo deshabilitado, spawnear sin importar regiones si hay config
                if (configs == null || configs.isEmpty()) continue;

                for (RegionConfig config : configs.values()) {
                    int currentCount = countEntitiesOfTypes(world, config.allowedMobs);
                    int toSpawn = Math.min(5, config.maxCount - currentCount);
                    if (toSpawn <= 0) continue;

                    for (int i = 0; i < toSpawn; i++) {
                        EntityType type = config.getRandomMobType();
                        Location loc = getRandomLocationInWorld(world);
                        world.spawnEntity(loc, type);
                    }
                }
            }
        }
    }

    private int countEntitiesInRegion(World world, String regionId, List<EntityType> allowedTypes) {
        return (int) world.getEntities().stream()
                .filter(e -> e instanceof LivingEntity && !e.isDead())
                .filter(e -> regionId.equals(getRegionAt(e.getLocation())))
                .filter(e -> allowedTypes.contains(e.getType()))
                .count();
    }

    private int countEntitiesOfTypes(World world, List<EntityType> types) {
        return (int) world.getEntities().stream()
                .filter(e -> e instanceof LivingEntity && !e.isDead() && types.contains(e.getType()))
                .count();
    }

    private void spawnMobsInRegion(World world, ProtectedRegion region, RegionConfig config, int toSpawn) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        for (int i = 0; i < toSpawn; i++) {
            EntityType type = config.getRandomMobType();
            int minX = min.x();
            int minY = min.y();
            int minZ = min.z();

            int maxX = max.x();
            int maxY = max.y();
            int maxZ = max.z();

            double x = minX + Math.random() * (maxX - minX);
            double y = minY;
            double z = minZ + Math.random() * (maxZ - minZ);

            Location loc = new Location(world, x, y, z);
            while (loc.getBlock().getType().isAir() && loc.getY() > 1) {
                loc.subtract(0, 1, 0);
            }

            LivingEntity entity = (LivingEntity) world.spawnEntity(loc.add(0, 1, 0), type);
            entity.setAI(false);
            entity.setRemoveWhenFarAway(false);
        }
    }

    private Location getRandomLocationInWorld(World world) {
        int x = ThreadLocalRandom.current().nextInt(-1000, 1000);
        int z = ThreadLocalRandom.current().nextInt(-1000, 1000);
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y + 1, z);
    }

    private double randomInRange(int min, int max) {
        return min + Math.random() * (max - min + 1);
    }

    private String getRegionAt(Location loc) {
        RegionManager regions = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld()));
        if (regions == null) return null;

        ApplicableRegionSet regionSet = regions.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
        for (ProtectedRegion region : regionSet) {
            return region.getId();
        }
        return null;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        String region = getRegionAt(entity.getLocation());

        if (region != null) {
            String worldName = entity.getWorld().getName();
            RegionConfig config = regionConfigs.getOrDefault(worldName, Collections.emptyMap()).get(region);

            if (config != null && config.customDrops.containsKey(entity.getType())) {
                event.getDrops().clear();
                event.getDrops().addAll(config.customDrops.get(entity.getType()));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        String fromRegion = getRegionAt(event.getFrom());
        String toRegion = getRegionAt(event.getTo());

        if (toRegion != null && !toRegion.equals(fromRegion)) {
            String worldName = event.getTo().getWorld().getName();
            RegionConfig config = regionConfigs.getOrDefault(worldName, Collections.emptyMap()).get(toRegion);

            if (config != null && config.requiredPermission != null && !event.getPlayer().hasPermission(config.requiredPermission)) {
                event.getPlayer().sendMessage(ChatColor.RED + "No tienes permiso para entrar en esta zona.");
                event.setCancelled(true);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rz")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuración recargada correctamente.");
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Uso: /rz reload");
                return true;
            }
        }
        return false;
    }

    private static class RegionConfig {
        List<EntityType> allowedMobs;
        int maxCount;
        String requiredPermission;
        Map<EntityType, List<ItemStack>> customDrops;

        public RegionConfig(List<EntityType> allowedMobs, int maxCount, String requiredPermission, Map<EntityType, List<ItemStack>> customDrops) {
            this.allowedMobs = allowedMobs;
            this.maxCount = maxCount;
            this.requiredPermission = requiredPermission;
            this.customDrops = customDrops;
        }

        public EntityType getRandomMobType() {
            return allowedMobs.get(new Random().nextInt(allowedMobs.size()));
        }
    }
}