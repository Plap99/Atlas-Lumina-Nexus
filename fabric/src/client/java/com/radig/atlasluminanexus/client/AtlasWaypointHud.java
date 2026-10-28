package com.radig.atlasluminanexus.client;

import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** HUD de waypoints anclados a posiciones XYZ, proyectados con la camara real 26.2. */
public final class AtlasWaypointHud {
    private AtlasWaypointHud() {}
    private record Target(Waypoint waypoint,double distance) {}

    private static void label(GuiGraphicsExtractor g,Minecraft mc,String text,int x,int y,int color){
        float scale=AtlasDisplayConfig.textScale();
        g.pose().pushMatrix();
        g.pose().translate((float)x,(float)y);
        g.pose().scale(scale,scale);
        g.centeredText(mc.font,text,0,0,color);
        g.pose().popMatrix();
    }
    public static void draw(GuiGraphicsExtractor g){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.gui.screen()!=null||mc.gui.hud.isHidden())return;
        AtlasMinimapHud.draw(g);
        String dim=mc.level.dimension().toString();
        Vec3 camera=mc.gameRenderer.mainCamera().position();
        // Camera forward, including pitch: rejects markers behind the player's view.
        Vector3fc forward=mc.gameRenderer.mainCamera().forwardVector();
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        if(w<100||h<100)return;
        List<Target> visible=new ArrayList<>();
        for(Waypoint wp:AtlasWaypointClient.all(mc)){
            if(!wp.visible()||!dim.equals(wp.dimension()))continue;
            double dx=wp.x()+.5-camera.x,dy=wp.y()+1.05-camera.y,dz=wp.z()+.5-camera.z;
            double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(distance>2.2)visible.add(new Target(wp,distance));
        }
        visible.sort(Comparator.comparingDouble(Target::distance));
        int shown=0;
        for(Target target:visible){
            if(shown>=18)break;
            Waypoint wp=target.waypoint();
            // The projection can yield plausible screen coordinates for points *behind*
            // the eye (perspective division). Reject these before projecting.
            Vec3 anchor=new Vec3(wp.x()+.5,wp.y()+1.05,wp.z()+.5);
            Vec3 offset=anchor.subtract(camera);
            double depth=offset.x*forward.x()+offset.y*forward.y()+offset.z*forward.z();
            if(depth<=0.10)continue;
            // This is the same fixed world-space anchor used by the camera projection.
            Vec3 ndc=mc.gameRenderer.projectPointToScreen(anchor);
            if(!Double.isFinite(ndc.x)||!Double.isFinite(ndc.y)||!Double.isFinite(ndc.z)
                    ||ndc.z < -1.0||ndc.z > 1.0||Math.abs(ndc.x)>0.97||Math.abs(ndc.y)>0.94)continue;
            int sx=(int)Math.round((ndc.x*.5+.5)*w);
            int sy=(int)Math.round((1-(ndc.y*.5+.5))*h);
            if(AtlasMinimapHud.overlapsMinimap(mc,sx,sy,10))continue;
            int size=AtlasDisplayConfig.iconSize();
            Identifier icon=AtlasWaypointIconTextures.of(AtlasWaypointIcons.get(mc,wp.id()),wp.argb());
            int lo=size/2,hi=size-lo;
            g.blit(icon,sx-lo,sy-lo,sx+hi,sy+hi,0f,1f,0f,1f);
            label(g,mc,Math.round(target.distance())+" m",sx,sy+hi+3,0xFFEAE2D5);
            if(Math.abs(sx-w/2)<Math.max(8,size/2+4)&&Math.abs(sy-h/2)<Math.max(9,size/2+4)){
                label(g,mc,wp.name(),sx,sy+hi+15,0xFFFFE2A5);
            }
            shown++;
        }
    }
}
