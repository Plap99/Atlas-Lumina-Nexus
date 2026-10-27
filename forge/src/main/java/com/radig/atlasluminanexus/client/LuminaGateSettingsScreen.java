package com.radig.atlasluminanexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Ajustes de Lumina Gate: sin dependencias específicas de Fabric o Forge. */
public final class LuminaGateSettingsScreen extends Screen {
    private Button mode, particles, sound, messages, vanillaFeedback;
    private int left, top, wide, high;

    public LuminaGateSettingsScreen() {
        super(Component.literal("Lumina Gate - Efectos"));
    }

    @Override protected void init() {
        wide = Math.min(315, width - 30);
        high = Math.min(250, height - 24);
        left = (width - wide) / 2;
        top = (height - high) / 2;
        int x = left + 16, w = wide - 32;
        mode = addRenderableWidget(Button.builder(Component.empty(), b -> {
            LuminaGateConfig.nextMode(); updateLabels();
        }).bounds(x, top + 42, w, 20).build());
        particles = addRenderableWidget(Button.builder(Component.empty(), b -> {
            LuminaGateConfig.nextDensity(); updateLabels();
        }).bounds(x, top + 68, w, 20).build());
        sound = addRenderableWidget(Button.builder(Component.empty(), b -> {
            LuminaGateConfig.toggleSound(); updateLabels();
        }).bounds(x, top + 94, w, 20).build());
        messages = addRenderableWidget(Button.builder(Component.empty(), b -> {
            LuminaGateConfig.toggleMessages(); updateLabels();
        }).bounds(x, top + 120, w, 20).build());
        vanillaFeedback = addRenderableWidget(Button.builder(Component.empty(), b -> {
            LuminaGateConfig.toggleVanillaFeedback(); updateLabels();
        }).bounds(x, top + 146, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Probar sonido mágico"),
                b -> LuminaGateClient.playMagicSound(false))
                .bounds(x, top + 175, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Volver al Atlas"),
                b -> Minecraft.getInstance().gui.setScreen(new AtlasScreen()))
                .bounds(x, top + 202, w, 20).build());
        updateLabels();
    }

    private void updateLabels() {
        String selectedMode = switch (LuminaGateConfig.mode()) {
            case FULL -> "Completo (~3 s)";
            case FAST -> "Rapido (~0.5 s)";
            case INSTANT -> "Instantaneo";
        };
        String selectedParticles = switch (LuminaGateConfig.density()) {
            case LOW -> "Bajas";
            case NORMAL -> "Normales";
            case HIGH -> "Altas";
        };
        mode.setMessage(Component.literal("Teletransporte: " + selectedMode));
        particles.setMessage(Component.literal("Particulas: " + selectedParticles));
        sound.setMessage(Component.literal("Sonido: " + (LuminaGateConfig.sound() ? "Si" : "No")));
        messages.setMessage(Component.literal("Mensajes Lumina: " + (LuminaGateConfig.messages() ? "Si" : "No")));
        vanillaFeedback.setMessage(Component.literal("Avisos vanilla del viaje: " +
                (LuminaGateConfig.showVanillaFeedback() ? "Mostrar" : "Ocultar")));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float tick) {
        g.fill(left, top, left + wide, top + high, 0xF60D1422);
        g.outline(left, top, wide, high, 0xFFE3BF75);
        g.text(font, "LUMINA GATE  |  EFECTOS", left + 16, top + 12, 0xFFE3BF75, false);
        g.text(font, "La mejor magia es sencilla.", left + 16, top + 26, 0xFFB4A993, false);
        g.text(font, "Los permisos siguen siendo del servidor.",
                left + 16, top + 230, 0xFFB4A993, false);
        super.extractRenderState(g, mouseX, mouseY, tick);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_M) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean isPauseScreen() { return false; }
}
