package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.BuiltInRegistries;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Cartografía ALN de alta definición: una muestra por bloque del terreno recibido.
 * Nunca genera chunks. Los datos 6A/6B permanecen intactos y se usan como respaldo.
 * Escrituras atómicas por región de 64x64 con caché LRU acotada.
 */
public final class AtlasMinimapFineStore {
    private static final int MAGIC=0x414C4E33, VERSION=3;
    private static final int REGION=64, CELLS=REGION*REGION, MAX_CACHED=64;
    private static final int RADIUS=100, SAMPLES_PER_TICK=280;
    private static final int[] OX,OZ;
    private static final Map<Long,Tile> CACHE=new LinkedHashMap<>(80,0.75f,true);
    private static Path directory, legacyDirectory;
    private static String worldKey="";
    private static int cursor,tickCount,nearCursor;
    private static final class Tile {
        final int x,z;
        final int[] colors=new int[CELLS];
        final short[] heights=new short[CELLS];
        final byte[] water=new byte[CELLS];
        final byte[] material=new byte[CELLS];
        final int[] seabed=new int[CELLS];
        final short[] textureIndex=new short[CELLS];
        final List<String> palette=new ArrayList<>();
        final Map<String,Integer> ids=new HashMap<>();
        boolean dirty;
        int textureId(String name){
            if(name==null||name.isEmpty())return 0;
            Integer prior=ids.get(name);if(prior!=null)return prior;
            if(palette.size()>=65534)return 0;
            int id=palette.size()+1;palette.add(name);ids.put(name,id);return id;
        }
        String texture(int i){
            int id=Short.toUnsignedInt(textureIndex[i]);
            return id==0||id>palette.size()?"":palette.get(id-1);
        }
        Tile(int x,int z){this.x=x;this.z=z;Arrays.fill(heights,Short.MIN_VALUE);}
    }
    static {
        List<int[]> points=new ArrayList<>();
        for(int z=-RADIUS;z<=RADIUS;z++)for(int x=-RADIUS;x<=RADIUS;x++)
            if(x*x+z*z<=RADIUS*RADIUS)points.add(new int[]{x,z});
        points.sort(Comparator.comparingInt(p->p[0]*p[0]+p[1]*p[1]));
        OX=new int[points.size()];OZ=new int[points.size()];
        for(int i=0;i<points.size();i++){OX[i]=points.get(i)[0];OZ[i]=points.get(i)[1];}
    }
    private AtlasMinimapFineStore() {}
    private static long key(int x,int z){return ((long)x<<32)|(z&0xffffffffL);}
    private static int offset(int x,int z){return Math.floorMod(z,REGION)*REGION+Math.floorMod(x,REGION);}
    private static Path file(int rx,int rz){return directory.resolve("r_"+rx+"_"+rz+".gz");}
    public static void sync(Minecraft mc){
        Path base=mc.level==null?null:AtlasWaypointClient.storageFile(mc);
        String dim=mc.level==null?"":mc.level.dimension().toString().replaceAll("[^A-Za-z0-9]","_");
        String id=base==null?"":base.toAbsolutePath()+"_"+dim;
        if(id.equals(worldKey))return;
        flush();CACHE.clear();AtlasBlockTextures.clear();worldKey=id;cursor=tickCount=nearCursor=0;
        // ALN 6N uses a side-by-side store. Original 6C/6M fine files remain untouched.
        legacyDirectory=base==null?null:base.resolveSibling(base.getFileName()+"_"+dim+".map.gz_fine_lumina");
        directory=base==null?null:base.resolveSibling(base.getFileName()+"_"+dim+".map.gz_fine_true");
    }
    private static Tile open(int cx,int cz){
        if(directory==null)return null;
        int rx=Math.floorDiv(cx,REGION),rz=Math.floorDiv(cz,REGION);
        long k=key(rx,rz);
        Tile t=CACHE.get(k);if(t!=null)return t;
        t=new Tile(rx,rz);
        Path p=file(rx,rz);
        if(!Files.isRegularFile(p)&&legacyDirectory!=null){
            p=legacyDirectory.resolve("r_"+rx+"_"+rz+".gz");
            if(!Files.isRegularFile(p)){
                Path early=legacyDirectory.resolveSibling(legacyDirectory.getFileName().toString().replace("_fine_lumina","_fine"));
                p=early.resolve("r_"+rx+"_"+rz+".gz");
            }
        }
        if(Files.isRegularFile(p))try(DataInputStream in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(p))))){
            if(in.readInt()!=MAGIC)throw new IOException("ALN tile magic mismatch");
            int version=in.readInt();
            if((version<1||version>3)||in.readInt()!=rx||in.readInt()!=rz)
                throw new IOException("ALN tile header mismatch");
            if(version>=3){
                int n=in.readUnsignedShort();
                for(int j=0;j<n;j++){
                    String tex=in.readUTF();t.palette.add(tex);t.ids.put(tex,j+1);
                }
            }
            for(int i=0;i<CELLS;i++){
                t.colors[i]=in.readInt();t.heights[i]=in.readShort();t.water[i]=in.readByte();
                if(version>=2){t.material[i]=in.readByte();t.seabed[i]=in.readInt();}
                if(version>=3)t.textureIndex[i]=in.readShort();
            }
        }catch(IOException ex){System.err.println("[ALN] Fine minimap read: "+ex);}
        CACHE.put(k,t);
        if(CACHE.size()>MAX_CACHED){
            Iterator<Tile> it=CACHE.values().iterator();
            if(it.hasNext()&&save(it.next()))it.remove();
        }
        return t;
    }
    private static boolean save(Tile t){
        if(!t.dirty)return true;
        if(directory==null)return false;
        try{
            Files.createDirectories(directory);
            Path tmp=Files.createTempFile(directory,"aln_map_",".tmp");
            try{
                try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(tmp))))){
                    out.writeInt(MAGIC);out.writeInt(VERSION);out.writeInt(t.x);out.writeInt(t.z);
                    out.writeShort(t.palette.size());for(String value:t.palette)out.writeUTF(value);
                    for(int i=0;i<CELLS;i++){
                        out.writeInt(t.colors[i]);out.writeShort(t.heights[i]);out.writeByte(t.water[i]);
                        out.writeByte(t.material[i]);out.writeInt(t.seabed[i]);out.writeShort(t.textureIndex[i]);
                    }
                }
                try{Files.move(tmp,file(t.x,t.z),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
                catch(AtomicMoveNotSupportedException e){Files.move(tmp,file(t.x,t.z),StandardCopyOption.REPLACE_EXISTING);}
                t.dirty=false;
            }finally{Files.deleteIfExists(tmp);}
            return true;
        }catch(IOException e){System.err.println("[ALN] Fine minimap save: "+e);return false;}
    }
    public static void flush(){for(Tile t:CACHE.values())save(t);}
    /** Read-only packed ARGB(0..31), height(32..47), water(48). Zero is missing. */
    public static long data(int x,int z){
        Tile t=CACHE.get(key(Math.floorDiv(x,REGION),Math.floorDiv(z,REGION)));
        if(t!=null){int i=offset(x,z);if(t.colors[i]!=0)
            return (t.colors[i]&0xffffffffL)|((long)t.heights[i]&65535L)<<32|((long)t.water[i]&255L)<<48|((long)t.material[i]&255L)<<56;}
        long coarse=AtlasMinimapStore.data(Math.floorDiv(x,2),Math.floorDiv(z,2));
        return coarse==0?0:(coarse&0x0000ffffffffffffL);
    }
    /** Top block texture recorded by ALN 6O; empty on legacy regions. No disk I/O. */
    public static String texture(int x,int z){
        Tile t=CACHE.get(key(Math.floorDiv(x,REGION),Math.floorDiv(z,REGION)));
        return t==null?"":t.texture(offset(x,z));
    }
    public static int color(long p){return (int)p;}
    public static int height(long p){int h=(short)(p>>>32);return p==0||h==Short.MIN_VALUE?Integer.MIN_VALUE:h;}
    public static boolean water(long p){return ((p>>>48)&255)!=0;}
    public static int waterDepth(long p){return (int)((p>>>48)&255);}
    /** Shallow-water seafloor tint; zero for unrecorded or old regions. */
    public static int seabedColor(int x,int z){
        Tile t=CACHE.get(key(Math.floorDiv(x,REGION),Math.floorDiv(z,REGION)));
        return t==null?0:t.seabed[offset(x,z)];
    }
    private static void record(Minecraft mc,int x,int z,boolean refresh){
        if(!mc.level.hasChunk(Math.floorDiv(x,16),Math.floorDiv(z,16)))return;
        Tile t=open(x,z);if(t==null)return;
        int idx=offset(x,z);if(!refresh&&t.colors[idx]!=0&&t.material[idx]!=0&&
           (t.textureIndex[idx]!=0 || Byte.toUnsignedInt(t.material[idx])==AtlasTerrainEngine.WATER))return;
        int y=mc.level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)-1;
        if(y<mc.level.getMinY())return;
        BlockPos p=new BlockPos(x,y,z);
        BlockState state=mc.level.getBlockState(p);
        int c=state.getMapColor(mc.level,p).calculateARGBColor(MapColor.Brightness.NORMAL);
        if((c&0xffffff)==0)c=0xFF57616A;
        c|=0xFF000000;
        short h=(short)Math.max(Short.MIN_VALUE+1,Math.min(Short.MAX_VALUE,y));
        byte water=0;
        int seabed=0;
        if(!state.getFluidState().isEmpty()){
            int floor=mc.level.getHeight(Heightmap.Types.OCEAN_FLOOR,x,z)-1;
            water=(byte)Math.max(1,Math.min(24,y-floor));
            if(floor>=mc.level.getMinY()){
                BlockPos bottom=new BlockPos(x,floor,z);
                seabed=mc.level.getBlockState(bottom).getMapColor(mc.level,bottom)
                        .calculateARGBColor(MapColor.Brightness.NORMAL)|0xFF000000;
            }
        }
        String blockId=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        byte material=(byte)AtlasTerrainEngine.materialFromId(blockId,!state.getFluidState().isEmpty());
        String textureKey=AtlasBlockTextures.textureFor(blockId);
        int textureId=t.textureId(textureKey);
        if(t.colors[idx]!=c||t.heights[idx]!=h||t.water[idx]!=water||t.material[idx]!=material||t.seabed[idx]!=seabed||Short.toUnsignedInt(t.textureIndex[idx])!=textureId){
            t.colors[idx]=c;t.heights[idx]=h;t.water[idx]=water;t.material[idx]=material;t.seabed[idx]=seabed;t.textureIndex[idx]=(short)textureId;t.dirty=true;
        }
    }
    public static void tick(Minecraft mc){
        sync(mc);
        if(directory==null||mc.player==null||mc.level==null)return;
        if(++tickCount%120==0)flush();
        int px=(int)Math.floor(mc.player.getX()),pz=(int)Math.floor(mc.player.getZ());
        for(int i=0;i<SAMPLES_PER_TICK;i++){
            int n=cursor++%OX.length;record(mc,px+OX[n],pz+OZ[n],false);
        }
        // Keep nearby builds, removed blocks, doors, etc. current without scanning the whole map.
        for(int i=0;i<28;i++){
            int n=nearCursor++%(17*17);
            record(mc,px+(n%17)-8,pz+(n/17)-8,true);
        }
    }
}
