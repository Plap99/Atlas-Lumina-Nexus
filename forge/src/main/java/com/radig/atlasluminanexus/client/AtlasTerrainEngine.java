package com.radig.atlasluminanexus.client;

/**
 * Lumina Terrain Engine - portable CPU-side 2.5D renderer for known map columns.
 * No game or graphics calls; colours are stable in world coordinates.
 * Quality 0: Classic. Quality 1: Detailed. Quality 2: Lumina HD.
 * Detailed and HD use REAL top-face pixels from the active resource packs when
 * available, backed by original map-colour for legacy/unresolved models.
 */
public final class AtlasTerrainEngine {
    public static final int CLASSIC=0, DETAILED=1, LUMINA_HD=2, BALANCED=3;
    public static final String[] NAMES={"Clásico", "Detallado", "Lumina HD", "Lumina Balanced"};
    // 0: old unmigrated material, 1: generic; remaining values: known types.
    public static final int OTHER=1, GRASS=2, LEAVES=3, SAND=4, ROCK=5,
            WOOD=6, SNOW=7, SOIL=8, PATH=9, ORE=10, WATER=11,
            ICE=12, NETHER=13, SOUL=14;
    private AtlasTerrainEngine() {}
    public static int materialFromId(String id, boolean fluid){
        if(fluid)return WATER;
        if(id==null)return OTHER;
        String n=id.toLowerCase(java.util.Locale.ROOT);
        if(n.contains("leaves")||n.contains("moss_block")||n.contains("azalea"))return LEAVES;
        if(n.contains("grass")||n.contains("mycelium")||n.contains("podzol"))return GRASS;
        if(n.contains("sand")||n.contains("gravel"))return SAND;
        if(n.contains("log")||n.contains("planks")||n.contains("wood")||n.contains("bamboo")||n.contains("stripped_"))return WOOD;
        if(n.contains("snow")||n.contains("powder_snow"))return SNOW;
        if(n.contains("ice"))return ICE;
        if(n.contains("soul_"))return SOUL;
        if(n.contains("nether")||n.contains("basalt")||n.contains("blackstone")||n.contains("magma"))return NETHER;
        if(n.contains("path")||n.contains("brick")||n.contains("tile")||n.contains("concrete"))return PATH;
        if(n.contains("ore"))return ORE;
        if(n.contains("stone")||n.contains("cobble")||n.contains("deepslate")||n.contains("tuff")||n.contains("andesite")||n.contains("diorite")||n.contains("granite"))return ROCK;
        if(n.contains("dirt")||n.contains("mud")||n.contains("clay"))return SOIL;
        return OTHER;
    }
    private static int cap(int n){return n<0?0:Math.min(255,n);}
    private static int rgb(int r,int g,int b){return 0xFF000000|(cap(r)<<16)|(cap(g)<<8)|cap(b);}
    private static int hash(int x,int z){
        int v=x*0x7feb352d ^ z*0x846ca68b;
        v^=v>>>16;v*=0x7feb352d;v^=v>>>15;v*=0x846ca68b;return v^(v>>>16);
    }
    private static int height(long p){return AtlasMinimapFineStore.height(p);}
    private static int neighbor(int x,int z){return height(AtlasMinimapFineStore.data(x,z));}
    /** One stored world block -> one shaded pixel. Colour 0 means unexplored. */
    static int column(int x,int z,int quality){return pixel(x+.5,z+.5,quality);}
    private static int pixel(double wx,double wz,int quality){
        int x=(int)Math.floor(wx),z=(int)Math.floor(wz);
        long tile=AtlasMinimapFineStore.data(x,z);
        if(tile==0)return 0;
        int base=(int)tile;
        if(quality<=CLASSIC)return base;
        int material=(int)((tile>>>56)&255);
        if(material==0){
            int r=(base>>16)&255,g=(base>>8)&255,b=base&255;
            material=AtlasMinimapFineStore.water(tile)?WATER:
                (g>r*1.20&&g>b*1.18?GRASS:r>g*1.45&&g>b*1.13?SOIL:OTHER);
        }
        int h=height(tile);
        int noise=hash(x,z);
        int grain=((noise>>>22)&15)-7;
        int r=(base>>16)&255,g=(base>>8)&255,b=base&255;
        // Pixel de la textura SUPERIOR del bloque real, cuando exista en el pack activo.
        String top=AtlasMinimapFineStore.texture(x,z);
        int original=AtlasBlockTextures.sample(top,wx,wz,base,material);
        boolean textured=original!=0;
        if(textured){r=(original>>16)&255;g=(original>>8)&255;b=original&255;}
        // Luz del noroeste, sombreado por relieve y oclusión de contacto.
        // Las muestras faltantes nunca se interpretan como precipicios.
        int shade=0;
        if(h!=Integer.MIN_VALUE){
            int west=neighbor(x-1,z),east=neighbor(x+1,z);
            int north=neighbor(x,z-1),south=neighbor(x,z+1);
            shade=reliefShade(h,west,east,north,south,quality);
            if(quality==LUMINA_HD || quality==BALANCED){
                shade-=castShadow(x,z,h); // árboles, casas y riscos, sin ray-tracing 3D
                shade-=contactOcclusion(h,west,east,north,south);
                shade-=edgeDetail(x,z,h,material,east,south);
            }
        }
        int detail=0;
        switch(material){
            case WATER -> {
                int depth=AtlasMinimapFineStore.waterDepth(tile);
                // Coasts are brighter; deep water is cooler and less transparent.
                int deep=Math.min(24,Math.max(0,depth-1)*2);
                r=37-deep/5;g=139-deep; b=175-deep/2;
                int ripple=((hash(x/3,z/2)>>>22)&7)-3;
                detail=quality==LUMINA_HD?ripple*2:ripple;
                // Transparent-look shallow water: mix the real recorded seabed map colour.
                int floor=AtlasMinimapFineStore.seabedColor(x,z);
                if(depth<=10&&floor!=0){
                    int visibility=Math.max(0,10-depth)*5;
                    r=(r*(100-visibility)+((floor>>>16)&255)*visibility)/100;
                    g=(g*(100-visibility)+((floor>>>8)&255)*visibility)/100;
                    b=(b*(100-visibility)+(floor&255)*visibility)/100;
                }else if(depth<=3){r+=14;g+=9;b+=4;}
                shade=shade/4 + coastLight(x,z,depth);
            }
            case LEAVES -> {
                detail=(grain*2)+(((noise&7)==1)?15:0);
                if(quality==LUMINA_HD){
                    int edge=0;
                    for(int dx=-1;dx<=1;dx+=2){
                        long n=AtlasMinimapFineStore.data(x+dx,z);
                        if(n!=0&&((n>>>56)&255)!=LEAVES)edge++;
                    }
                    detail-=edge*7;
                }
                r-=6;g+=3;
            }
            case GRASS -> {detail=grain+(quality==LUMINA_HD&&((noise&31)==0)?14:0);g+=4;}
            case ROCK, ORE -> {detail=grain*2 + (((x+z+h)&7)==0?-8:0);}
            case WOOD -> {detail=grain+(x%4==0?-9:4);}
            case SAND -> {detail=grain/2+(((noise&31)==3)?-11:1);r+=8;g+=5;}
            case SNOW -> {detail=grain/3;r+=6;g+=6;b+=7;}
            case ICE -> {detail=grain;r-=4;g+=7;b+=11;}
            case SOIL, PATH -> {detail=grain+(quality==LUMINA_HD&&(noise&7)==0?-7:0);}
            case NETHER, SOUL -> {detail=grain*2;}
            default -> {detail=grain/2;}
        }
        if(quality==DETAILED){shade=shade*3/4;detail=detail*2/3;}
        // 6Q: HDR-like contrast reduction: vegetation retains hue without neon highlights.
        if(material==LEAVES || material==GRASS){
            // Pull overbright foliage toward natural midtones without masking the original texture.
            int peak=Math.max(r,Math.max(g,b));
            if(peak>165){int excess=peak-165;r-=excess/5;g-=excess/4;b-=excess/5;}
        }
        if(quality==BALANCED){
            // Less visual noise than HD, but retain genuine texture and restrained terrain depth.
            shade=shade*2/5;
            detail=detail/3;
            if(material==LEAVES){shade=shade/2;detail=0;}
            if(material==PATH || material==WOOD || material==ROCK)shade=shade*3/4;
        }else if(quality==LUMINA_HD){
            // Preserve depth while preventing almost-black patches and extreme highlights.
            shade=shade*3/5;
            detail=detail/2;
            if(material==LEAVES)shade=shade*2/3;
        }
        if(textured)detail=0; // texture already provides grain and real surface detail
        int light=shade+detail;
        int litR=cap(r+light),litG=cap(g+light),litB=cap(b+light);
        // Fluorescent biome tint was obscuring paths/buildings. Restrict only
        // excessively bright green surfaces; preserve their hue and relief.
        if((quality==BALANCED||quality==LUMINA_HD)
                && (material==GRASS||material==LEAVES)
                && litG>litR*1.10 && litG>litB*1.10){
            int greenLimit=material==GRASS
                    ? (quality==BALANCED?180:192)
                    : (quality==BALANCED?170:185);
            if(litG>greenLimit){
                double factor=(double)greenLimit/litG;
                litR=cap((int)Math.round(litR*factor));
                litG=greenLimit;
                litB=cap((int)Math.round(litB*factor));
            }
        }
        return rgb(litR,litG,litB);
    }
    private static int limit(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private static int knownHeight(int candidate,int fallback){
        return candidate==Integer.MIN_VALUE?fallback:candidate;
    }
    /** Deterministic illumination of the height field, with light coming from NW.
     * Package-private to permit lightweight tests without the Minecraft runtime.
     * Height changes are bounded so sharp cliffs remain legible rather than pure black.
     */
    static int reliefShade(int h,int west,int east,int north,int south,int quality){
        west=knownHeight(west,h);east=knownHeight(east,h);
        north=knownHeight(north,h);south=knownHeight(south,h);
        int slopeX=limit(east-west,-10,10);
        int slopeZ=limit(south-north,-10,10);
        // Height grows towards east/south => west/north facing sides brighten.
        int illumination=limit((slopeX+slopeZ)*2,-26,26);
        return quality==DETAILED ? illumination*2/3 : illumination;
    }
    /** Contact AO. Only HIGHER neighboring surfaces occlude this column. */
    static int contactOcclusion(int h,int west,int east,int north,int south){
        int occlusion=0;
        if(west!=Integer.MIN_VALUE)occlusion+=limit(west-h-1,0,7);
        if(east!=Integer.MIN_VALUE)occlusion+=limit(east-h-1,0,7);
        if(north!=Integer.MIN_VALUE)occlusion+=limit(north-h-1,0,7);
        if(south!=Integer.MIN_VALUE)occlusion+=limit(south-h-1,0,7);
        return Math.min(16,occlusion);
    }
    /** Soft cast shadow along a north-west sun ray, using only stored heights.
     * Work is bounded (five samples, no chunk access) and stable between frames.
     */
    private static int castShadow(int x,int z,int h){
        int darkness=0;
        for(int i=1;i<=4;i++){
            int prev=neighbor(x-i,z-i);
            if(prev==Integer.MIN_VALUE)continue;
            int rise=prev-h-i; // light travels down about one block per diagonal cell
            if(rise>0)darkness=Math.max(darkness,limit(rise*3/(i+1),0,20));
        }
        // Two extra off-axis samples soften diagonal aliasing.
        int nw=neighbor(x-2,z-1),wn=neighbor(x-1,z-2);
        if(nw!=Integer.MIN_VALUE)darkness=Math.max(darkness,limit((nw-h-2),0,10));
        if(wn!=Integer.MIN_VALUE)darkness=Math.max(darkness,limit((wn-h-2),0,10));
        return darkness;
    }
    /** Thin contact line for steps and hard roof/road boundaries, not every leaf. */
    private static int edgeDetail(int x,int z,int h,int material,int east,int south){
        if(material==WATER||material==LEAVES||material==GRASS)return 0;
        int result=0;
        if(east!=Integer.MIN_VALUE&&east>h+1)result+=7;
        if(south!=Integer.MIN_VALUE&&south>h+1)result+=7;
        if(material==WOOD||material==ROCK||material==PATH){
            long a=AtlasMinimapFineStore.data(x+1,z);
            long b=AtlasMinimapFineStore.data(x,z+1);
            if(a!=0&&(int)((a>>>56)&255)!=material)result+=2;
            if(b!=0&&(int)((b>>>56)&255)!=material)result+=2;
        }
        return Math.min(10,result/2);
    }
    /** Shores are slightly brighter; deeper recorded water gets cooler/darker. */
    private static int coastLight(int x,int z,int depth){
        if(depth>4)return -Math.min(16,depth-4);
        int shore=0;
        long w=AtlasMinimapFineStore.data(x-1,z);
        long e=AtlasMinimapFineStore.data(x+1,z);
        long n=AtlasMinimapFineStore.data(x,z-1);
        long s=AtlasMinimapFineStore.data(x,z+1);
        if(w!=0&&!AtlasMinimapFineStore.water(w))shore++;
        if(e!=0&&!AtlasMinimapFineStore.water(e))shore++;
        if(n!=0&&!AtlasMinimapFineStore.water(n))shore++;
        if(s!=0&&!AtlasMinimapFineStore.water(s))shore++;
        return Math.min(15,shore*5);
    }
    /** Nearest-world-block samples at close zoom, capped multisamples at distant zoom. */
    public static int sample(double wx,double wz,double blocksPerPixel,int quality,boolean smooth){
        int x=(int)Math.floor(wx),z=(int)Math.floor(wz);
        int center=pixel(wx,wz,quality);
        if(!smooth||blocksPerPixel<1.5)return center;
        double d=Math.min(1.7,blocksPerPixel*.24);
        int[] colors={pixel(wx-d,wz-d,quality),pixel(wx+d,wz-d,quality),
                pixel(wx-d,wz+d,quality),pixel(wx+d,wz+d,quality)};
        int cr=0,cg=0,cb=0,n=0;
        for(int c:colors)if(c!=0){cr+=(c>>>16)&255;cg+=(c>>>8)&255;cb+=c&255;n++;}
        if(n==0)return center;
        int mean=rgb(cr/n,cg/n,cb/n);
        if(center==0)return mean;
        return rgb((((center>>16)&255)*2+((mean>>16)&255))/3,
                (((center>>8)&255)*2+((mean>>8)&255))/3,
                ((center&255)*2+(mean&255))/3);
    }
}
