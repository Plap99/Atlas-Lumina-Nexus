package com.radig.atlasluminanexus.forge;

import com.radig.atlasluminanexus.core.AtlasConstants;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(AtlasConstants.MOD_ID)
public final class AtlasForge {
    public AtlasForge() {
        RegisterKeyMappingsEvent.BUS.addListener(AtlasForgeClient::registerKeys);
        TickEvent.ClientTickEvent.Post.BUS.addListener(AtlasForgeClient::afterTick);
        AddGuiOverlayLayersEvent.BUS.addListener(AtlasForgeClient::registerHud);
        System.out.println("[ALN] Atlas Lumina Nexus | Forge 26.2 initialized");
    }
}
