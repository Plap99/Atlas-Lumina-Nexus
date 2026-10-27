package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Cartografia persistente por mundo y dimension.
 * - Conserva el archivo .map.gz de 6A como respaldo de baja resolucion (4 bloques).
 * - Guarda nueva cartografia de 2 bloques en regiones comprimidas independientes.
 * - Solo consulta chunks ya recibidos por el cliente; no fuerza su generacion.
 * - El cache de regiones se limita a 48 entradas, pero los mapas en disco no se eliminan.
 */
public final class AtlasMinimapStore {
    private static final int LEGACY_MAGIC=0x414C4E4D, LEGACY_VERSION=1;
    private static final int MAGIC=0x414C4E32, VERSION=1;
    private static final int REGION_SIDE=64, REGION_CELLS=REGION_SIDE*REGION_SIDE;
    private static final int CACHE_LIMIT=48, MAX_OLD_CELLS=350000;
    private static final int SAMPLE_RADIUS=96, SAMPLES_PER_TICK=200;
    private static final Map<Long,Integer> LEGACY=new HashMap<>();
    private static final LinkedHashMap<Long,Region> CACHE=new LinkedHashMap<>(64,.75f,true);
    private static final int[] SAMPLE_DX;
    private static final int[] SAMPLE_DZ;
    private static Path detailDir;
    private static String identity="";
    private static int cursor,ticks,refreshCursor;

    private static final class Region {
        final int x,z;
        final int[] colors=new int[REGION_CELLS];
        final short[] heights=new short[REGION_CELLS];
        boolean dirty;
        Region(int x,int z){this.x=x;this.z=z;Arrays.fill(heights,Short.MIN_VALUE);}
    }
    public record Sample(int color,int height) {
        public boolean known(){return color!=0;}
    }
    private static final Sample UNKNOWN=new Sample(0,Integer.MIN_VALUE);

    static {
        // Anillos concentrados: el centro del minimapa se dibuja primero al entrar.
        ArrayList<int[]> offsets=new ArrayList<>();
        for(int z=-SAMPLE_RADIUS;z<=SAMPLE_RADIUS;z++)
            for(int x=-SAMPLE_RADIUS;x<=SAMPLE_RADIUS;x++)
                if(x*x+z*z<=SAMPLE_RADIUS*SAMPLE_RADIUS)offsets.add(new int[]{x,z});
        offsets.sort(Comparator.comparingInt(a->a[0]*a[0]+a[1]*a[1]));
        SAMPLE_DX=new int[offsets.size()];SAMPLE_DZ=new int[offsets.size()];
        for(int i=0;i<offsets.size();i++){
            SAMPLE_DX[i]=offsets.get(i)[0];SAMPLE_DZ[i]=offsets.get(i)[1];
        }
    }
    private AtlasMinimapStore() {}
    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private static int index(int x,int z){return Math.floorMod(z,REGION_SIDE)*REGION_SIDE+Math.floorMod(x,REGION_SIDE);}
    private static String dimName(Minecraft mc){return mc.level==null?"":mc.level.dimension().toString();}
    private static Path legacyPath(Minecraft mc){
        if(mc.level==null)return null;
        Path wp=AtlasWaypointClient.storageFile(mc);
        if(wp==null)return null;
        String dimension=dimName(mc).replaceAll("[^A-Za-z0-9]","_");
        return wp.resolveSibling(wp.getFileName().toString()+"_"+dimension+".map.gz");
    }
    private static Path regionPath(int x,int z){return detailDir.resolve("r_"+x+"_"+z+".gz");}
    private static void readLegacy(Path path){
        LEGACY.clear();
        if(path==null||!Files.isRegularFile(path))return;
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path))))) {
            if(in.readInt()!=LEGACY_MAGIC||in.readInt()!=LEGACY_VERSION)throw new IOException("Unknown legacy map format");
            int n=in.readInt();if(n<0||n>MAX_OLD_CELLS)throw new IOException("Invalid legacy map size");
            for(int i=0;i<n;i++)LEGACY.put(in.readLong(),in.readInt());
        }catch(Exception ex){LEGACY.clear();System.err.println("[ALN] Legacy map read failed: "+ex);}
    }
    public static void sync(Minecraft mc){
        Path old=legacyPath(mc);
        String next=old==null?"":old.toAbsolutePath().toString();
        if(next.equals(identity))return;
        flush();CACHE.clear();identity=next;detailDir=null;cursor=ticks=refreshCursor=0;
        readLegacy(old);
        if(old!=null)detailDir=old.resolveSibling(old.getFileName().toString()+"_detail");
    }
    private static Region openRegion(int cx,int cz){
        if(detailDir==null)return null;
        int rx=Math.floorDiv(cx,REGION_SIDE),rz=Math.floorDiv(cz,REGION_SIDE);
        long k=key(rx,rz);
        Region r=CACHE.get(k);
        if(r!=null)return r;
        r=new Region(rx,rz);
        Path path=regionPath(rx,rz);
        if(Files.isRegularFile(path)){
            try(DataInputStream in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path))))) {
                if(in.readInt()!=MAGIC||in.readInt()!=VERSION)throw new IOException("Unknown detailed map format");
                if(in.readInt()!=rx||in.readInt()!=rz)throw new IOException("Region location mismatch");
                for(int i=0;i<REGION_CELLS;i++){r.colors[i]=in.readInt();r.heights[i]=in.readShort();}
            }catch(Exception ex){System.err.println("[ALN] Detailed map read failed ("+rx+","+rz+"): "+ex);}
        }
        CACHE.put(k,r);
        if(CACHE.size()>CACHE_LIMIT){
            Iterator<Map.Entry<Long,Region>> it=CACHE.entrySet().iterator();
            if(it.hasNext()){
                Region first=it.next().getValue();
                if(saveRegion(first))it.remove(); // no se descartan datos sucios si falla el guardado
            }
        }
        return r;
    }
    private static boolean saveRegion(Region r){
        if(!r.dirty)return true;
        if(detailDir==null)return false;
        try {
            Files.createDirectories(detailDir);
            Path file=regionPath(r.x,r.z);
            Path tmp=Files.createTempFile(detailDir,"aln_region_",".tmp");
            try {
                try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp))))) {
                    out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(r.x);out.writeInt(r.z);
                    for(int i=0;i<REGION_CELLS;i++){out.writeInt(r.colors[i]);out.writeShort(r.heights[i]);}
                }
                try{Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException ex){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}
                r.dirty=false;
                return true;
            }finally{Files.deleteIfExists(tmp);}
        }catch(Exception ex){System.err.println("[ALN] Detailed map save failed: "+ex);return false;}
    }
    public static void flush(){for(Region r:CACHE.values())saveRegion(r);AtlasMinimapFineStore.flush();}

    /** Lectura ligera para el HUD: sin objetos temporales ni acceso al disco. */
    public static long data(int cellX,int cellZ){
        Region r=CACHE.get(key(Math.floorDiv(cellX,REGION_SIDE),Math.floorDiv(cellZ,REGION_SIDE)));
        if(r!=null){
            int idx=index(cellX,cellZ),color=r.colors[idx];
            if(color!=0)return ((long)r.heights[idx]<<32)|(color&0xffffffffL);
        }
        // Compatibilidad con cartografia 6A (celdas de 4 bloques).
        int legacyX=Math.floorDiv(cellX,2),legacyZ=Math.floorDiv(cellZ,2);
        int c=LEGACY.getOrDefault(key(legacyX,legacyZ),0);
        return c==0?0L:((long)Short.MIN_VALUE<<32)|(c&0xffffffffL);
    }
    public static int color(long sample){return (int)sample;}
    public static int height(long sample){
        return sample==0||((short)(sample>>32))==Short.MIN_VALUE?Integer.MIN_VALUE:(short)(sample>>32);
    }
    public static Sample sample(int cellX,int cellZ){
        long sample=data(cellX,cellZ);
        return sample==0?UNKNOWN:new Sample(color(sample),height(sample));
    }
    public static int color(int fourBlockX,int fourBlockZ){return color(data(fourBlockX*2,fourBlockZ*2));}

    private static void updateCell(Minecraft mc,int cx,int cz,boolean refresh){
        int x=cx*2+1,z=cz*2+1;
        // Se lee la memoria cartografica aunque ese chunk ya no este cargado.
        // Solo para obtener muestras NUEVAS exigimos que el cliente tenga el chunk.
        Region region=openRegion(cx,cz);
        if(region==null)return;
        if(!mc.level.hasChunk(Math.floorDiv(x,16),Math.floorDiv(z,16)))return;
        int idx=index(cx,cz);
        if(!refresh && region.colors[idx]!=0)return;
        int h=mc.level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1;
        if(h<mc.level.getMinY())return;
        BlockPos pos=new BlockPos(x,h,z);
        BlockState state=mc.level.getBlockState(pos);
        int c=state.getMapColor(mc.level,pos).calculateARGBColor(MapColor.Brightness.NORMAL);
        if((c&0xFFFFFF)==0)c=0xFF505861;
        short height=(short)Math.max(Short.MIN_VALUE+1,Math.min(Short.MAX_VALUE,h));
        c=0xFF000000|(c&0x00FFFFFF);
        if(region.colors[idx]!=c||region.heights[idx]!=height){
            region.colors[idx]=c;region.heights[idx]=height;region.dirty=true;
        }
    }
    /** Muestreo incremental del terreno recibido, fuera del renderizado del HUD. */
    public static void tick(Minecraft mc){
        sync(mc);
        if(mc.level==null||mc.player==null||detailDir==null)return;
        if(++ticks%120==0)flush();
        int centerX=Math.floorDiv((int)Math.floor(mc.player.getX()),2);
        int centerZ=Math.floorDiv((int)Math.floor(mc.player.getZ()),2);
        for(int i=0;i<SAMPLES_PER_TICK;i++){
            int p=cursor++%SAMPLE_DX.length;
            updateCell(mc,centerX+SAMPLE_DX[p],centerZ+SAMPLE_DZ[p],false);
        }
        // Al construir cerca, el minimapa refresca el entorno inmediato en pocos ticks.
        for(int i=0;i<8;i++){
            int p=refreshCursor++%(13*13);
            updateCell(mc,centerX+(p%13)-6,centerZ+(p/13)-6,true);
        }
        if(AtlasMinimapConfig.highDetail)AtlasMinimapFineStore.tick(mc);
    }
}
