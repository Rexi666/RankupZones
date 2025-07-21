package org.rexi.rankupZones.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.rexi.rankupZones.RankupZones;
import org.rexi.rankupZones.model.ZoneRegion;
import org.rexi.rankupZones.registry.RegionRegistry;

public class EntitySpawnListener implements Listener {

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        World world = entity.getWorld();

        if (!RegionRegistry.getDisabledWorlds().contains(world.getName())) return;

        String region = getRegionName(entity);
        ZoneRegion zr = RegionRegistry.getRegion(region);

        // Si no hay región o el mob no está permitido, cancelamos el spawn
        if (zr == null || !zr.isMobAllowed(entity.getType())) {
            event.setCancelled(true);
            return;
        }

        // Si está permitido, desactivamos AI y otras flags
        if (entity instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false); // No se elimina si el jugador se aleja
            living.setAI(false);                // Sin IA
            living.setPersistent(true);         // No despawnea naturalmente

            // Restar del contador
            RankupZones plugin = RankupZones.getInstance();
            EntityType type = entity.getType();

            switch (type) {
                case COW -> plugin.countVacas++;
                case PIG -> plugin.countCerdos++;
                case SHEEP -> plugin.countOvejas++;
                case RABBIT -> plugin.countConejos++;
                case CHICKEN -> plugin.countPollos++;
                case BEE -> plugin.countAbejas++;
                case ZOMBIE -> plugin.countZombies++;
                case SKELETON -> plugin.countEsqueletos++;
                case SPIDER -> plugin.countAranas++;
                case BAT -> plugin.countMurcielagos++;
                case CREEPER -> plugin.countCreepers++;
            }
        }
    }

    private String getRegionName(Entity entity) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(entity.getWorld()));
        if (manager == null) return null;

        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(entity.getLocation()));
        return set.getRegions().stream().findFirst().map(r -> r.getId().toLowerCase()).orElse(null);
    }
}
