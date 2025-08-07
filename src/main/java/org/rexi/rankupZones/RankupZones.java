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
import org.rexi.rankupZones.listener.*;
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
    public boolean mobsCleared = false;

    public static RankupZones getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
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

        getServer().getPluginManager().registerEvents(new EntitySpawnListener(), this);
        getServer().getPluginManager().registerEvents(new EntityDeathListener(), this);
        getServer().getPluginManager().registerEvents(new PermissionListener(), this);
        getServer().getPluginManager().registerEvents(new EntityMoveListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

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

}
