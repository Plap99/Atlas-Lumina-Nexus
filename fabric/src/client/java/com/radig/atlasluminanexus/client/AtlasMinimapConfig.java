package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Preferencias propias del minimapa, globales del cliente y ampliables. */
public final class AtlasMinimapConfig {
    public static boolean enabled = true, round = true, rotate = false;
    public static boolean showNorth = true, showBrand = false;
    public static boolean showCoordinates = true, highDetail = true, smoothTerrain = true, luminaFrame = true;
    /** Separate visual quality presets: 0 Classic, 1 Detailed, 2 Lumina HD. */
    public static int terrainMinimap = 1, terrainWorldMap = 2;
    public static int size = 1; // 0..4; 1 = compacto (80 px de diametro en GUI)
    public static boolean showUnknownGrid = true, left = true;
    public static boolean players = true, friends = true, showFriendNames = true;
    public static boolean mobs = false, animals = false, villagers = true;
    public static boolean waypoints = true, waypointLabels = false;
    /** 0 = cuerpo completo, 1 = cabeza, 2 = flecha, 3 = punto. */
    public static int playerMarkerStyle = 0;
    /** Tamaño independiente de criaturas/otros jugadores. */
    public static int playerMarkerSize = 17; // 1..28 px, mínimo visual según estilo
    public static boolean playerShowEquipment = false;
    public static int playerMarkerColor = 1; // 0..5, cian predeterminado
    /** Preferencia antigua: mantener para migrar sin perder la selección del jugador. */
    public static boolean animatedAvatar = true;
    public static boolean worldMapEntities = true; // Casilla independiente para mapa grande
    public static int zoom = 2; // 0..4; mayor indice = acercamiento
    public static int entityIconSize = 7; // 3..10; escala tambien el indicador del jugador
    public static int opacity = 86; // porcentaje de fondo
    public static final Set<String> friendNames = new LinkedHashSet<>();
    private static boolean loaded;
    private AtlasMinimapConfig() {}
    private static Path file() { return Minecraft.getInstance().gameDirectory.toPath()
            .resolve("config/atlasluminanexus/minimap.properties"); }
    private static boolean flag(Properties p, String name, boolean original) {
        return Boolean.parseBoolean(p.getProperty(name, Boolean.toString(original)));
    }
    private static int integer(Properties p, String key, int original, int min, int max) {
        try { return Math.max(min,Math.min(max,Integer.parseInt(p.getProperty(key,""+original)))); }
        catch (NumberFormatException ex) { return original; }
    }
    public static synchronized void load() {
        if (loaded) return;
        loaded=true;
        Properties p=new Properties();
        if(Files.isRegularFile(file()))try(InputStream in=Files.newInputStream(file())){p.load(in);}
        catch(IOException ex){System.err.println("[ALN] Minimap settings: "+ex);}
        enabled=flag(p,"enabled",enabled); round=flag(p,"round",round);
        showNorth=flag(p,"showNorth",showNorth);showBrand=flag(p,"showBrand",showBrand);
        size=integer(p,"size",size,0,4);
        rotate=flag(p,"rotate",rotate);showUnknownGrid=flag(p,"unknownGrid",showUnknownGrid);
        showCoordinates=flag(p,"showCoordinates",showCoordinates);
        highDetail=flag(p,"highDetail",highDetail);smoothTerrain=flag(p,"smoothTerrain",smoothTerrain);
        luminaFrame=flag(p,"luminaFrame",luminaFrame);
        terrainMinimap=integer(p,"terrainMinimap",terrainMinimap,0,3);
        terrainWorldMap=integer(p,"terrainWorldMap",terrainWorldMap,0,3);
        left=flag(p,"left",left);players=flag(p,"players",players);
        friends=flag(p,"highlightFriends",friends);showFriendNames=flag(p,"friendNames",showFriendNames);
        mobs=flag(p,"monsters",mobs);animals=flag(p,"animals",animals);
        villagers=flag(p,"villagers",villagers);waypoints=flag(p,"waypoints",waypoints);
        waypointLabels=flag(p,"waypointLabels",waypointLabels);
        worldMapEntities=flag(p,"worldMapEntities",worldMapEntities);
        animatedAvatar=flag(p,"animatedAvatar",animatedAvatar);
        // Preferencia nueva; si no existe, respetar la opción previa del Paso 6J.
        playerMarkerStyle=integer(p,"playerMarkerStyle",animatedAvatar?0:1,0,4);
        playerShowEquipment=flag(p,"playerShowEquipment",playerMarkerStyle==4);
        playerMarkerColor=integer(p,"playerMarkerColor",playerMarkerColor,0,5);
        if(playerMarkerStyle==4)playerMarkerStyle=0; // migrar el antiguo modo Equipo 3D a Cuerpo+Equipo
        playerMarkerSize=integer(p,"playerMarkerSize",playerMarkerSize,1,28);
        animatedAvatar=playerMarkerStyle==0;
        zoom=integer(p,"zoom",zoom,0,4);entityIconSize=integer(p,"entityIconSize",entityIconSize,3,10);opacity=integer(p,"opacity",opacity,30,100);
        friendNames.clear();
        for(String name:p.getProperty("friendList","").split(",")) {
            String n=name.trim().toLowerCase(Locale.ROOT);
            if(!n.isEmpty() && n.length()<=40 && friendNames.size()<64) friendNames.add(n);
        }
    }
    public static synchronized void save(){
        load();Properties p=new Properties();
        p.setProperty("enabled",""+enabled);p.setProperty("round",""+round);
        p.setProperty("showNorth",""+showNorth);p.setProperty("showBrand",""+showBrand);
        p.setProperty("size",""+size);
        p.setProperty("showCoordinates",""+showCoordinates);
        p.setProperty("highDetail",""+highDetail);p.setProperty("smoothTerrain",""+smoothTerrain);
        p.setProperty("luminaFrame",""+luminaFrame);
        p.setProperty("terrainMinimap",""+terrainMinimap);
        p.setProperty("terrainWorldMap",""+terrainWorldMap);
        p.setProperty("rotate",""+rotate);p.setProperty("unknownGrid",""+showUnknownGrid);
        p.setProperty("left",""+left);p.setProperty("players",""+players);
        p.setProperty("highlightFriends",""+friends);p.setProperty("friendNames",""+showFriendNames);
        p.setProperty("monsters",""+mobs);p.setProperty("animals",""+animals);
        p.setProperty("villagers",""+villagers);p.setProperty("waypoints",""+waypoints);
        p.setProperty("waypointLabels",""+waypointLabels);
        p.setProperty("worldMapEntities",""+worldMapEntities);
        animatedAvatar=playerMarkerStyle==0;
        p.setProperty("animatedAvatar",""+animatedAvatar); // compatibilidad 6J
        p.setProperty("playerMarkerStyle",""+playerMarkerStyle);
        p.setProperty("playerMarkerSize",""+playerMarkerSize);
        p.setProperty("playerShowEquipment",""+playerShowEquipment);
        p.setProperty("playerMarkerColor",""+playerMarkerColor);
        p.setProperty("zoom",""+zoom);p.setProperty("entityIconSize",""+entityIconSize);p.setProperty("opacity",""+opacity);
        p.setProperty("friendList",String.join(",",friendNames));
        try {
            Path f=file(); Files.createDirectories(f.getParent());
            Path tmp=Files.createTempFile(f.getParent(),"minimap_",".tmp");
            try {try(OutputStream os=Files.newOutputStream(tmp)){p.store(os,"Atlas Lumina Nexus - minimap");}
                try{Files.move(tmp,f,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException ex){Files.move(tmp,f,StandardCopyOption.REPLACE_EXISTING);}}
            finally{Files.deleteIfExists(tmp);}
        }catch(IOException ex){System.err.println("[ALN] Cannot save minimap settings: "+ex);}
    }
    public static boolean isFriend(String name) {load();return friendNames.contains(name.toLowerCase(Locale.ROOT));}
    public static void addFriends(String names) {
        load();for(String name:names.split(",")){
            String n=name.trim().toLowerCase(Locale.ROOT);
            if(!n.isEmpty() && n.length()<=40 && friendNames.size()<64)friendNames.add(n);
        }save();
    }
    public static void removeFriend(String name){load();friendNames.remove(name.toLowerCase(Locale.ROOT));save();}
    public static void clearFriends(){load();friendNames.clear();save();}
}
