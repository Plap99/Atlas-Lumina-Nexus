package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** HUD sizing is saved independently of waypoint data. Supports the original 5I preferences. */
public final class AtlasDisplayConfig {
    private static final int[] OLD_ICONS={8,10,12,14,16};
    private static final int[] OLD_TEXT={70,85,100,115,130};
    private static int iconSize=10, textPercent=100;
    private static boolean loaded;
    private AtlasDisplayConfig() {}
    private static Path file(){return Minecraft.getInstance().gameDirectory.toPath().resolve("config/atlasluminanexus/appearance.properties");}
    private static int parse(Properties p,String key,int def){try{return Integer.parseInt(p.getProperty(key,""+def));}catch(NumberFormatException e){return def;}}
    private static int clamp(int n,int low,int high){return Math.max(low,Math.min(high,n));}
    public static synchronized void load(){
        if(loaded)return;loaded=true;
        Properties p=new Properties();
        if(Files.isRegularFile(file()))try(InputStream in=Files.newInputStream(file())){p.load(in);}catch(IOException ex){System.err.println("[ALN] Aspecto: "+ex);}
        int oi=clamp(parse(p,"hudIconIndex",1),0,OLD_ICONS.length-1);
        int ot=clamp(parse(p,"hudTextIndex",2),0,OLD_TEXT.length-1);
        iconSize=clamp(parse(p,"hudIconPx",OLD_ICONS[oi]),4,16);
        textPercent=clamp(parse(p,"hudTextPercent",OLD_TEXT[ot]),30,150);
    }
    public static int iconSize(){load();return iconSize;}
    public static int textPercent(){load();return textPercent;}
    public static float textScale(){return textPercent()/100f;}
    public static synchronized void setIconSize(int n){load();iconSize=clamp(n,4,16);save();}
    public static synchronized void setTextPercent(int n){load();textPercent=clamp(n,30,150);save();}
    public static void nextIconSize(){setIconSize(iconSize()+1>16?4:iconSize()+1);}
    public static void nextTextScale(){setTextPercent(textPercent()+5>150?30:textPercent()+5);}
    public static synchronized void save(){
        load();Properties p=new Properties();p.setProperty("hudIconPx",""+iconSize);p.setProperty("hudTextPercent",""+textPercent);
        Path f=file();try{Files.createDirectories(f.getParent());Path tmp=Files.createTempFile(f.getParent(),"appearance_",".tmp");
            try{try(OutputStream out=Files.newOutputStream(tmp)){p.store(out,"Atlas Lumina Nexus - HUD");}
                try{Files.move(tmp,f,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException e){Files.move(tmp,f,StandardCopyOption.REPLACE_EXISTING);}}
            finally{Files.deleteIfExists(tmp);}
        }catch(IOException ex){System.err.println("[ALN] No se guardo aspecto: "+ex);}
    }
}
