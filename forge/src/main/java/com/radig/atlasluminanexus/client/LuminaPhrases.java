package com.radig.atlasluminanexus.client;

import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.HashMap;
import java.util.Map;

/** Frases locales de Lumina, seleccionadas al iniciar/terminar el viaje.
 *  No utiliza chat ajeno, no registra posiciones ni realiza llamadas de red.
 */
public final class LuminaPhrases {
    private LuminaPhrases() {}
    private static final Map<String,Integer> last=new HashMap<>();
    private static final String[] GENERIC={
        "El Atlas acaba de descubrir un atajo entre las estrellas.",
        "Lumina, haz tu magia... pero sin perder mis zapatos.",
        "Las rutas del Nexo se alinean. ¡Allá vamos!",
        "Activando viaje arcano. Equipaje de mano, por favor.",
        "Un parpadeo, un destello... y cambiamos de paisaje.",
        "Hoy el transporte público es un círculo mágico.",
        "Si aparecemos en un árbol, no fui yo. Fue la brújula."
    };
    private static final String[] CREEPER={
        "¿Escuchaste ese sssss? Yo tampoco quiero averiguarlo. ¡Vámonos!",
        "El creeper quería un abrazo explosivo. Lumina dice que no.",
        "¡Viaje urgente! El vecino verde no parecía amistoso.",
        "Ese creeper tenía demasiadas ganas de conocerte."
    };
    private static final String[] LAVA={
        "Temperatura preocupante. ¡Activando salida de emergencia!",
        "Hoy no es día de convertir el inventario en carbón.",
        "La lava está preciosa... vista desde MUY lejos."
    };
    private static final String[] RESCUE={
        "¡Rescate Lumina! El destino seguro te está esperando.",
        "No hay tiempo para dramatismos. ¡Portal de emergencia!",
        "¡El Atlas te cubre! Bueno... al menos lo intenta.",
        "Esa aventura se estaba poniendo intensa. ¡Retirada mágica!"
    };
    private static final String[] NETHER={
        "Próxima parada: Nether. No olvides tu protector solar.",
        "La temperatura sube y el sentido común baja. ¡Al Nether!",
        "Cuidado con los ghasts. No aceptan sugerencias de viaje."
    };
    private static final String[] END={
        "Rumbo al End. Se recomienda no mirar fijamente a los altos.",
        "El vacío llama... pero preferimos seguir en tierra firme.",
        "Si ves un dragón, salúdalo desde lejos."
    };
    private static final String[] HOME={
        "Hogar, dulce hogar. Ojalá nadie haya tocado los cofres.",
        "Volviendo a casa: el mejor bioma es donde está tu cama.",
        "El Atlas dice que ya es hora de descansar."
    };
    private static final String[] NETHER_ARRIVAL={
        "Llegamos al Nether. El termómetro renunció.",
        "¡Destino infernal alcanzado! No acaricies a los ghasts.",
        "Lumina aterriza con estilo... aunque aquí huele a azufre."
    };
    private static final String[] END_ARRIVAL={
        "Llegamos al End. Recuerda: no mires a los ojos.",
        "El vacío es bonito desde una distancia prudente.",
        "¡El Nexo ha vencido al espacio! De momento."
    };
    private static final String[] HOME_ARRIVAL={
        "Hogar, dulce hogar. ¿Siguen ahí los diamantes?",
        "¡De vuelta en casa! Por fin un lugar sin creepers... quizá.",
        "Llegamos. Si falta algo del cofre, yo no fui."
    };
    private static final String[] ARRIVAL={
        "¡Aterrizaje impecable! O casi.",
        "Portal cerrado. Ningún enderman resultó herido (que sepamos).",
        "¡Destino encontrado! Lumina merece unas vacaciones.",
        "Un segundo antes estábamos lejos. La magia tiene ventajas."
    };
    private static String choose(String category,String[] phrases){
        int n=phrases.length;
        int choice=ThreadLocalRandom.current().nextInt(n);
        Integer prior=last.get(category);
        if(prior!=null&&n>1&&choice==prior)choice=(choice+1+ThreadLocalRandom.current().nextInt(n-1))%n;
        last.put(category,choice);
        return phrases[choice];
    }
    private static boolean creeperNearby(Minecraft mc){
        if(mc.level==null||mc.player==null)return false;
        int examined=0;
        for(Entity entity:mc.level.entitiesForRendering()){
            if(examined++>120)break;
            if(entity instanceof Creeper&&entity.isAlive()){
                double x=entity.getX()-mc.player.getX(),y=entity.getY()-mc.player.getY(),z=entity.getZ()-mc.player.getZ();
                if(x*x+y*y+z*z<=100)return true;
            }
        }
        return false;
    }
    public static String starting(Minecraft mc,Waypoint destination,boolean emergency){
        if(mc.player==null)return choose("general",GENERIC);
        if(mc.player.isInLava()||mc.player.isOnFire())return choose("lava",LAVA);
        if(creeperNearby(mc))return choose("creeper",CREEPER);
        if(emergency)return choose("rescue",RESCUE);
        String target=destination.dimension().toLowerCase(Locale.ROOT);
        if(target.contains("the_nether"))return choose("nether",NETHER);
        if(target.contains("the_end"))return choose("end",END);
        if(homeName(destination.name()))return choose("home",HOME);
        return choose("general",GENERIC);
    }
    public static String arrived(String name,String dimension){
        String dim=dimension.toLowerCase(Locale.ROOT);
        if(homeName(name))return choose("home_arrival",HOME_ARRIVAL);
        if(dim.contains("the_nether"))return choose("nether_arrival",NETHER_ARRIVAL);
        if(dim.contains("the_end"))return choose("end_arrival",END_ARRIVAL);
        return "["+name+"] "+choose("arrival",ARRIVAL);
    }
    private static boolean homeName(String name){
        String value=name.toLowerCase(Locale.ROOT);
        return value.contains("casa")||value.contains("hogar")||value.contains("home");
    }
}
