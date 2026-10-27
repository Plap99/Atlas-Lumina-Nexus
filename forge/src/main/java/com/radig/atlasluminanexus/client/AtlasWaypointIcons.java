package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.io.*;
import java.util.*;

/** Personal icon settings: separate from existing waypoint JSON for backward compatibility. */
public final class AtlasWaypointIcons {
    public static final String[] NAMES = {"Marcador", "Casa", "Mina", "Bosque", "Gema", "Bandera"};
    private AtlasWaypointIcons() {}
    private static Path cachedFile;
    private static final Properties CACHE=new Properties();
    private static Path file(Minecraft mc) {
        Path wp=AtlasWaypointClient.storageFile(mc);
        if(wp==null)return null;
        return wp.resolveSibling(wp.getFileName().toString()+".icons.properties");
    }
    private static void ensureLoaded(Path p){
        if(Objects.equals(p,cachedFile))return;
        cachedFile=p;CACHE.clear();
        if(p!=null&&Files.isRegularFile(p))try(InputStream in=Files.newInputStream(p)){
            CACHE.load(in);
        }catch(IOException ex){System.err.println("[ALN] Iconos: "+ex.getMessage());}
    }
    public static int get(Minecraft mc, UUID id) {
        Path p=file(mc);ensureLoaded(p);
        try{return Math.max(0,Math.min(NAMES.length-1,Integer.parseInt(CACHE.getProperty(id.toString(),"0"))));}
        catch(NumberFormatException ignored){return 0;}
    }
    public static void set(Minecraft mc,UUID id,int icon) throws IOException {
        Path p=file(mc);
        if(p==null)throw new IOException("No hay mundo abierto");
        ensureLoaded(p);
        Properties prop=new Properties();prop.putAll(CACHE);
        prop.setProperty(id.toString(),Integer.toString(Math.max(0,Math.min(NAMES.length-1,icon))));
        Files.createDirectories(p.getParent());
        Path temp=p.resolveSibling(p.getFileName()+".tmp");
        try(OutputStream out=Files.newOutputStream(temp)){prop.store(out,"Atlas Lumina Nexus waypoint icons");}
        try{Files.move(temp,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException ex){Files.move(temp,p,StandardCopyOption.REPLACE_EXISTING);}
        CACHE.clear();CACHE.putAll(prop);
    }
}
