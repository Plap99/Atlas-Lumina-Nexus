package com.radig.atlasluminanexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class AtlasLuminaNexusClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
                !LuminaGateClient.shouldHideVanillaTeleportMessage(message));
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("atlasluminanexus", "controls"));
        KeyMapping openAtlas = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.atlasluminanexus.waypoint_list", InputConstants.Type.KEYSYM,
                InputConstants.KEY_U, category));
        // Ctrl + R: rescate deliberado; respeta los permisos vanilla.
        KeyMapping emergency = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.atlasluminanexus.emergency_rescue_ctrl_r", InputConstants.Type.KEYSYM,
                InputConstants.KEY_R, category));
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("atlasluminanexus", "waypoint_markers"),
                (graphics, deltaTracker) -> AtlasWaypointHud.draw(graphics));
        KeyMapping quickWaypoint = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.atlasluminanexus.save_new_waypoint", InputConstants.Type.KEYSYM,
                InputConstants.KEY_N, category));
        KeyMapping openMap = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.atlasluminanexus.world_map", InputConstants.Type.KEYSYM,
                InputConstants.KEY_M, category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AtlasWaypointClient.sync(client);
            AtlasMinimapStore.tick(client);
            AtlasWaypointClient.tickPreview(client);
            LuminaGateClient.tick(client);
            while (openMap.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) client.gui.setScreen(new AtlasWorldMapScreen());
            }
            while (openAtlas.consumeClick()) {
                if (client.player != null && client.gui.screen() == null) client.gui.setScreen(new AtlasScreen());
            }
            while (quickWaypoint.consumeClick()) {
                if (client.player != null && client.gui.screen() == null)
                    client.player.sendSystemMessage(Component.literal("[ALN] " + AtlasWaypointClient.quickCreate(client)));
            }
            while (emergency.consumeClick()) {
                if (client.player != null && client.gui.screen() == null && client.hasControlDown())
                    AtlasEmergencyRescue.rescue(client);
            }
        });
    }
}
