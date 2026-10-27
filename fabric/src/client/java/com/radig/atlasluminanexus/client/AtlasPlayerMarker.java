package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;

/** Marcador personal independiente de las criaturas; colores y equipo configurables. */
public final class AtlasPlayerMarker {
    public static final int[] COLORS = {0xFFFFD46A,0xFF73D9FF,0xFFB58AFF,0xFF70E4A4,0xFFFF7777,0xFFFFFFFF};
    private AtlasPlayerMarker() {}
    public static void draw(GuiGraphicsExtractor g,Minecraft mc,int x,int y,double yaw,
                            boolean mapRotates,int style,int size){
        int s=Math.max(1,Math.min(28,size));
        int color=COLORS[Math.max(0,Math.min(COLORS.length-1,AtlasMinimapConfig.playerMarkerColor))];
        switch(style){
            case 0 -> {
                if(AtlasMinimapConfig.playerShowEquipment)
                    AtlasPlayerEquipped.draw(g,mc,x,y,Math.max(10,s),yaw,mapRotates);
                else AtlasPlayerAvatar.draw(g,mc,x,y,Math.max(10,s),yaw,mapRotates);
            }
            case 1 -> {
                // Con casco, recortar la cabeza del modelo 3D REAL;
                // sin equipo, dibujar la skin nítida de siempre.
                boolean equipped = AtlasMinimapConfig.playerShowEquipment && mc.player != null
                        && !mc.player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
                if (equipped) AtlasPlayerEquipped.drawHead(g, mc, x, y, Math.max(8, s));
                else AtlasPlayerPortrait.draw(g, mc, x, y, Math.max(8, s), yaw, !mapRotates);
            }
            case 2 -> arrow(g,x,y,yaw,mapRotates,Math.max(3,s),color);
            case 3 -> dot(g,x,y,s,color);
            default -> arrow(g,x,y,yaw,mapRotates,Math.max(3,s),color);
        }
    }
    private static void dot(GuiGraphicsExtractor g,int x,int y,int size,int color){
        if(size<=1){g.fill(x,y,x+1,y+1,color);return;}
        int radius=Math.max(1,size/2);int r2=radius*radius;
        for(int dy=-radius;dy<=radius;dy++)for(int dx=-radius;dx<=radius;dx++){
            int distance=dx*dx+dy*dy;
            if(distance<=r2)g.fill(x+dx,y+dy,x+dx+1,y+dy+1,
                distance>=Math.max(1,(radius-1)*(radius-1))?0xDD152438:color);
        }
    }
    private static void arrow(GuiGraphicsExtractor g,int x,int y,double yaw,boolean rotating,int size,int color){
        double dx=rotating?0:-Math.sin(yaw),dy=rotating?-1:Math.cos(yaw);
        double px=-dy,py=dx,span=Math.max(2,size*.56),wing=Math.max(1.3,size*.42);
        double ax=x+dx*span,ay=y+dy*span;
        double bx=x-dx*span*.65+px*wing,by=y-dy*span*.65+py*wing;
        double cx=x-dx*span*.65-px*wing,cy=y-dy*span*.65-py*wing;
        double den=((by-cy)*(ax-cx)+(cx-bx)*(ay-cy));if(Math.abs(den)<.0001)return;
        int r=(int)Math.ceil(span+wing)+1;
        for(int oy=-r;oy<=r;oy++)for(int ox=-r;ox<=r;ox++){
            double xx=x+ox+.5,yy=y+oy+.5;
            double a=((by-cy)*(xx-cx)+(cx-bx)*(yy-cy))/den;
            double b=((cy-ay)*(xx-cx)+(ax-cx)*(yy-cy))/den;
            if(a>=0&&b>=0&&a+b<=1)g.fill(x+ox,y+oy,x+ox+1,y+oy+1,color);
        }
    }
}
