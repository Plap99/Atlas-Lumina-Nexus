package com.radig.atlasluminanexus.client;

import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lumina Gate, cliente: partículas, sonido y comando vanilla autorizado.
 * Nunca mueve al jugador localmente ni concede protección de daño.
 */
public final class LuminaGateClient {
    private static final Pattern DIMENSION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final DustParticleOptions GOLD = new DustParticleOptions(0xFFE8BA64, 0.86f);
    private static final DustParticleOptions BLUE = new DustParticleOptions(0xFF65C9FF, 0.75f);
    private static final DustParticleOptions PURPLE = new DustParticleOptions(0xFFA48AFF, 0.76f);

    private static Journey journey;
    private static int arrivalTicks;
    private static int arrivalAge;
    private static UUID arrivalPlayer;
    // Confirmacion de teletransporte solicitada por ALN (cliente).
    private static int feedbackWindowTicks;
    private static double feedbackX, feedbackY, feedbackZ;

    private LuminaGateClient() { }

    private static final class Journey {
        final String command;
        final String dimension;
        final String name;
        final boolean emergency;
        final double x, y, z;
        int casting;
        int age;
        int waiting = -1;

        Journey(String command, String dimension, Waypoint waypoint, int casting, boolean emergency) {
            this.emergency = emergency;
            this.command = command;
            this.dimension = dimension;
            this.name = waypoint.name();
            this.x = center(waypoint.x());
            this.y = waypoint.y();
            this.z = center(waypoint.z());
            this.casting = casting;
        }
    }

    public static void start(Waypoint point) { start(point, false); }

    /** Solicita el viaje en este tick, sin animación. No bloquea daño ni evade permisos. */
    public static void startEmergency(Waypoint point) { start(point, true); }

    private static void start(Waypoint point, boolean emergency) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.connection == null) return;
        if (journey != null) {
            if (emergency) journey = null; // el rescate tiene prioridad sobre la animación
            else {
                info(mc, "Un portal ya se está formando.", ChatFormatting.YELLOW);
                return;
            }
        }
        String target = dimensionId(point.dimension());
        String here = dimensionId(mc.level.dimension().toString());
        if (target == null || here == null) {
            mc.gui.setScreen(null);
            info(mc, "Este destino no tiene una dimensión reconocida.", ChatFormatting.RED);
            return;
        }
        var commands = mc.player.connection.getCommands().getRoot();
        String tp = commands.getChild("teleport") != null ? "teleport" :
                    commands.getChild("tp") != null ? "tp" : null;
        if (tp == null || (!target.equals(here) && commands.getChild("execute") == null)) {
            mc.gui.setScreen(null);
            info(mc, "Este mundo no permite abrir el portal. Necesitas permisos de teletransporte.", ChatFormatting.RED);
            return;
        }
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y()) || !Double.isFinite(point.z())) {
            mc.gui.setScreen(null);
            info(mc, "Las coordenadas del waypoint no son válidas.", ChatFormatting.RED);
            return;
        }
        if (target.equals(here) && Math.abs(mc.player.getX() - center(point.x())) < 0.8
                && Math.abs(mc.player.getY() - point.y()) < 1.4
                && Math.abs(mc.player.getZ() - center(point.z())) < 0.8) {
            mc.gui.setScreen(null);
            info(mc, "¡Ya estás en ese destino! El Nexo no necesita despertarse.", ChatFormatting.GOLD);
            return;
        }

        String coordinates = String.format(Locale.ROOT, "%.3f %.3f %.3f",
                center(point.x()), point.y(), center(point.z()));
        String command = target.equals(here) ? tp + " @s " + coordinates
                : "execute in " + target + " run " + tp + " @s " + coordinates;
        int cast = emergency ? 0 : switch (LuminaGateConfig.mode()) {
            case FULL -> 60;
            case FAST -> 9;
            case INSTANT -> 0;
        };
        journey = new Journey(command, target, point, cast, emergency);
        arrivalTicks = 0;
        mc.gui.setScreen(null);
        if (LuminaGateConfig.messages()) {
            info(mc, LuminaPhrases.starting(mc,point,emergency), ChatFormatting.LIGHT_PURPLE);
        }
        if (LuminaGateConfig.sound()) {
            playMagicSound(false);
        }
        if (cast == 0) sendCommand(mc);
    }

    /** Sonido local de Lumina Gate, independiente de la distancia al jugador. */
    public static void playMagicSound(boolean arrival) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null) return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.AMETHYST_BLOCK_CHIME, arrival ? 1.48f : 1.05f, 1.0f));
        mc.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.ENCHANTMENT_TABLE_USE, arrival ? 1.3f : 0.95f, 0.55f));
    }

    /** Se llama una sola vez por tick cliente, desde el loader correspondiente. */
    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            journey = null;
            arrivalTicks = 0;
            arrivalPlayer = null;
            feedbackWindowTicks = 0;
            return;
        }
        if (feedbackWindowTicks > 0) feedbackWindowTicks--;
        if (journey != null) {
            Journey j = journey;
            if (j.casting > 0) {
                circle(mc, j.age++, LuminaGateConfig.mode() == LuminaGateConfig.Mode.FULL);
                j.casting--;
                if (j.casting == 0) sendCommand(mc);
            } else if (j.waiting >= 0) {
                j.waiting++;
                String current = dimensionId(mc.level.dimension().toString());
                boolean correctDimension = j.dimension.equals(current);
                boolean reached = correctDimension && j.waiting > 1
                        && Math.abs(mc.player.getX() - j.x) < 3.0
                        && Math.abs(mc.player.getY() - j.y) < 4.0
                        && Math.abs(mc.player.getZ() - j.z) < 3.0;
                if (reached) {
                    journey = null;
                    if (!j.emergency && LuminaGateConfig.mode() != LuminaGateConfig.Mode.INSTANT) {
                        arrivalTicks = LuminaGateConfig.mode() == LuminaGateConfig.Mode.FAST ? 8 : 28;
                        arrivalAge = 0;
                        arrivalPlayer = mc.player.getUUID();
                    }
                    if (LuminaGateConfig.sound()) {
                        playMagicSound(true);
                    }
                    if (LuminaGateConfig.messages()) {
                        info(mc, LuminaPhrases.arrived(j.name,j.dimension), ChatFormatting.GOLD);
                    }
                } else if (j.waiting > 100) {
                    journey = null;
                    info(mc, "El portal no pudo completarse. Comprueba los permisos del servidor.", ChatFormatting.RED);
                }
            }
        }
        if (arrivalTicks > 0) {
            if (arrivalPlayer == null || arrivalPlayer.equals(mc.player.getUUID())) {
                circle(mc, arrivalAge++, false);
            }
            arrivalTicks--;
        }
    }

    private static void sendCommand(Minecraft mc) {
        if (journey == null) return;
        if (mc.player == null || mc.player.connection == null) {
            journey = null;
            return;
        }
        journey.waiting = 0;
        feedbackX = journey.x;
        feedbackY = journey.y;
        feedbackZ = journey.z;
        feedbackWindowTicks = 100;
        mc.player.connection.sendCommand(journey.command);
    }

    /**
     * Filtra la confirmacion de la posicion especifica que ALN acaba de solicitar.
     * No deshabilita comentarios ni mensajes ordinarios del servidor.
     */
    public static boolean shouldHideVanillaTeleportMessage(Component message) {
        if (LuminaGateConfig.showVanillaFeedback() || feedbackWindowTicks <= 0 || message == null) {
            return false;
        }
        if (message.getContents() instanceof TranslatableContents contents
                && "commands.teleport.success.location.single".equals(contents.getKey())) {
            Object[] args = contents.getArgs();
            if (args.length < 4) return false;
            try {
                double x = messageNumber(args[1]);
                double y = messageNumber(args[2]);
                double z = messageNumber(args[3]);
                boolean ownTrip = Math.abs(x - feedbackX) < 0.06
                        && Math.abs(y - feedbackY) < 0.06
                        && Math.abs(z - feedbackZ) < 0.06;
                if (ownTrip) feedbackWindowTicks = 0;
                return ownTrip;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return false;
    }

    private static double messageNumber(Object value) {
        String number = value instanceof Component component ? component.getString() : String.valueOf(value);
        return Double.parseDouble(number);
    }

    /**
     * Sello de particulas de geometria fija: aros, estrella, glifos y halo.
     */
    private static void circle(Minecraft mc, int t, boolean full) {
        // Dibujamos el sello varias veces mientras dura la invocación; los
        // DustParticleOptions anteriores permanecen visibles entre fotogramas.
        int interval = switch (LuminaGateConfig.density()) {
            case LOW -> 5;
            case NORMAL -> 3;
            case HIGH -> 2;
        };
        double px = mc.player.getX(), py = mc.player.getY() + 0.045, pz = mc.player.getZ();
        // El trazo se revela progresivamente; la llegada aparece casi completa.
        boolean quick = !full || LuminaGateConfig.mode() == LuminaGateConfig.Mode.FAST;
        double reveal = Math.min(1.0, (t + 1) / (quick ? 4.0 : 13.0));
        double rotation = t * 0.010;
        double scale = 1.0 + 0.012 * Math.sin(t * 0.22);
        if (t % interval == 0) {
            double spacing = switch (LuminaGateConfig.density()) {
                case LOW -> 0.145;
                case NORMAL -> 0.095;
                case HIGH -> 0.068;
            };
            double outer = 1.64 * scale;
            drawArc(mc, px, py, pz, outer, 0, 2 * Math.PI * reveal, GOLD, spacing);
            drawArc(mc, px, py, pz, 1.24 * scale, 0, 2 * Math.PI * reveal, GOLD, spacing);
            if (reveal > 0.32) {
                // Rosa de los vientos de 8 puntas (motivo original de ALN).
                drawCompassStar(mc, px, py, pz, scale, Math.min(1, (reveal - .32) / .55), spacing);
            }
            if (reveal > 0.64) {
                for (int i = 0; i < 8; i++) {
                    drawGlyph(mc, px, py, pz, (i * 2 * Math.PI / 8) + rotation * .15,
                            scale, spacing * 1.05);
                }
            }
            // Un anillo interior azul separa el núcleo de la rosa.
            if (reveal > 0.5) drawArc(mc, px, py + .006, pz, .40 * scale,
                    0, 2 * Math.PI * reveal, BLUE, spacing);
        }
        // Luz aparente: pequeñas chispas brillantes ascienden por el borde.
        // Esto no altera la iluminación real de los bloques.
        if (t % 3 == 0) {
            for (int i = 0; i < (full ? 3 : 1); i++) {
                double a = t * .21 + i * 2.399;
                double r = 1.45;
                mc.level.addParticle(ParticleTypes.END_ROD,
                        px + Math.cos(a) * r, py + .055, pz + Math.sin(a) * r,
                        0, .012, 0);
            }
        }
        // Columna sutil ascendente, sin ocultar al personaje ni saturar la vista.
        if (full && t % 2 == 0) {
            double a = t * .3;
            double h = .30 + (t % 18) * .10;
            mc.level.addParticle(ParticleTypes.END_ROD,
                    px + Math.cos(a) * .47, py + h, pz + Math.sin(a) * .47,
                    0, .015, 0);
        }
        if (t % 12 == 0) {
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    px, py + .08, pz, 0, .015, 0);
        }
    }

    private static void drawArc(Minecraft mc, double x, double y, double z,
                                double r, double start, double end,
                                DustParticleOptions color, double spacing) {
        int count = Math.max(1, (int) Math.ceil(Math.abs(end - start) * r / spacing));
        for (int i = 0; i <= count; i++) {
            double a = start + (end - start) * i / count;
            emit(mc, x + r * Math.cos(a), y, z + r * Math.sin(a), color);
        }
    }

    private static void drawLine(Minecraft mc, double x1, double y, double z1,
                                 double x2, double z2, DustParticleOptions color, double spacing) {
        double length = Math.hypot(x2 - x1, z2 - z1);
        int count = Math.max(1, (int) Math.ceil(length / spacing));
        for (int i = 0; i <= count; i++) {
            double u = (double) i / count;
            emit(mc, x1 + (x2 - x1) * u, y, z1 + (z2 - z1) * u, color);
        }
    }

    private static void drawCompassStar(Minecraft mc, double x, double y, double z,
                                        double scale, double progress, double spacing) {
        // 16 segmentos: 8 puntas largas alternadas con 8 espacios internos.
        int n = Math.max(1, (int) Math.ceil(16 * progress));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * i / 8.0 - Math.PI / 2.0;
            double b = Math.PI * (i + 1) / 8.0 - Math.PI / 2.0;
            double r1 = (i % 2 == 0 ? 1.08 : .57) * scale;
            double r2 = ((i + 1) % 2 == 0 ? 1.08 : .57) * scale;
            drawLine(mc, x + Math.cos(a) * r1, y + .007, z + Math.sin(a) * r1,
                    x + Math.cos(b) * r2, z + Math.sin(b) * r2, BLUE, spacing);
        }
        // Agujas de brújula hacia N, S, E y O.
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2.0;
            drawLine(mc, x, y + .013, z,
                    x + Math.cos(a) * .95 * scale, z + Math.sin(a) * .95 * scale,
                    GOLD, spacing);
        }
    }

    private static void drawGlyph(Minecraft mc, double x, double y, double z,
                                  double angle, double scale, double spacing) {
        // Glifo propio: pequeño rombo rúnico con una marca radial de orientación.
        double ca = Math.cos(angle), sa = Math.sin(angle);
        double cx = x + ca * 1.43 * scale, cz = z + sa * 1.43 * scale;
        double radial = .087 * scale, tangent = .061 * scale;
        double upX = ca * radial, upZ = sa * radial;
        double acrossX = -sa * tangent, acrossZ = ca * tangent;
        for (int j = 0; j < 4; j++) {
            double ax = switch (j) {case 0 -> cx + upX; case 1 -> cx + acrossX;
                case 2 -> cx - upX; default -> cx - acrossX;};
            double az = switch (j) {case 0 -> cz + upZ; case 1 -> cz + acrossZ;
                case 2 -> cz - upZ; default -> cz - acrossZ;};
            double bx = switch (j) {case 0 -> cx + acrossX; case 1 -> cx - upX;
                case 2 -> cx - acrossX; default -> cx + upX;};
            double bz = switch (j) {case 0 -> cz + acrossZ; case 1 -> cz - upZ;
                case 2 -> cz - acrossZ; default -> cz + upZ;};
            drawLine(mc, ax, y + .011, az, bx, bz, j % 2 == 0 ? PURPLE : GOLD, spacing);
        }
    }

    private static void emit(Minecraft mc, double x, double y, double z, DustParticleOptions color) {
        mc.level.addParticle(color, x, y, z, 0, 0.0001, 0);
    }

    private static void info(Minecraft mc, String message, ChatFormatting color) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("✦ [Lumina] " + message).withStyle(color));
        }
    }

    private static double center(double value) {
        return Math.abs(value - Math.rint(value)) < 0.00001 ? value + 0.5 : value;
    }

    /** Reconoce los ResourceKey serializados usados por el sistema de waypoints de 26.3. */
    private static String dimensionId(String value) {
        if (value == null) return null;
        Matcher m = DIMENSION.matcher(value.toLowerCase(Locale.ROOT));
        String id = null;
        while (m.find()) id = m.group();
        if (id != null && !id.equals("minecraft:dimension")) return id;
        return switch (value.toLowerCase(Locale.ROOT).trim()) {
            case "overworld" -> "minecraft:overworld";
            case "nether" -> "minecraft:the_nether";
            case "end" -> "minecraft:the_end";
            default -> null;
        };
    }
}
