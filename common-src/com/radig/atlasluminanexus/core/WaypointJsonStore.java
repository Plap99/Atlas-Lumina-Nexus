package com.radig.atlasluminanexus.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Client-side persistent JSON storage. Never mixes files from distinct world identities. */
public final class WaypointJsonStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private WaypointJsonStore() {}

    public static List<Waypoint> load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) return List.of();
        JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray items = root.getAsJsonArray("waypoints");
        if (items == null) return List.of();
        List<Waypoint> result = new ArrayList<>();
        for (JsonElement element : items) {
            try {
                JsonObject j = element.getAsJsonObject();
                result.add(new Waypoint(UUID.fromString(j.get("id").getAsString()),
                    j.get("name").getAsString(), j.get("dimension").getAsString(),
                    j.get("x").getAsDouble(), j.get("y").getAsDouble(), j.get("z").getAsDouble(),
                    j.get("argb").getAsInt(), j.get("category").getAsString(),
                    j.get("visible").getAsBoolean(), j.get("emergencyDestination").getAsBoolean()));
            } catch (RuntimeException ignored) { /* Skip invalid entry; preserve remaining points. */ }
        }
        return result;
    }

    public static void save(Path file, List<Waypoint> waypoints) throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        JsonArray entries = new JsonArray();
        for (Waypoint wp : waypoints) {
            JsonObject j = new JsonObject();
            j.addProperty("id", wp.id().toString());
            j.addProperty("name", wp.name());
            j.addProperty("dimension", wp.dimension());
            j.addProperty("x", wp.x()); j.addProperty("y", wp.y()); j.addProperty("z", wp.z());
            j.addProperty("argb", wp.argb()); j.addProperty("category", wp.category());
            j.addProperty("visible", wp.visible());
            j.addProperty("emergencyDestination", wp.emergencyDestination());
            entries.add(j);
        }
        root.add("waypoints", entries);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
        try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
    }
}
