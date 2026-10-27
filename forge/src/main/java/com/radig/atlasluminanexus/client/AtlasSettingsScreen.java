package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Entrada histórica: todas las preferencias ahora están en pestañas integradas. */
public final class AtlasSettingsScreen extends Screen {
    public AtlasSettingsScreen(){super(Component.literal("Atlas - Configuración"));}
    @Override protected void init(){Minecraft.getInstance().gui.setScreen(new AtlasMinimapSettingsScreen(0));}
}
