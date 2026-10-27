package com.radig.atlasluminanexus.client;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Especies vanilla Minecraft 26.3 identificadas por ID de registro, no por nombre visible. */
public final class AtlasEntityCatalog {
    public record Entry(String key,String label) {}
    private AtlasEntityCatalog() {}
    private static final Map<EntityType<?>, String> TYPE_CACHE = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Set<String> MISSING_REPORTED = Collections.synchronizedSet(new HashSet<>());
    private static final Entry[] OVERWORLD = {
        new Entry("wandering_trader","Comerciante errante"),
        new Entry("trader_llama","Llama de comerciante"),
        new Entry("nautilus","Nautilo"),
        new Entry("zombie_nautilus","Nautilo zombi"),
        new Entry("allay","Allay"),
        new Entry("armadillo","Armadillo"),
        new Entry("axolotl","Ajolote"),
        new Entry("bat","Murciélago"),
        new Entry("bee","Abeja"),
        new Entry("bogged","Esqueleto pantanoso"),
        new Entry("breeze","Breeze"),
        new Entry("camel","Camello"),
        new Entry("camel_husk","Camello momificado"),
        new Entry("cat","Gato"),
        new Entry("cave_spider","Araña de cueva"),
        new Entry("chicken","Gallina"),
        new Entry("cod","Bacalao"),
        new Entry("copper_golem","Gólem de cobre"),
        new Entry("cow","Vaca"),
        new Entry("creaking","Crujiente"),
        new Entry("creeper","Creeper"),
        new Entry("dolphin","Delfín"),
        new Entry("donkey","Burro"),
        new Entry("drowned","Ahogado"),
        new Entry("elder_guardian","Guardián anciano"),
        new Entry("enderman","Enderman"),
        new Entry("evoker","Evocador"),
        new Entry("fox","Zorro"),
        new Entry("frog","Rana"),
        new Entry("glow_squid","Calamar brillante"),
        new Entry("goat","Cabra"),
        new Entry("guardian","Guardián"),
        new Entry("happy_ghast","Ghast feliz"),
        new Entry("horse","Caballo"),
        new Entry("husk","Zombi desértico"),
        new Entry("illusioner","Ilusionista"),
        new Entry("iron_golem","Gólem de hierro"),
        new Entry("llama","Llama"),
        new Entry("mooshroom","Vaca champiñón"),
        new Entry("mule","Mula"),
        new Entry("ocelot","Ocelote"),
        new Entry("panda","Panda"),
        new Entry("parched","Esqueleto reseco"),
        new Entry("parrot","Loro"),
        new Entry("phantom","Fantasma"),
        new Entry("pig","Cerdo"),
        new Entry("pillager","Saqueador"),
        new Entry("polar_bear","Oso polar"),
        new Entry("pufferfish","Pez globo"),
        new Entry("rabbit","Conejo"),
        new Entry("ravager","Devastador"),
        new Entry("salmon","Salmón"),
        new Entry("sheep","Oveja"),
        new Entry("silverfish","Lepisma"),
        new Entry("skeleton","Esqueleto"),
        new Entry("skeleton_horse","Caballo esqueleto"),
        new Entry("slime","Slime"),
        new Entry("sniffer","Sniffer"),
        new Entry("snow_golem","Gólem de nieve"),
        new Entry("spider","Araña"),
        new Entry("squid","Calamar"),
        new Entry("stray","Esqueleto glacial"),
        new Entry("tadpole","Renacuajo"),
        new Entry("tropical_fish","Pez tropical"),
        new Entry("turtle","Tortuga"),
        new Entry("vex","Vex"),
        new Entry("villager","Aldeano"),
        new Entry("vindicator","Vindicador"),
        new Entry("warden","Warden"),
        new Entry("witch","Bruja"),
        new Entry("wolf","Lobo"),
        new Entry("zombie","Zombi"),
        new Entry("zombie_horse","Caballo zombi"),
        new Entry("zombie_villager","Aldeano zombi"),
        new Entry("player","Jugador"),
        new Entry("friend","Amigo / equipo")
    };
    private static final Entry[] NETHER = {
        new Entry("blaze","Blaze"),
        new Entry("ghast","Ghast"),
        new Entry("happy_ghast","Ghast feliz"),
        new Entry("hoglin","Hoglin"),
        new Entry("magma_cube","Cubo de magma"),
        new Entry("piglin","Piglin"),
        new Entry("piglin_brute","Piglin bruto"),
        new Entry("strider","Strider"),
        new Entry("sulfur_cube","Cubo de azufre"),
        new Entry("wither_skeleton","Esqueleto wither"),
        new Entry("wither","Wither"),
        new Entry("zoglin","Zoglin"),
        new Entry("zombified_piglin","Piglin zombificado"),
        new Entry("enderman","Enderman"),
        new Entry("player","Jugador"),
        new Entry("friend","Amigo / equipo")
    };
    private static final Entry[] END = {
        new Entry("enderman","Enderman"),
        new Entry("endermite","Endermite"),
        new Entry("shulker","Shulker"),
        new Entry("ender_dragon","Dragón del End"),
        new Entry("player","Jugador"),
        new Entry("friend","Amigo / equipo")
    };
    private static final Set<String> SUPPORTED = Set.of(
        "allay",
        "armadillo",
        "axolotl",
        "bat",
        "bee",
        "blaze",
        "bogged",
        "breeze",
        "camel",
        "camel_husk",
        "cat",
        "cave_spider",
        "chicken",
        "cod",
        "copper_golem",
        "cow",
        "creaking",
        "creeper",
        "dolphin",
        "donkey",
        "drowned",
        "elder_guardian",
        "ender_dragon",
        "enderman",
        "endermite",
        "evoker",
        "fox",
        "friend",
        "frog",
        "ghast",
        "giant",
        "glow_squid",
        "goat",
        "guardian",
        "happy_ghast",
        "hoglin",
        "horse",
        "husk",
        "illusioner",
        "iron_golem",
        "llama",
        "magma_cube",
        "mooshroom",
        "mule",
        "nautilus",
        "ocelot",
        "panda",
        "parched",
        "parrot",
        "phantom",
        "pig",
        "piglin",
        "piglin_brute",
        "pillager",
        "player",
        "polar_bear",
        "pufferfish",
        "rabbit",
        "ravager",
        "salmon",
        "sheep",
        "shulker",
        "silverfish",
        "skeleton",
        "skeleton_horse",
        "slime",
        "sniffer",
        "snow_golem",
        "spider",
        "squid",
        "stray",
        "strider",
        "sulfur_cube",
        "tadpole",
        "trader_llama",
        "tropical_fish",
        "turtle",
        "vex",
        "villager",
        "vindicator",
        "wandering_trader",
        "warden",
        "witch",
        "wither",
        "wither_skeleton",
        "wolf",
        "zoglin",
        "zombie",
        "zombie_horse",
        "zombie_nautilus",
        "zombie_villager",
        "zombified_piglin"
    );
    public static Entry[] forDimension(String dimension) {
        if (dimension.contains("the_nether")) return NETHER;
        if (dimension.contains("the_end")) return END;
        return OVERWORLD;
    }
    public static String dimensionTitle(String dimension) {
        if (dimension.contains("the_nether")) return "NETHER";
        if (dimension.contains("the_end")) return "END";
        return "OVERWORLD";
    }
    public static Identifier icon(String key) {
        return Identifier.fromNamespaceAndPath("atlasluminanexus", "textures/gui/minimap/entities/"+key+".png");
    }
    public static String keyOf(Entity entity) {
        return TYPE_CACHE.computeIfAbsent(entity.getType(),AtlasEntityCatalog::resolveType);
    }
    private static String resolveType(EntityType<?> type) {
        Identifier id=EntityType.getKey(type);
        if(id==null) return "unknown";
        String key=id.getPath();
        if(SUPPORTED.contains(key))return key;
        if(MISSING_REPORTED.add(id.toString()))
            System.out.println("[ALN] Entidad sin icono especifico: "+id);
        return "unknown";
    }
    public static Identifier icon(Entity entity, boolean friend) {
        return icon(friend?"friend":keyOf(entity));
    }
    public static boolean isVillager(Entity entity) {
        String key=keyOf(entity);
        return key.equals("villager")||key.equals("wandering_trader");
    }
    public static boolean isHostile(Entity entity) {
        if(entity instanceof Monster)return true;
        String k=keyOf(entity);
        return k.equals("ender_dragon")||k.equals("wither")||k.equals("giant")
            ||k.equals("ghast")||k.equals("shulker")||k.equals("hoglin")
            ||k.equals("zoglin")||k.equals("endermite")||k.equals("creaking")
            ||k.equals("parched")||k.equals("camel_husk")||k.equals("zombie_nautilus");
    }
    public static boolean isPassive(Entity entity) {
        // Todos los Mob amistosos, incluidos acuáticos y allays, sin mezclar comerciantes con animales.
        return entity instanceof Mob && !isHostile(entity) && !isVillager(entity);
    }
}
