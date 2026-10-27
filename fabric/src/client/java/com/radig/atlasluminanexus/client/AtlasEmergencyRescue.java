package com.radig.atlasluminanexus.client;

import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Rescate deliberado Ctrl+R. Sin invulnerabilidad: requiere permisos vanilla del servidor. */
public final class AtlasEmergencyRescue {
    private AtlasEmergencyRescue() { }

    public static void rescue(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.player.connection == null) return;
        Waypoint home = AtlasWaypointClient.all(mc).stream()
                .filter(Waypoint::emergencyDestination).findFirst().orElse(null);
        if (home == null) {
            mc.player.sendSystemMessage(Component.literal("[ALN] Primero elige un destino seguro en el Atlas (U)."));
            return;
        }
        // Ningún salto sin los permisos que el servidor ya concede.
        LuminaGateClient.startEmergency(home);
    }
}
