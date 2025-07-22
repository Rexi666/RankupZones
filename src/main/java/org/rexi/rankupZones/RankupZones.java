package org.rexi.rankupZones;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.rexi.rankupZones.command.CountingCommand;
import org.rexi.rankupZones.listener.EntityMoveListener;
import org.rexi.rankupZones.listener.EntitySpawnListener;
import org.rexi.rankupZones.listener.EntityDeathListener;
import org.rexi.rankupZones.listener.PermissionListener;
import org.rexi.rankupZones.registry.RegionRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class RankupZones extends JavaPlugin {

    private static RankupZones instance;

    public int countVacas = 0;
    public int countCerdos = 0;
    public int countOvejas = 0;
    public int countConejos = 0;
    public int countPollos = 0;
    public int countAbejas = 0;
    public int countZombies = 0;
    public int countEsqueletos = 0;
    public int countAranas = 0;
    public int countMurcielagos = 0;
    public int countCreepers = 0;
    public int countBlazes = 0;

    public static RankupZones getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Carga la configuración hardcodeada
        RegionRegistry.loadRegions();

        new BukkitRunnable() {
            @Override
            public void run() {
                removeMobsInRegions();
            }
        }.runTaskLater(this, 100L);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnMobsInRegions();
            }
        }.runTaskTimer(this, 150L, 20L * 15);

        new BukkitRunnable() {
            @Override
            public void run() {
                recountEntitiesInRegions();
            }
        }.runTaskTimer(this, 150L, 20L * 120); // cada 2 minutos


        getServer().getPluginManager().registerEvents(new EntitySpawnListener(), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(), this);
        getServer().getPluginManager().registerEvents(new PermissionListener(), this);
        getServer().getPluginManager().registerEvents(new EntityMoveListener(), this);

        CountingCommand CountingCommand = new CountingCommand(this);
        getCommand("rz").setExecutor(CountingCommand);
        getLogger().info("RankupZones ha sido habilitado.");
    }

    @Override
    public void onDisable() {
        getLogger().info("RankupZones ha sido deshabilitado.");
    }

    private void spawnMobsInRegions() {
        if (countVacas < 15) {
            RegionRegistry.spawnMob("rankup_vacas", EntityType.COW, 3);
        }
        if (countCerdos < 15) {
            RegionRegistry.spawnMob("rankup_cerdos", EntityType.PIG, 3);
        }
        if (countOvejas < 15) {
            RegionRegistry.spawnMob("rankup_ovejas", EntityType.SHEEP, 3);
        }
        if (countConejos < 15) {
            RegionRegistry.spawnMob("rankup_conejos", EntityType.RABBIT, 3);
        }
        if (countPollos < 15) {
            RegionRegistry.spawnMob("rankup_pollos", EntityType.CHICKEN, 3);
        }
        if (countAbejas < 15) {
            RegionRegistry.spawnMob("rankup_abejas", EntityType.BEE, 3);
        }
        if (countZombies < 15) {
            RegionRegistry.spawnMob("rankup_zombies", EntityType.ZOMBIE, 3);
        }
        if (countEsqueletos < 15) {
            RegionRegistry.spawnMob("rankup_esqueletos", EntityType.SKELETON, 3);
        }
        if (countAranas < 15) {
            RegionRegistry.spawnMob("rankup_aranas", EntityType.SPIDER, 3);
        }
        if (countMurcielagos < 15) {
            RegionRegistry.spawnMob("rankup_muercielagos", EntityType.BAT, 3);
        }
        if (countCreepers < 15) {
            RegionRegistry.spawnMob("rankup_creepers", EntityType.CREEPER, 3);
        }
        if (countBlazes < 15) {
            RegionRegistry.spawnMob("rankup_blazes", EntityType.BLAZE, 3);
        }
    }

    private void removeMobsInRegions() {
        World world = getServer().getWorld("mobs");
        if (world == null) return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return;

        world.getEntities().stream()
                .filter(entity -> entity instanceof LivingEntity && !(entity instanceof Player))
                .forEach(entity -> {
                    Location loc = entity.getLocation();
                    ProtectedRegion region = manager.getApplicableRegions(BukkitAdapter.asBlockVector(loc))
                            .getRegions()
                            .stream()
                            .findFirst()
                            .orElse(null);

                    if (region != null && RegionRegistry.getRegion(region.getId()) != null) {
                        entity.remove();
                    }
                });
    }

    private void recountEntitiesInRegions() {
        World world = getServer().getWorld("mobs");
        if (world == null) return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return;

        RegionRegistry.getAllRegions().forEach(region -> {
            String regionId = region.getName();
            ProtectedRegion protectedRegion = manager.getRegion(regionId);
            if (protectedRegion == null) return;

            boolean hasEntity = world.getEntities().stream()
                    .filter(entity -> entity instanceof LivingEntity && !(entity instanceof Player))
                    .anyMatch(entity -> {
                        Location loc = entity.getLocation();
                        if (!protectedRegion.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                            return false;
                        }
                        return regionMatchesEntityType(region.getName(), entity.getType());
                    });

            if (!hasEntity) {
                resetCounterForRegion(regionId);
            }
        });
    }

    private boolean regionMatchesEntityType(String regionId, EntityType type) {
        return switch (regionId) {
            case "rankup_vacas" -> type == EntityType.COW;
            case "rankup_cerdos" -> type == EntityType.PIG;
            case "rankup_ovejas" -> type == EntityType.SHEEP;
            case "rankup_conejos" -> type == EntityType.RABBIT;
            case "rankup_pollos" -> type == EntityType.CHICKEN;
            case "rankup_abejas" -> type == EntityType.BEE;
            case "rankup_zombies" -> type == EntityType.ZOMBIE;
            case "rankup_esqueletos" -> type == EntityType.SKELETON;
            case "rankup_aranas" -> type == EntityType.SPIDER;
            case "rankup_muercielagos" -> type == EntityType.BAT;
            case "rankup_creepers" -> type == EntityType.CREEPER;
            case "rankup_blazes" -> type == EntityType.BLAZE;
            default -> false;
        };
    }

    private void resetCounterForRegion(String regionId) {
        switch (regionId) {
            case "rankup_vacas" -> countVacas = 0;
            case "rankup_cerdos" -> countCerdos = 0;
            case "rankup_ovejas" -> countOvejas = 0;
            case "rankup_conejos" -> countConejos = 0;
            case "rankup_pollos" -> countPollos = 0;
            case "rankup_abejas" -> countAbejas = 0;
            case "rankup_zombies" -> countZombies = 0;
            case "rankup_esqueletos" -> countEsqueletos = 0;
            case "rankup_aranas" -> countAranas = 0;
            case "rankup_muercielagos" -> countMurcielagos = 0;
            case "rankup_creepers" -> countCreepers = 0;
            case "rankup_blazes" -> countBlazes = 0;
        }
    }

}
