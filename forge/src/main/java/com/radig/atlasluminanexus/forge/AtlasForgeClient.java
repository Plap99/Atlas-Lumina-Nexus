package com.radig.atlasluminanexus.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.radig.atlasluminanexus.client.AtlasScreen;
import com.radig.atlasluminanexus.client.AtlasWaypointClient;
import com.radig.atlasluminanexus.client.LuminaGateClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.SystemMessageReceivedEvent;
import com.radig.atlasluminanexus.client.AtlasWaypointHud;
import com.radig.atlasluminanexus.client.AtlasEmergencyRescue;
import net.minecraftforge.event.TickEvent;

public final class AtlasForgeClient {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("atlasluminanexus", "controls"));
    private static final KeyMapping OPEN = new KeyMapping("key.atlasluminanexus.waypoint_list",
            InputConstants.Type.KEYSYM, InputConstants.KEY_U, CATEGORY);
    private static final KeyMapping RESCUE = new KeyMapping("key.atlasluminanexus.emergency_rescue_ctrl_r",
            InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY);
    private static final KeyMapping SAVE = new KeyMapping("key.atlasluminanexus.save_new_waypoint",
            InputConstants.Type.KEYSYM, InputConstants.KEY_N, CATEGORY);
    private static final KeyMapping WORLD_MAP = new KeyMapping("key.atlasluminanexus.world_map",
            InputConstants.Type.KEYSYM, InputConstants.KEY_M, CATEGORY);
    private AtlasForgeClient() {}
    public static void registerHud(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(
                Identifier.fromNamespaceAndPath("atlasluminanexus", "waypoint_hud"),
                (graphics, delta) -> AtlasWaypointHud.draw(graphics)
        );
    }
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
        event.register(WORLD_MAP);
        event.register(RESCUE);
        event.register(SAVE);
        // Bus de mensajes de sistema (Forge 65.1.5, EventBus 7).
        SystemMessageReceivedEvent.BUS.addListener(AtlasForgeClient::filterSystemMessage);
    }
    public static boolean filterSystemMessage(SystemMessageReceivedEvent event) {
        return LuminaGateClient.shouldHideVanillaTeleportMessage(event.getMessage());
    }
    public static void afterTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        AtlasWaypointClient.sync(mc);
        com.radig.atlasluminanexus.client.AtlasMinimapStore.tick(mc);
        AtlasWaypointClient.tickPreview(mc);
        LuminaGateClient.tick(mc);
        while (WORLD_MAP.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null)
                mc.gui.setScreen(new com.radig.atlasluminanexus.client.AtlasWorldMapScreen());
        }
        while (OPEN.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) mc.gui.setScreen(new AtlasScreen());
        }
        while (SAVE.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null)
                mc.player.sendSystemMessage(Component.literal("[ALN] " + AtlasWaypointClient.quickCreate(mc)));
        }
        while (RESCUE.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null && mc.hasControlDown())
                AtlasEmergencyRescue.rescue(mc);
        }
    }
}
