package org.rexi.rankupZones.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.entity.EntityMoveEvent;

public class EntityMoveListener implements Listener {

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        Entity entity = event.getEntity();
        World world = entity.getWorld();

        if (!world.getName().equalsIgnoreCase("mobs")) return;

        // Si no es un LivingEntity o no tiene AI, permitimos el movimiento
        if (!(entity instanceof LivingEntity living) || !living.hasAI()) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        // Si el bloque de destino es el mismo, ignoramos (para optimización)
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        String fromRegion = getRegionName(from);
        String toRegion = getRegionName(to);

        // Si la región cambia, cancelamos el movimiento
        if (fromRegion != null && (toRegion == null || !fromRegion.equalsIgnoreCase(toRegion))) {
            event.setCancelled(true);
        }
    }

    private String getRegionName(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return null;

        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        return set.getRegions().stream().findFirst().map(r -> r.getId().toLowerCase()).orElse(null);
    }
}
