package com.radig.atlasluminanexus.client;

import com.radig.atlasluminanexus.core.Waypoint;
import com.radig.atlasluminanexus.core.WaypointJsonStore;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

/** Respaldo ALN nativo; nunca escribe en directorios de otros mods. */
public final class AtlasWaypointTransfer {
    private static final int LIMIT=1500, MAX_IMAGE=600_000, MAX_TOTAL=80_000_000;
    private AtlasWaypointTransfer() {}
    private static Path current(Minecraft mc) throws IOException {
        Path p=AtlasWaypointClient.storageFile(mc);
        if(p==null)throw new IOException("Abre un mundo primero");
        return p;
    }
    public static Path exportCurrent(Minecraft mc) throws Exception {
        Path world=current(mc), folder=mc.gameDirectory.toPath().resolve("config/atlasluminanexus/exports");
        Files.createDirectories(folder);
        String stamp=LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
        Path result=folder.resolve("ALN_"+world.getFileName().toString().replace(".json","")+"_"+stamp+".zip");
        Path tmp=result.resolveSibling(result.getFileName()+".tmp");
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(tmp))){
            out.putNextEntry(new ZipEntry("manifest.txt"));
            out.write(("Atlas Lumina Nexus waypoints v1\nArchivo mundo: "+world.getFileName()+"\n").getBytes(StandardCharsets.UTF_8));out.closeEntry();
            Path src=world;
            if(!Files.isRegularFile(src)){
                Path empty=Files.createTempFile("aln_empty_",".json");
                try{WaypointJsonStore.save(empty,List.of());entry(out,"waypoints.json",empty);}finally{Files.deleteIfExists(empty);}
            }else entry(out,"waypoints.json",src);
            Path icons=world.resolveSibling(world.getFileName()+".icons.properties");
            if(Files.isRegularFile(icons))entry(out,"icons.properties",icons);
            Path cats=world.resolveSibling(world.getFileName().toString().replace(".json",".categories.txt"));
            if(Files.isRegularFile(cats))entry(out,"categories.txt",cats);
            for(Waypoint wp:AtlasWaypointClient.all(mc)){
                Path png=AtlasPreviewClient.previewFile(mc,wp.id());
                if(png!=null&&Files.isRegularFile(png)&&Files.size(png)<=MAX_IMAGE)
                    entry(out,"photos/"+wp.id()+".png",png);
            }
        }catch(Exception ex){Files.deleteIfExists(tmp);throw ex;}
        try{Files.move(tmp,result,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
        catch(AtomicMoveNotSupportedException ex){Files.move(tmp,result,StandardCopyOption.REPLACE_EXISTING);}
        return result;
    }
    private static void entry(ZipOutputStream out,String name,Path file)throws IOException {
        out.putNextEntry(new ZipEntry(name));Files.copy(file,out);out.closeEntry();
    }
    public static String importArchive(Minecraft mc,Path archive,boolean replace)throws Exception {
        current(mc);
        if(!Files.isRegularFile(archive)||Files.size(archive)>MAX_TOTAL)throw new IOException("Archivo ZIP no válido o demasiado grande");
        Map<String,byte[]> contents=new HashMap<>();long total=0;
        try(ZipInputStream zin=new ZipInputStream(Files.newInputStream(archive))){
            ZipEntry item;
            while((item=zin.getNextEntry())!=null){
                String name=item.getName();
                if(!name.equals("waypoints.json")&&!name.equals("icons.properties")&&!name.equals("categories.txt")
                    &&!name.equals("manifest.txt")&&!name.matches("photos/[a-fA-F0-9-]{36}\\.png"))continue;
                int cap=name.startsWith("photos/")?MAX_IMAGE:MAX_TOTAL/10;
                ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] piece=new byte[8192];int n;
                while((n=zin.read(piece))!=-1){
                    if(buffer.size()+n>cap || total+n>MAX_TOTAL)throw new IOException("El respaldo supera los límites de seguridad");
                    buffer.write(piece,0,n);total+=n;
                }
                contents.put(name,buffer.toByteArray());zin.closeEntry();
            }
        }
        if(!contents.containsKey("waypoints.json"))throw new IOException("El ZIP no contiene waypoints de ALN");
        JsonObject root=JsonParser.parseString(new String(contents.get("waypoints.json"),StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray entries=root.getAsJsonArray("waypoints");if(entries==null)throw new IOException("Formato de waypoints incorrecto");
        if(entries.size()>LIMIT)throw new IOException("Demasiados waypoints en el archivo");
        List<Waypoint> imported=new ArrayList<>();Set<UUID> oldIds=new HashSet<>();
        for(JsonElement elem:entries){
            JsonObject j=elem.getAsJsonObject();UUID id=UUID.fromString(j.get("id").getAsString());
            if(!oldIds.add(id))throw new IOException("IDs repetidos en el respaldo");
            String name=j.get("name").getAsString(),dim=j.get("dimension").getAsString();
            if(name.length()>120||dim.length()>150)throw new IOException("Datos incorrectos en el respaldo");
            imported.add(new Waypoint(id,name,dim,j.get("x").getAsDouble(),j.get("y").getAsDouble(),j.get("z").getAsDouble(),
                j.get("argb").getAsInt(),j.get("category").getAsString(),j.get("visible").getAsBoolean(),j.get("emergencyDestination").getAsBoolean()));
        }
        // Validar todo antes de modificar archivos.
        Properties iconProps=new Properties();
        if(contents.containsKey("icons.properties"))try(InputStream in=new ByteArrayInputStream(contents.get("icons.properties"))){iconProps.load(in);}
        List<Waypoint> previous=AtlasWaypointClient.all(mc);
        // Respaldo automático antes de reemplazar, restaurable desde General.
        if(replace)exportCurrent(mc);
        if(replace){for(Waypoint wp:previous)AtlasWaypointClient.delete(mc,wp.id());}
        boolean alreadyHome=!replace&&previous.stream().anyMatch(Waypoint::emergencyDestination);
        int count=0;
        for(Waypoint wp:imported){
            UUID newId=replace?wp.id():UUID.randomUUID();
            boolean home=wp.emergencyDestination()&&!alreadyHome;
            if(home)alreadyHome=true;
            Waypoint added=new Waypoint(newId,wp.name(),wp.dimension(),wp.x(),wp.y(),wp.z(),wp.argb(),wp.category(),wp.visible(),home);
            AtlasWaypointClient.upsert(mc,added);
            byte[] image=contents.get("photos/"+wp.id()+".png");
            if(image!=null&&image.length>8){
                Path photo=AtlasPreviewClient.previewFile(mc,newId);
                if(photo!=null){Files.createDirectories(photo.getParent());Path staged=Files.createTempFile(photo.getParent(),"aln_photo_",".tmp");
                    try{Files.write(staged,image);Files.move(staged,photo,StandardCopyOption.REPLACE_EXISTING);}
                    finally{Files.deleteIfExists(staged);}
                }
            }
            String icon=iconProps.getProperty(wp.id().toString(),"0");
            int which;try{which=Integer.parseInt(icon);}catch(Exception ex){which=0;}
            AtlasWaypointIcons.set(mc,newId,which);
            count++;
        }
        byte[] categories=contents.get("categories.txt");
        if(categories!=null){
            String content=new String(categories,StandardCharsets.UTF_8);
            for(String category:content.split("\\R")){
                String clean=category.trim();
                if(clean.length()>0&&clean.length()<=35&&!clean.equalsIgnoreCase("General"))
                    try{AtlasCategories.add(mc,clean);}catch(Exception ignored){}
            }
        }
        AtlasPreviewClient.clear(null);
        return "Importados "+count+" waypoints"+(replace?" (reemplazo)":" (agregados)");
    }
}
