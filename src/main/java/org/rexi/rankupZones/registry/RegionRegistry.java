package org.rexi.rankupZones.registry;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.rexi.rankupZones.RankupZones;
import org.rexi.rankupZones.model.CustomDrop;
import org.rexi.rankupZones.model.ZoneRegion;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;

public class RegionRegistry {

    private static final Map<String, ZoneRegion> regions = new HashMap<>();
    private static final Set<String> disabledWorlds = Set.of("mobs");

    public static void loadRegions() {
        // Región ejemplo: rankup_vacas
        addRegion("rankup_vacas", "group.default", EntityType.COW, "Esencia de Vaca");
        addRegion("rankup_cerdos", "nivel.1", EntityType.PIG, "Esencia de Cerdo");
        addRegion("rankup_ovejas", "nivel.2", EntityType.SHEEP, "Esencia de Oveja");
        addRegion("rankup_conejos", "nivel.3",  EntityType.RABBIT, "Esencia de Conejo");
        addRegion("rankup_pollos", "nivel.10",  EntityType.CHICKEN, "Esencia de Pollo");
        addRegion("rankup_abejas", "nivel.11",  EntityType.BEE, "Esencia de Abeja");
        addRegion("rankup_zombies", "nivel.5",  EntityType.ZOMBIE, "Esencia de Zombie");
        addRegion("rankup_esqueletos", "nivel.5",  EntityType.SKELETON, "Esencia de Esqueleto");
        addRegion("rankup_aranas", "nivel.6",  EntityType.SPIDER, "Esencia de Arañas");
        addRegion("rankup_muercielagos", "nivel.7",  EntityType.BAT, "Esencia de Murciélagos");
        addRegion("rankup_creepers", "nivel.8",  EntityType.CREEPER, "Esencia de Creepers");
    }

    private static void addRegion(String name, String permission, EntityType mob, String dropName) {
        ZoneRegion region = new ZoneRegion(name, permission);
        region.addMob(mob);
        region.addDrop(mob, new CustomDrop(
                Material.REDSTONE,
                1,
                "&6" + dropName,
                List.of("&r", "&7Tradeable en /warp rankup"),
                true
        ));
        regions.put(name, region);
    }

    public static Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public static ZoneRegion getRegion(String name) {
        return regions.get(name);
    }

    public static Collection<ZoneRegion> getAllRegions() {
        return regions.values();
    }

    public static void spawnMob(String regionId, EntityType type, int amount) {
        ZoneRegion zone = getRegion(regionId);
        if (zone == null) return;

        World world = Bukkit.getWorld("mobs"); // Ajusta si tu mundo tiene otro nombre
        if (world == null) return;

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return;

        ProtectedRegion region = manager.getRegion(regionId);
        if (region == null) return;

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        for (int i = 0; i < amount; i++) {
            Location loc = null;

            for (int attempt = 0; attempt < 10; attempt++) {
                double x = randomBetween(min.getBlockX(), max.getBlockX()) + 0.5;
                double y = randomBetween(min.getBlockY(), max.getBlockY());
                double z = randomBetween(min.getBlockZ(), max.getBlockZ()) + 0.5;

                Location candidate = new Location(world, x, y, z);

                if (candidate.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                    loc = candidate;
                    break;
                }
            }

            if (loc == null) continue;

            Entity rawEntity = world.spawnEntity(loc, type, true); // Spawnea el mob
        }
    }

    private static int randomBetween(int min, int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }

    public static boolean hasRegion(String regionId) {
        return regions.containsKey(regionId.toLowerCase());
    }
}

