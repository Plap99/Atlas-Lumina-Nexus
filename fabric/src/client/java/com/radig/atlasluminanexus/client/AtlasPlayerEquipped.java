package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Miniatura del jugador con su equipo 3D real (armadura y accesorios que renderice Minecraft). */
public final class AtlasPlayerEquipped {
    private AtlasPlayerEquipped() {}

    /**
     * Cuerpo completo: una caja de extracción con margen suficiente para la cabeza
     * y accesorios (sin recortar el casco ni futuros audífonos de RecordCraft).
     * El tamaño configurado representa aproximadamente la ALTURA total del avatar,
     * no la escala bruta que InventoryScreen aplica al modelo.
     */
    public static void draw(GuiGraphicsExtractor g, Minecraft mc, int x, int y,
                            int configuredSize, double yaw, boolean mapRotates) {
        if (mc.player == null) return;
        int size = Math.max(10, Math.min(28, configuredSize));
        int scale = Math.max(5, Math.round(size * .54f));
        int halfW = Math.max(7, (int)Math.ceil(size * .57) + 3);
        int halfH = Math.max(9, (int)Math.ceil(size * .75) + 4);
        float lookX = mapRotates ? (float)x : (float)(x - Math.sin(yaw) * 4.0);
        float lookY = (float)y;
        InventoryScreen.extractEntityInInventoryFollowsMouse(
                g, x - halfW, y - halfH, x + halfW, y + halfH,
                scale, 0.0f, lookX, lookY, mc.player);
    }

    /**
     * Rostro equipado: recorta el MODELO REAL del jugador, con el casco puesto.
     * No superpone el icono plano del ítem: armaduras, sombreros y otros modelos
     * colocados en la cabeza conservan su tamaño y geometría reales.
     */
    public static void drawHead(GuiGraphicsExtractor g, Minecraft mc, int x, int y,
                                int configuredSize) {
        if (mc.player == null) return;
        int s = Math.max(8, Math.min(28, configuredSize));
        int modelScale = Math.max(16, Math.round(s * 2.05f));
        int wide = Math.max(30, Math.round(s * 3.1f));
        // La cabeza está situada sobre el centro vertical de la entidad.
        int modelCenterY = y + Math.round(modelScale * .65f);
        int left = x - s/2;
        int top = y - s/2;
        int right = left + s;
        int bottom = top + s;
        // Scissor anidado: solo aparece la cabeza en el minimapa, sin el torso.
        g.enableScissor(left, top, right, bottom);
        try {
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                    g, x - wide, modelCenterY - wide,
                    x + wide, modelCenterY + wide,
                    modelScale, 0.0f, (float)x, (float)modelCenterY, mc.player);
        } finally {
            g.disableScissor();
        }
    }
}
