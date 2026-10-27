package com.radig.atlasluminanexus.core;
import java.util.UUID;
/** Waypoint data that deliberately has no loader-specific dependencies. */
public record Waypoint(UUID id, String name, String dimension, double x, double y, double z,
                       int argb, String category, boolean visible, boolean emergencyDestination) {
    public Waypoint {
        if (id == null || name == null || name.isBlank() || dimension == null || dimension.isBlank())
            throw new IllegalArgumentException("Invalid waypoint identity/name/dimension");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Coordinates must be finite");
        category = category == null ? "General" : category;
    }
}
