package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;

/** Mini figura 2D de la skin local. Animación por poses, sin render 3D ni texturas nuevas.
 *  Las coordenadas UV son las oficiales de la skin 64x64; soporta segunda capa.
 */
public final class AtlasPlayerAvatar {
    private AtlasPlayerAvatar() {}

    private static void piece(GuiGraphicsExtractor g, Identifier skin,
                              int x,int y,int width,int height,
                              int u,int v,int textureWidth,int textureHeight) {
        if(width<=0||height<=0)return;
        g.blit(skin,x,y,x+width,y+height,
                u/64f,(u+textureWidth)/64f,v/64f,(v+textureHeight)/64f);
    }

    /** Dibuja el cuerpo en coordenadas locales 12x20, centrado en el jugador. */
    public static void draw(GuiGraphicsExtractor g, Minecraft mc, int x, int y,
                            int preferredSize, double yawRadians, boolean mapRotates) {
        if(mc.player==null)return;
        int size=Math.max(4,Math.min(28,preferredSize));
        float k=size/20f;
        g.pose().pushMatrix();
        g.pose().translate(x,y);
        g.pose().scale(k,k);
        AbstractClientPlayer client=mc.player;
        Identifier skin=client.getSkin().body().texturePath();
        // En el mapa fijo se distinguen frente, espalda y costados.
        // En el mapa giratorio la figura mira hacia arriba, como el jugador.
        int face=mapRotates?0:Math.floorMod((int)Math.round(Math.toDegrees(yawRadians)/90.0),4);
        int headU=switch(face){case 1->0;case 2->24;case 3->16;default->8;};
        int bodyU=switch(face){case 1->16;case 2->32;case 3->28;default->20;};
        int armU=face==2?52:44;
        int legU=face==2?12:4;
        boolean swimming=mc.player.isSwimming();
        boolean crouching=mc.player.isCrouching()&&!swimming;
        boolean airborne=!mc.player.onGround()&&!swimming;
        double vx=mc.player.getDeltaMovement().x;
        double vz=mc.player.getDeltaMovement().z;
        boolean walking=vx*vx+vz*vz>0.0004&&!swimming&&!crouching&& !airborne;
        int stride=walking&&((mc.player.tickCount/4)%2==0)?1:walking?-1:0;
        if(swimming){
            // Pose horizontal de nado: brazos adelante, piernas detrás.
            piece(g,skin,-10,-4,5,5,headU,8,8,8);
            piece(g,skin,-10,-4,5,5,headU+32,8,8,8);
            piece(g,skin,-5,-3,8,5,bodyU,20,8,12);
            piece(g,skin,3,-3,5,2,legU,20,4,12);
            piece(g,skin,3,0,6,2,20,52,4,12);
            piece(g,skin,-7,-7,8,2,armU,20,4,12);
            piece(g,skin,-7,3,8,2,36,52,4,12);
        }else{
            int lift=airborne?2:0;
            int headY=-10+lift+(crouching?3:0);
            int bodyY=-3+lift+(crouching?2:0);
            int legY=3+lift+(crouching?1:0);
            // El vaivén es de un píxel: expresivo sin tapar el minimapa.
            piece(g,skin,-3+stride,legY,3,6,legU,20,4,12);
            piece(g,skin,0-stride,legY,3,6,20,52,4,12);
            piece(g,skin,-3+stride,legY,3,6,legU,36,4,12);
            piece(g,skin,0-stride,legY,3,6,4,52,4,12);
            piece(g,skin,-3,bodyY,6,crouching?5:6,bodyU,20,8,12);
            piece(g,skin,-3,bodyY,6,crouching?5:6,bodyU,36,8,12);
            int armY=bodyY+(airborne?-3:0);
            piece(g,skin,-5,armY+stride,2,6,armU,20,4,12);
            piece(g,skin,3,armY-stride,2,6,36,52,4,12);
            piece(g,skin,-5,armY+stride,2,6,armU,36,4,12);
            piece(g,skin,3,armY-stride,2,6,52,52,4,12);
            piece(g,skin,-4,headY,8,7,headU,8,8,8);
            piece(g,skin,-4,headY,8,7,headU+32,8,8,8);
        }
        g.pose().popMatrix();
    }

    private static void fallback(GuiGraphicsExtractor g){
        g.fill(-3,-9,4,-3,0xFFF1C6A8);
        g.fill(-3,-2,4,4,0xFF6FC9DC);
        g.fill(-3,4,-1,9,0xFF4E476E);
        g.fill(1,4,3,9,0xFF4E476E);
    }
}
