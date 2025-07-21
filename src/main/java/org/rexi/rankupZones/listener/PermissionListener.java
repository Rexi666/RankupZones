package org.rexi.rankupZones.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.rexi.rankupZones.model.ZoneRegion;
import org.rexi.rankupZones.registry.RegionRegistry;

import java.util.Optional;

public class PermissionListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        Location location = event.getEntity().getLocation();
        String regionId = getRegionAt(location);
        if (regionId == null) return;

        ZoneRegion region = RegionRegistry.getRegion(regionId);
        if (region == null) return;

        String requiredPermission = region.getPermission();
        String requiredPermission2 = region.getPermission2();
        if (requiredPermission != null && player.hasPermission(requiredPermission)) {
            if (requiredPermission2 != null && player.hasPermission(requiredPermission2)) {
                return;
            } else {
                player.sendMessage(ChatColor.RED + "No tienes permiso para dañar entidades en esta zona.");
                event.setCancelled(true);
            }
        } else {
            player.sendMessage(ChatColor.RED + "No tienes permiso para dañar entidades en esta zona.");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        String fromRegion = getRegionAt(event.getFrom());
        String toRegion = getRegionAt(event.getTo());

        // Si cambió de región
        if (toRegion != null && !toRegion.equals(fromRegion)) {
            ZoneRegion region = RegionRegistry.getRegion(toRegion);
            if (region == null) return;

            String requiredPermission = region.getPermission();
            String requiredPermission2 = region.getPermission2();
            if (requiredPermission != null && event.getPlayer().hasPermission(requiredPermission)) {
                if (requiredPermission2 != null && event.getPlayer().hasPermission(requiredPermission2)) {
                    return;
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "No tienes permiso para entrar en esta zona.");
                    event.setCancelled(true);
                }
            } else {
                event.getPlayer().sendMessage(ChatColor.RED + "No tienes permiso para entrar en esta zona.");
                event.setCancelled(true);
            }
        }
    }

    private String getRegionAt(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) return null;

        ApplicableRegionSet regions = manager.getApplicableRegions(BukkitAdapter.asBlockVector(location));
        Optional<String> first = regions.getRegions().stream()
                .map(r -> r.getId().toLowerCase())
                .filter(RegionRegistry::hasRegion) // solo considera regiones registradas
                .findFirst();

        return first.orElse(null);
    }
}