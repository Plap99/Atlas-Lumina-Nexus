package com.radig.atlasluminanexus.client;

import net.minecraft.resources.Identifier;

/** Graficos compartidos por HUD y lista. Cada icono tiene seis tintes PNG. */
public final class AtlasWaypointIconTextures {
    private static final String[] SHAPES = {"pin", "home", "mine", "forest", "gem", "flag"};
    private static final int[] COLORS = {
            0xFFFFD46A, 0xFF73D9FF, 0xFFAF88FF,
            0xFF70E4A4, 0xFFFF7777, 0xFFFFFFFF
    };
    private static final Identifier[][] ICONS = new Identifier[SHAPES.length][COLORS.length];
    static {
        for (int shape = 0; shape < SHAPES.length; shape++) {
            for (int color = 0; color < COLORS.length; color++) {
                ICONS[shape][color] = Identifier.fromNamespaceAndPath("atlasluminanexus",
                        "textures/gui/waypoint_colored/" + SHAPES[shape] + "_" + color + ".png");
            }
        }
    }
    private AtlasWaypointIconTextures() { }

    public static Identifier of(int shape, int argb) {
        shape = Math.max(0, Math.min(SHAPES.length - 1, shape));
        int color = 0;
        long min = Long.MAX_VALUE;
        for (int i = 0; i < COLORS.length; i++) {
            long dr = ((argb >> 16) & 255) - ((COLORS[i] >> 16) & 255);
            long dg = ((argb >> 8) & 255) - ((COLORS[i] >> 8) & 255);
            long db = (argb & 255) - (COLORS[i] & 255);
            long diff = dr * dr + dg * dg + db * db;
            if (diff < min) { min = diff; color = i; }
        }
        return ICONS[shape][color];
    }
}
