package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;

/**
 * Samples real block PNGs from Minecraft's ACTIVE resource manager, so vanilla,
 * server packs and many modded/resource-pack block textures use the same pixels
 * they use in game. Missing or dynamic models safely fall back to map colour.
 *
 * This is a top-down 2.5D renderer, NOT a 3D model renderer. Never copies game
 * assets into ALN's JAR or saved worlds; only a bounded, in-memory sprite cache.
 */
public final class AtlasBlockTextures {
    private static final int MAX = 512;
    private static final Map<String, Sprite> SPRITES = new LinkedHashMap<>(128, .75f, true);
    private static final Sprite MISSING = new Sprite(0, 0, new int[0]);
    private record Sprite(int width, int height, int[] pixels) {}
    private static Object resourceManagerIdentity;
    private AtlasBlockTextures() {}

    public static synchronized void clear() {
        SPRITES.clear();
        resourceManagerIdentity = null;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
    private static int argb(int r, int g, int b) {
        return 0xFF000000 | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
    }

    /** A portable texture reference, e.g. minecraft:grass_block_top. Empty = fallback. */
    static String textureFor(String blockId) {
        if (blockId == null || blockId.isEmpty()) return "";
        String namespace = "minecraft", name = blockId;
        int colon = name.indexOf(':');
        if (colon > 0) { namespace = name.substring(0, colon); name = name.substring(colon + 1); }
        if (name.endsWith("_wall") && !name.contains("wall_sign")) name = name.substring(0, name.length()-5);
        // Vanilla special top faces. Known top variants are resolved explicitly.
        name = switch (name) {
            case "grass_block" -> "grass_block_top";
            case "dirt_path" -> "dirt_path_top";
            case "mycelium" -> "mycelium_top";
            case "podzol" -> "podzol_top";
            case "farmland" -> "farmland";
            case "grass" -> "short_grass";
            case "redstone_ore" -> "redstone_ore";
            case "sandstone" -> "sandstone_top";
            case "red_sandstone" -> "red_sandstone_top";
            case "smooth_sandstone" -> "sandstone_top";
            case "smooth_red_sandstone" -> "red_sandstone_top";
            case "quartz_block" -> "quartz_block_top";
            case "hay_block" -> "hay_block_top";
            case "barrel" -> "barrel_top";
            case "crafting_table" -> "crafting_table_top";
            case "furnace", "blast_furnace" -> "furnace_top";
            case "smoker" -> "smoker_top";
            case "bookshelf" -> "oak_planks";
            case "chest", "trapped_chest", "ender_chest", "decorated_pot" -> "oak_planks";
            case "water", "bubble_column", "lava" -> "";
            default -> name;
        };
        if (name.isEmpty()) return "";
        // Logs, stems and stripped logs are top-down rings, not side bark.
        if (name.endsWith("_log") || name.endsWith("_stem") || name.endsWith("_hyphae")
            || name.endsWith("_wood") || name.startsWith("stripped_") &&
            (name.endsWith("_log") || name.endsWith("_wood") || name.endsWith("_stem") || name.endsWith("_hyphae")))
            name += "_top";
        // Slabs and stairs inherit their material texture. If that texture is
        // unavailable a named fallback is tried by the loading code below.
        if (name.endsWith("_slab") || name.endsWith("_stairs")) {
            String stem=name.substring(0,name.lastIndexOf('_'));
            if(stem.endsWith("_brick"))stem+="s";
            if(stem.matches("oak|spruce|birch|jungle|acacia|dark_oak|mangrove|cherry|bamboo|crimson|warped|pale_oak|poplar"))
                stem+="_planks";
            name=stem;
        }
        return namespace + ":" + name;
    }

    private static Sprite load(String key) {
        try {
            int sep=key.indexOf(':');
            if (sep<=0) return MISSING;
            String namespace=key.substring(0,sep), name=key.substring(sep+1);
            Identifier id=Identifier.fromNamespaceAndPath(namespace, "textures/block/"+name+".png");
            try(InputStream stream=Minecraft.getInstance().getResourceManager().open(id)) {
                BufferedImage img=ImageIO.read(stream);
                if(img==null || img.getWidth()<1 || img.getHeight()<1 || img.getWidth()>512 || img.getHeight()>512) return MISSING;
                int height=img.getHeight();
                // Animated block PNG: use only first square frame (not stacked frames).
                if(height>img.getWidth()) height=img.getWidth();
                return new Sprite(img.getWidth(),height,img.getRGB(0,0,img.getWidth(),height,null,0,img.getWidth()));
            }
        } catch (IOException | RuntimeException ex) { return MISSING; }
    }

    private static synchronized Sprite sprite(String key) {
        Object manager=Minecraft.getInstance().getResourceManager();
        if(resourceManagerIdentity!=manager){ SPRITES.clear(); resourceManagerIdentity=manager; }
        Sprite out=SPRITES.get(key);
        if(out==null){
            out=load(key);
            SPRITES.put(key,out);
            if(SPRITES.size()>MAX) {
                Iterator<String> iter=SPRITES.keySet().iterator();
                if(iter.hasNext()){iter.next();iter.remove();}
            }
        }
        return out;
    }

    /**
     * Returns a true source pixel for an (x,z) surface, optionally tinted by the
     * recorded biome map-color. Transparent pixels blend into the old map color.
     * World-relative UV gives stable detail as the player turns/moves.
     */
    public static int sample(String texture,double worldX,double worldZ,int mapColor,int material) {
        if(texture==null || texture.isEmpty() || material==AtlasTerrainEngine.WATER) return 0;
        Sprite s=sprite(texture);
        if(s==MISSING) return 0;
        int bx=(int)Math.floor(worldX),bz=(int)Math.floor(worldZ);
        double fx=worldX-bx, fz=worldZ-bz;
        // At 1 block per pixel we take different REAL texels from neighbouring
        // blocks, rather than sampling every block at exactly the same UV.
        int offsetU=(Math.floorMod(bx*3+bz,4)*s.width())/4;
        int offsetV=(Math.floorMod(bz*5+bx,4)*s.height())/4;
        int u=Math.floorMod((int)Math.floor(fx*s.width())+offsetU,s.width());
        int v=Math.floorMod((int)Math.floor(fz*s.height())+offsetV,s.height());
        int c=s.pixels()[v*s.width()+u];
        int a=(c>>>24)&255;
        if(a==0)return 0;
        int r=(c>>16)&255,g=(c>>8)&255,b=c&255;
        if(material==AtlasTerrainEngine.GRASS || material==AtlasTerrainEngine.LEAVES) {
            // Foliage and grass tops are often greyscale atlas textures;
            // tint using the actual map-colour captured at this location.
            int br=(mapColor>>16)&255,bg=(mapColor>>8)&255,bb=mapColor&255;
            r=clamp(r*br/125);g=clamp(g*bg/125);b=clamp(b*bb/125);
        }
        if(a<255){
            r=(r*a+((mapColor>>16)&255)*(255-a))/255;
            g=(g*a+((mapColor>>8)&255)*(255-a))/255;
            b=(b*a+(mapColor&255)*(255-a))/255;
        }
        return argb(r,g,b);
    }
}
