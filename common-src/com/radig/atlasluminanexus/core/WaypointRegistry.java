package com.radig.atlasluminanexus.core;
import java.util.*;
/** Temporary registry. Disk persistence and world/server profiles follow later. */
public final class WaypointRegistry {
    private final Map<UUID, Waypoint> waypoints = new LinkedHashMap<>();
    public synchronized void put(Waypoint waypoint) { waypoints.put(waypoint.id(), waypoint); }
    public synchronized boolean remove(UUID id) { return waypoints.remove(id) != null; }
    public synchronized List<Waypoint> all() { return List.copyOf(waypoints.values()); }
    public synchronized void clear() { waypoints.clear(); }
}
