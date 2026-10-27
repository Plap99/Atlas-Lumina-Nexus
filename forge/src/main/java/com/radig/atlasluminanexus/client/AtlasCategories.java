package com.radig.atlasluminanexus.client;

import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Categorías por mundo; al borrarlas se reasignan los puntos a General. */
public final class AtlasCategories {
    private AtlasCategories() {}
    private static Path path(Minecraft mc) {
        Path p=AtlasWaypointClient.storageFile(mc);
        return p==null ? null : p.resolveSibling(p.getFileName().toString().replace(".json",".categories.txt"));
    }
    public static List<String> all(Minecraft mc) {
        LinkedHashSet<String> result=new LinkedHashSet<>();result.add("General");
        Path p=path(mc);
        try { if(p!=null&&Files.isRegularFile(p)) for(String s:Files.readAllLines(p,StandardCharsets.UTF_8)) {
            String n=s.trim();if(!n.isEmpty())result.add(n);
        }} catch(Exception e){System.err.println("[ALN] Categories read: "+e);}
        for(Waypoint w:AtlasWaypointClient.all(mc))result.add(w.category());
        return new ArrayList<>(result);
    }
    private static void store(Minecraft mc,List<String> values)throws Exception{
        Path p=path(mc);if(p==null)throw new IllegalStateException("Sin mundo abierto");
        Files.createDirectories(p.getParent());
        Path temp=p.resolveSibling(p.getFileName()+".tmp");
        Files.write(temp,values,StandardCharsets.UTF_8);
        Files.move(temp,p,StandardCopyOption.REPLACE_EXISTING);
    }
    public static void add(Minecraft mc,String name)throws Exception{
        String n=name.trim();if(n.isBlank()||n.length()>35||n.contains("\n")||n.contains("\r"))throw new IllegalArgumentException("Nombre inválido");
        List<String> names=all(mc);
        if(names.stream().noneMatch(s->s.equalsIgnoreCase(n)))names.add(n);
        store(mc,names);
    }
    public static void remove(Minecraft mc,String name)throws Exception{
        if(name.equalsIgnoreCase("General"))throw new IllegalArgumentException("General no se puede borrar");
        for(Waypoint w:AtlasWaypointClient.all(mc))if(w.category().equalsIgnoreCase(name)){
            AtlasWaypointClient.upsert(mc,new Waypoint(w.id(),w.name(),w.dimension(),w.x(),w.y(),w.z(),w.argb(),"General",w.visible(),w.emergencyDestination()));
        }
        List<String> names=all(mc);names.removeIf(s->s.equalsIgnoreCase(name));store(mc,names);
    }
}
