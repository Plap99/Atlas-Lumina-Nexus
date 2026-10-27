package com.radig.atlasluminanexus.client;
import com.radig.atlasluminanexus.core.*;
import net.minecraft.client.Minecraft;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Shared behavior, compiled separately under Fabric and Forge client source trees. */
public final class AtlasWaypointClient {
    private static final WaypointRegistry REGISTRY = new WaypointRegistry();
    private static String currentWorld = "";
    private static Path currentFile;
    private static UUID pendingPreview;
    private static int previewDelay;
    public static Path storageFile(Minecraft mc) { sync(mc); return currentFile; }
    public static void queuePreview(UUID id) { pendingPreview=id; previewDelay=3; }
    public static void tickPreview(Minecraft mc) { if (pendingPreview==null || mc.gui.screen()!=null || mc.level==null) return; if (--previewDelay>0) return; UUID id=pendingPreview;pendingPreview=null; AtlasPreviewClient.capture(mc,id); }
    private AtlasWaypointClient() {}
    private static String worldIdentity(Minecraft mc) {
        if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip;
        if (mc.getSingleplayerServer() != null)
            return "singleplayer:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        return "";
    }
    private static String safeHash(String value) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(h).substring(0, 20);
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    public static void sync(Minecraft mc) {
        String identity = worldIdentity(mc);
        if (identity.equals(currentWorld)) return;
        currentWorld = identity;
        REGISTRY.clear();
        currentFile = identity.isEmpty() ? null : Path.of("config", "atlasluminanexus", "waypoints", safeHash(identity) + ".json");
        if (currentFile == null) return;
        try { for (Waypoint wp : WaypointJsonStore.load(currentFile)) REGISTRY.put(wp); }
        catch (Exception ex) { System.err.println("[ALN] Cannot load waypoints: " + ex); }
    }
    public static List<Waypoint> all(Minecraft mc) { sync(mc); return REGISTRY.all(); }
    public static void upsert(Minecraft mc, Waypoint wp) throws Exception {
        sync(mc);
        if (currentFile == null) throw new IllegalStateException("No hay mundo abierto");
        List<Waypoint> old = REGISTRY.all();
        REGISTRY.put(wp);
        try { WaypointJsonStore.save(currentFile, REGISTRY.all()); }
        catch (Exception ex) { REGISTRY.clear(); for (Waypoint item : old) REGISTRY.put(item); throw ex; }
    }
    public static void delete(Minecraft mc, UUID id) throws Exception {
        sync(mc);
        if (currentFile == null) throw new IllegalStateException("No hay mundo abierto");
        List<Waypoint> old = REGISTRY.all();
        REGISTRY.remove(id);
        try { WaypointJsonStore.save(currentFile, REGISTRY.all()); AtlasPreviewClient.delete(mc,id); }
        catch (Exception ex) { REGISTRY.clear(); for (Waypoint item : old) REGISTRY.put(item); throw ex; }
    }
    public static String quickCreate(Minecraft mc) {
        sync(mc);
        if (currentFile == null || mc.player == null || mc.level == null) return "No world open";
        int next = REGISTRY.all().size() + 1;
        boolean home = REGISTRY.all().isEmpty();
        Waypoint wp = new Waypoint(UUID.randomUUID(), "Punto " + next,
            mc.level.dimension().toString(), mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            0xFFFFD46A, "General", true, home);
        try { upsert(mc, wp); queuePreview(wp.id()); return "Waypoint saved: " + wp.name(); }
        catch (Exception ex) { return "Error saving waypoint: " + ex.getMessage(); }
    }
    public static void setHome(Minecraft mc, UUID id) throws Exception {
        sync(mc);
        List<Waypoint> old = REGISTRY.all();
        if (old.stream().noneMatch(w -> w.id().equals(id))) return;
        REGISTRY.clear();
        for (Waypoint w : old)
            REGISTRY.put(new Waypoint(w.id(), w.name(), w.dimension(), w.x(), w.y(), w.z(),
                w.argb(), w.category(), w.visible(), w.id().equals(id)));
        try { WaypointJsonStore.save(currentFile, REGISTRY.all()); }
        catch (Exception ex) { REGISTRY.clear(); for (Waypoint w : old) REGISTRY.put(w); throw ex; }
    }
}
