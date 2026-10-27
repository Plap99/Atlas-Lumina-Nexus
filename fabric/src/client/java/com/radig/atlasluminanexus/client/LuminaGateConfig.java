package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Properties;

/** Preferencias exclusivamente del cliente; no modifica reglas del servidor. */
public final class LuminaGateConfig {
    public enum Mode { FULL, FAST, INSTANT }
    public enum Density { LOW, NORMAL, HIGH }

    private static boolean loaded;
    private static Mode mode = Mode.FULL;
    private static Density density = Density.NORMAL;
    private static boolean sound = true;
    private static boolean messages = true;
    // Solo ocultar la confirmacion vanilla producida por un comando de ALN.
    private static boolean showVanillaFeedback = false;

    private LuminaGateConfig() { }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("atlasluminanexus").resolve("lumina_gate.properties");
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        Path path = file();
        if (!Files.isRegularFile(path)) return;
        Properties p = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            p.load(input);
            try { mode = Mode.valueOf(p.getProperty("mode", "FULL").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { mode = Mode.FULL; }
            try { density = Density.valueOf(p.getProperty("particles", "NORMAL").toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException ignored) { density = Density.NORMAL; }
            sound = Boolean.parseBoolean(p.getProperty("sound", "true"));
            messages = Boolean.parseBoolean(p.getProperty("messages", "true"));
            showVanillaFeedback = Boolean.parseBoolean(p.getProperty("vanillaTeleportFeedback", "false"));
        } catch (IOException ex) {
            System.err.println("[ALN] No se pudieron leer los efectos de Lumina: " + ex.getMessage());
        }
    }

    public static synchronized void save() {
        load();
        Properties p = new Properties();
        p.setProperty("mode", mode.name().toLowerCase(Locale.ROOT));
        p.setProperty("particles", density.name().toLowerCase(Locale.ROOT));
        p.setProperty("sound", Boolean.toString(sound));
        p.setProperty("messages", Boolean.toString(messages));
        p.setProperty("vanillaTeleportFeedback", Boolean.toString(showVanillaFeedback));
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            Path temp = Files.createTempFile(path.getParent(), "lumina_", ".tmp");
            try {
                try (OutputStream output = Files.newOutputStream(temp)) {
                    p.store(output, "Atlas Lumina Nexus - Lumina Gate (solo cliente)");
                }
                try {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException ex) {
            System.err.println("[ALN] No se pudieron guardar los efectos de Lumina: " + ex.getMessage());
        }
    }

    public static Mode mode() { load(); return mode; }
    public static Density density() { load(); return density; }
    public static boolean sound() { load(); return sound; }
    public static boolean messages() { load(); return messages; }
    public static void nextMode() {
        load(); mode = Mode.values()[(mode.ordinal() + 1) % Mode.values().length]; save();
    }
    public static void nextDensity() {
        load(); density = Density.values()[(density.ordinal() + 1) % Density.values().length]; save();
    }
    public static void toggleSound() { load(); sound = !sound; save(); }
    public static void toggleMessages() { load(); messages = !messages; save(); }
    public static boolean showVanillaFeedback() { load(); return showVanillaFeedback; }
    public static void toggleVanillaFeedback() {
        load(); showVanillaFeedback = !showVanillaFeedback; save();
    }
}
