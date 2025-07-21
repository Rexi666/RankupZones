package org.rexi.rankupZones.model;

import org.bukkit.entity.EntityType;

import java.util.*;

public class ZoneRegion {

    private final String name;
    private final String permission;
    private final Set<EntityType> allowedMobs;
    private final Map<EntityType, List<CustomDrop>> drops;

    public ZoneRegion(String name, String permission) {
        this.name = name;
        this.permission = permission;
        this.allowedMobs = new HashSet<>();
        this.drops = new HashMap<>();
    }

    public void addMob(EntityType type) {
        allowedMobs.add(type);
    }

    public void addDrop(EntityType type, CustomDrop drop) {
        drops.computeIfAbsent(type, k -> new ArrayList<>()).add(drop);
    }

    public boolean isMobAllowed(EntityType type) {
        return allowedMobs.contains(type);
    }

    public List<CustomDrop> getDrops(EntityType type) {
        return drops.getOrDefault(type, Collections.emptyList());
    }

    public String getPermission() {
        return permission;
    }

    public String getName() {
        return name;
    }
}
