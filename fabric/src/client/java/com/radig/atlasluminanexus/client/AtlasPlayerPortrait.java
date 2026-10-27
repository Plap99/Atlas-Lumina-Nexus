package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/** Retratos pequeños sin marcos ni puntos decorativos. El cuerpo animado llegará en 6J. */
public final class AtlasPlayerPortrait {
    private AtlasPlayerPortrait() {}

    public static void draw(GuiGraphicsExtractor g, Minecraft mc, int x, int y,
                            int size, double yaw, boolean northUp) {
        if(mc.player!=null)drawPlayer(g,mc.player,x,y,size);
    }

    /** Devuelve false cuando todavía no hay una skin de jugador cliente disponible. */
    public static boolean drawPlayer(GuiGraphicsExtractor g, Player player, int x, int y, int size) {
        if(!(player instanceof AbstractClientPlayer clientPlayer))return false;
        Identifier texture=clientPlayer.getSkin().body().texturePath();
        int s=Math.max(4,Math.min(28,size));
        int left=x-s/2,top=y-s/2;
        // Rostro vanilla: UV 8,8 de la textura 64x64, más segunda capa UV 40,8.
        g.blit(texture,left,top,left+s,top+s,8f/64f,16f/64f,8f/64f,16f/64f);
        g.blit(texture,left,top,left+s,top+s,40f/64f,48f/64f,8f/64f,16f/64f);
        return true;
    }
}
