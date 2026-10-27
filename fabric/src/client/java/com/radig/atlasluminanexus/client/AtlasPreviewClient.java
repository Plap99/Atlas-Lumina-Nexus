package com.radig.atlasluminanexus.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import java.nio.file.*;
import java.util.UUID;

/** Stored preview snapshots, isolated by the same hashed world as JSON. */
public final class AtlasPreviewClient {
    private AtlasPreviewClient() {}
    private static UUID loadedId;
    private static DynamicTexture loaded;
    private static Identifier loadedTextureId;

    public static Path previewFile(Minecraft mc, UUID id) {
        Path waypointFile = AtlasWaypointClient.storageFile(mc);
        if (waypointFile == null) return null;
        return waypointFile.getParent().resolve("previews")
            .resolve(waypointFile.getFileName().toString().replace(".json", ""))
            .resolve(id.toString() + ".png");
    }
    public static void capture(Minecraft mc, UUID id) {
        Path target = previewFile(mc, id);
        if (target == null || mc.level == null) return;
        // The callback supplies an owned NativeImage. Resize before writing to keep files tiny.
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(), source -> {
            try (source) {
                Files.createDirectories(target.getParent());
                int sw = source.getWidth(), sh = source.getHeight();
                int w = 192, h = 108;
                int cropW = Math.min(sw, Math.round(sh * (w / (float) h)));
                int cropH = Math.min(sh, Math.round(sw * (h / (float) w)));
                int ox = (sw - cropW) / 2, oy = (sh - cropH) / 2;
                try (NativeImage small = new NativeImage(w, h, false)) {
                    source.resizeSubRectTo(ox, oy, cropW, cropH, small);
                    Path tmp = target.resolveSibling(id + ".tmp.png");
                    small.writeToFile(tmp);
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                clear(id);
            } catch (Exception ex) {
                System.err.println("[ALN] Preview capture failed: " + ex);
            }
        });
    }
    public static void delete(Minecraft mc, UUID id) {
        Path p = previewFile(mc, id);
        if (p != null) try { Files.deleteIfExists(p); } catch (Exception ex) {
            System.err.println("[ALN] Could not delete old preview: " + ex);
        }
        clear(id);
    }
    public static DynamicTexture texture(Minecraft mc, UUID id) {
        if (id.equals(loadedId)) return loaded;
        clear(null);
        Path p = previewFile(mc, id);
        if (p == null || !Files.isRegularFile(p)) return null;
        try {
            try (var stream = Files.newInputStream(p)) {
                loaded = new DynamicTexture(() -> "aln_waypoint_preview", NativeImage.read(stream));
            }
            loadedId = id;
            loadedTextureId = Identifier.fromNamespaceAndPath("atlasluminanexus", "preview/" + id.toString().replace("-", ""));
            mc.getTextureManager().register(loadedTextureId, loaded);
            return loaded;
        } catch (Exception ex) {
            System.err.println("[ALN] Could not read preview: " + ex);
            return null;
        }
    }
    public static Identifier textureId(Minecraft mc, UUID id) {
        return texture(mc, id) == null ? null : loadedTextureId;
    }
    public static void clear(UUID id) {
        if (id != null && !id.equals(loadedId)) return;
        if (loadedTextureId != null) {
            Minecraft.getInstance().getTextureManager().release(loadedTextureId);
            loadedTextureId = null;
            loaded = null;
        } else if (loaded != null) { loaded.close(); loaded = null; }
        loadedId = null;
    }
}
