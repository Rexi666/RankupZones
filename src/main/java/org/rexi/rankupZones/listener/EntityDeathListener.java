package org.rexi.rankupZones.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.rexi.rankupZones.RankupZones;
import org.rexi.rankupZones.model.CustomDrop;
import org.rexi.rankupZones.model.ZoneRegion;
import org.rexi.rankupZones.registry.RegionRegistry;

public class EntityDeathListener implements Listener {

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();

        String regionName = getRegionName(entity);
        if (regionName == null) return;

        ZoneRegion region = RegionRegistry.getRegion(regionName);
        if (region == null || !region.isMobAllowed(entity.getType())) return;

        event.getDrops().clear();

        for (CustomDrop drop : region.getDrops(entity.getType())) {
            event.getDrops().add(drop.toItemStack());
        }

        // Restar del contador
        RankupZones plugin = RankupZones.getInstance();
        EntityType type = entity.getType();

        switch (type) {
            case COW -> plugin.countVacas--;
            case PIG -> plugin.countCerdos--;
            case SHEEP -> plugin.countOvejas--;
            case RABBIT -> plugin.countConejos--;
            case CHICKEN -> plugin.countPollos--;
            case BEE -> plugin.countAbejas--;
            case ZOMBIE -> plugin.countZombies--;
            case SKELETON -> plugin.countEsqueletos--;
            case SPIDER -> plugin.countAranas--;
            case BAT -> plugin.countMurcielagos--;
            case CREEPER -> plugin.countCreepers--;
            case BLAZE -> plugin.countBlazes--;
        }
    }

    private String getRegionName(Entity entity) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(entity.getWorld()));
        if (manager == null) return null;

        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(entity.getLocation()));
        return set.getRegions().stream().findFirst().map(r -> r.getId().toLowerCase()).orElse(null);
    }

    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)) return;

        String regionName = getRegionName(creeper);
        if (regionName == null) return;

        ZoneRegion region = RegionRegistry.getRegion(regionName);
        if (region == null || !region.isMobAllowed(creeper.getType())) return;

        RankupZones.getInstance().countCreepers--;
    }
}