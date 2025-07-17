package org.rexi.rankupZones;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class RankupZones extends JavaPlugin implements Listener {

    private Set<String> disabledWorlds;
    private Map<String, Map<String, RegionConfig>> regionWhitelist;

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
        }.runTaskTimer(this, 20L, 20L * 60); // Cada 60 segundos
    }

    private void loadConfig() {
        disabledWorlds = new HashSet<>(getConfig().getStringList("disabled-worlds"));

        regionWhitelist = new HashMap<>();
        if (getConfig().isConfigurationSection("region-whitelist")) {
            for (String worldName : getConfig().getConfigurationSection("region-whitelist").getKeys(false)) {
                Map<String, RegionConfig> regions = new HashMap<>();
                for (String region : getConfig().getConfigurationSection("region-whitelist." + worldName).getKeys(false)) {
                    int max = getConfig().getInt("region-whitelist." + worldName + "." + region + ".max", 10);
                    List<String> mobNames = getConfig().getStringList("region-whitelist." + worldName + "." + region + ".mobs");
                    List<EntityType> entityTypes = new ArrayList<>();
                    for (String mob : mobNames) {
                        try {
                            entityTypes.add(EntityType.valueOf(mob.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            getLogger().warning("Entidad inválida en la configuración: " + mob);
                        }
                    }
                    regions.put(region, new RegionConfig(max, entityTypes));
                }
                regionWhitelist.put(worldName, regions);
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        String worldName = entity.getWorld().getName();

        if (!disabledWorlds.contains(worldName)) {
            return;
        }

        String region = getRegionAt(entity.getLocation());

        if (region == null) {
            event.setCancelled(true);
            return;
        }

        Map<String, RegionConfig> worldRegions = regionWhitelist.get(worldName);
        if (worldRegions != null) {
            RegionConfig config = worldRegions.get(region);
            if (config != null && config.entityTypes.contains(entity.getType())) {
                entity.setAI(false);
                return; // Permitido con IA desactivada
            }
        }

        event.setCancelled(true);
    }

    private void spawnMobsInRegions() {
        for (String worldName : regionWhitelist.keySet()) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) continue;

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

            if (regionManager == null) continue;

            Map<String, RegionConfig> regions = regionWhitelist.get(worldName);
            for (Map.Entry<String, RegionConfig> entry : regions.entrySet()) {
                String regionId = entry.getKey();
                RegionConfig config = entry.getValue();

                ProtectedRegion region = regionManager.getRegion(regionId);
                if (region == null) continue;

                int currentCount = 0;
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof LivingEntity) {
                        if (region.contains(entity.getLocation().getBlockX(), entity.getLocation().getBlockY(), entity.getLocation().getBlockZ())) {
                            if (config.entityTypes.contains(entity.getType())) {
                                currentCount++;
                            }
                        }
                    }
                }

                int toSpawn = Math.min(5, config.max - currentCount);
                for (int i = 0; i < toSpawn; i++) {
                    EntityType type = config.getRandomEntityType();
                    Location spawnLocation = getSafeLocation(world, region);
                    if (spawnLocation != null) {
                        LivingEntity spawned = (LivingEntity) world.spawnEntity(spawnLocation, type);
                        spawned.setAI(false);
                    }
                }
            }
        }
    }

    private Location getSafeLocation(World world, ProtectedRegion region) {
        Location min = BukkitAdapter.adapt(world, region.getMinimumPoint());
        Location max = BukkitAdapter.adapt(world, region.getMaximumPoint());

        for (int attempt = 0; attempt < 10; attempt++) {
            double x = min.getX() + Math.random() * (max.getX() - min.getX());
            double y = min.getY() + Math.random() * (max.getY() - min.getY());
            double z = min.getZ() + Math.random() * (max.getZ() - min.getZ());

            Location loc = new Location(world, x, y, z);
            Location ground = findGround(loc);
            if (ground != null) return ground;
        }

        return null;
    }

    private Location findGround(Location loc) {
        int y = loc.getBlockY();
        while (y > 0) {
            Block block = loc.getWorld().getBlockAt(loc.getBlockX(), y, loc.getBlockZ());
            if (block.getType().isSolid()) {
                return new Location(loc.getWorld(), loc.getX(), y + 1, loc.getZ());
            }
            y--;
        }
        return null;
    }

    private String getRegionAt(Location loc) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));

        if (regions == null) return null;

        ApplicableRegionSet regionSet = regions.getApplicableRegions(BukkitAdapter.asBlockVector(loc));

        for (ProtectedRegion region : regionSet) {
            return region.getId();
        }

        return null;
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
        int max;
        List<EntityType> entityTypes;

        public RegionConfig(int max, List<EntityType> entityTypes) {
            this.max = max;
            this.entityTypes = entityTypes;
        }

        public EntityType getRandomEntityType() {
            if (entityTypes.isEmpty()) return EntityType.PIG;
            return entityTypes.get(new Random().nextInt(entityTypes.size()));
        }
    }
}
