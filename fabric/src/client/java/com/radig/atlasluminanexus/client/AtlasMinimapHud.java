package com.radig.atlasluminanexus.client;

import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.Villager;

/** Vista cenital configurable: superficie real, relieve sutil y marcadores independientes. */
public final class AtlasMinimapHud {
    private static final int GOLD=0xFFE5C788, BG=0xFF121B29, TEXT=0xFFE8DFCD;
    private static final Identifier ROUND_FRAME=Identifier.fromNamespaceAndPath("atlasluminanexus","textures/gui/minimap/lumina_round.png");
    private static final Identifier SQUARE_FRAME=Identifier.fromNamespaceAndPath("atlasluminanexus","textures/gui/minimap/lumina_square.png");
    private static final int[] RADII={32,40,48,56,64};
    private static final double[] PIXELS_PER_CELL={0.72,1.0,1.4,1.85,2.45};
    private AtlasMinimapHud() {}

    public static int radius(Minecraft mc){
        AtlasMinimapConfig.load();
        int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();
        return Math.max(0,Math.min(RADII[AtlasMinimapConfig.size],Math.min((w-22)/4,(h-34)/3)));
    }
    public static boolean overlapsMinimap(Minecraft mc,int x,int y,int padding){
        if(!AtlasMinimapConfig.enabled)return false;
        int r=radius(mc),side=r*2;
        if(r<24)return false;
        int x0=AtlasMinimapConfig.left?7:mc.getWindow().getGuiScaledWidth()-side-7;
        int extra=AtlasMinimapConfig.showCoordinates?20:0;
        return x>=x0-padding && x<=x0+side+padding && y>=8-padding && y<=8+side+extra+padding;
    }
    private static boolean inside(double x,double y,int radius,boolean round){
        return round?x*x+y*y<=radius*radius:Math.abs(x)<=radius&&Math.abs(y)<=radius;
    }
    private static double[] orient(double dx,double dz,double yaw,boolean rotate){
        if(!rotate)return new double[]{dx,dz};
        double co=Math.cos(yaw),si=Math.sin(yaw);
        return new double[]{-co*dx-si*dz,si*dx-co*dz};
    }
    private static int clamp(int n){return Math.max(0,Math.min(255,n));}
    private static int blend(int fg,int bg,int opacity){
        int a=Math.max(0,Math.min(100,opacity));
        int r=(((fg>>16)&255)*a+((bg>>16)&255)*(100-a))/100;
        int g=(((fg>>8)&255)*a+((bg>>8)&255)*(100-a))/100;
        int b=((fg&255)*a+(bg&255)*(100-a))/100;
        return 0xFF000000|(r<<16)|(g<<8)|b;
    }
    private static void tinyMarker(GuiGraphicsExtractor g,int x,int y,int color,int style){
        if(style==1){ // amigo o equipo: rombo
            g.fill(x,y-3,x+1,y+4,color);
            g.fill(x-2,y-1,x+3,y+2,color);
        }else if(style==2){ // otro jugador: cuadrito con borde
            g.fill(x-2,y-2,x+3,y+3,0xDD0B1825);
            g.outline(x-2,y-2,5,5,color);
        }else if(style==3){ // hostil: cruz
            g.fill(x-2,y,x+3,y+1,color);g.fill(x,y-2,x+1,y+3,color);
        }else { // pasivos
            g.fill(x-1,y-1,x+2,y+2,color);
        }
    }
    private static boolean triangle(double x,double y,double ax,double ay,double bx,double by,double cx,double cy){
        double d1=(x-bx)*(ay-by)-(ax-bx)*(y-by);
        double d2=(x-cx)*(by-cy)-(bx-cx)*(y-cy);
        double d3=(x-ax)*(cy-ay)-(cx-ax)*(y-ay);
        return (d1>=0&&d2>=0&&d3>=0)||(d1<=0&&d2<=0&&d3<=0);
    }
    /** Flecha escalable del jugador, ahora independiente del tamaño de entidades. */
    private static void arrowShape(GuiGraphicsExtractor g,int x,int y,double dx,double dy,
                                   int color,double length,double wing){
        double px=-dy,py=dx;
        double ax=x+dx*length,ay=y+dy*length;
        double bx=x-dx*length*.63+px*wing,by=y-dy*length*.63+py*wing;
        double cx=x-dx*length*.63-px*wing,cy=y-dy*length*.63-py*wing;
        int extent=(int)Math.ceil(length+wing+2);
        for(int oy=-extent;oy<=extent;oy++)for(int ox=-extent;ox<=extent;ox++){
            if(triangle(x+ox+.5,y+oy+.5,ax,ay,bx,by,cx,cy))
                g.fill(x+ox,y+oy,x+ox+1,y+oy+1,color);
        }
    }
    static void playerArrow(GuiGraphicsExtractor g,int x,int y,double yaw,boolean rotate,int size){
        int selected=Math.max(4,Math.min(28,size));
        double dx=rotate?0:-Math.sin(yaw),dy=rotate?-1:Math.cos(yaw);
        double half=selected*.5;
        arrowShape(g,x,y,dx,dy,0xEE0D1A2B,half+.8,half*.71);
        arrowShape(g,x,y,dx,dy,0xFF65D8F4,half,half*.48);
        g.fill(x,y,x+1,y+1,0xFFFFFFFF);
    }
    private static void put(GuiGraphicsExtractor g,int x,int y,int c){g.fill(x,y,x+1,y+1,c);}
    private static void circlePixels(GuiGraphicsExtractor g,int cx,int cy,int radius,int color){
        int x=radius,y=0,error=1-radius;
        while(x>=y){
            put(g,cx+x,cy+y,color);put(g,cx+y,cy+x,color);
            put(g,cx-y,cy+x,color);put(g,cx-x,cy+y,color);
            put(g,cx-x,cy-y,color);put(g,cx-y,cy-x,color);
            put(g,cx+y,cy-x,color);put(g,cx+x,cy-y,color);
            y++;
            if(error<0)error+=2*y+1;
            else{x--;error+=2*(y-x)+1;}
        }
    }
    /** Borde transparente, sin cuadricula oscura ni textura que interrumpa el terreno. */
    private static void ring(GuiGraphicsExtractor g,int cx,int cy,int r,boolean round){
        if(round){
            circlePixels(g,cx,cy,r+2,0xA0584C3D);
            circlePixels(g,cx,cy,r+1,0xFFE4C781);
            circlePixels(g,cx,cy,r,0xFFFAE9B4);
        }else{
            g.outline(cx-r-2,cy-r-2,2*r+4,2*r+4,0xAA594B36);
            g.outline(cx-r-1,cy-r-1,2*r+2,2*r+2,0xFFE4C781);
            g.outline(cx-r,cy-r,2*r,2*r,0xFFF7E5AE);
        }
    }
    private static void coordinates(GuiGraphicsExtractor g,Minecraft mc,int x0,int y0,int side){
        if(!AtlasMinimapConfig.showCoordinates)return;
        String values=(int)Math.floor(mc.player.getX())+", "+(int)Math.floor(mc.player.getY())+", "+(int)Math.floor(mc.player.getZ());
        int tw=mc.font.width(values);
        float scale=Math.min(0.43f,(side-12)/(float)Math.max(1,tw));
        int py=y0+side+4,shownW=(int)Math.ceil(scale*tw),cx=x0+side/2;
        g.pose().pushMatrix();
        g.pose().translate((float)cx,(float)py);
        g.pose().scale(scale,scale);
        g.centeredText(mc.font,values,0,0,0xFFF2DEAF);
        g.pose().popMatrix();
    }
    private static void compass(GuiGraphicsExtractor g,int cx,int cy,int r,double yaw,boolean rotate){
        double[] north=orient(0,-1,yaw,rotate);
        int nx=cx+(int)Math.round(north[0]*(r+7)),ny=cy+(int)Math.round(north[1]*(r+7));
        // Aguja orientada al centro; gira de verdad con el norte del minimapa.
        double vx=-north[0],vy=-north[1];
        for(int py=-5;py<=5;py++)for(int px=-5;px<=5;px++){
            double ax=vx*4,ay=vy*4,bx=-vx*3-vy*2.5,by=-vy*3+vx*2.5;
            double dx=-vx*3+vy*2.5,dy=-vy*3-vx*2.5;
            if(triangle(px+.5,py+.5,ax,ay,bx,by,dx,dy))
                g.fill(nx+px,ny+py,nx+px+1,ny+py+1,0xFFE8C782);
        }
        g.fill(nx,ny,nx+1,ny+1,0xFFF8F4EA);
    }
    static Identifier entityIcon(Entity e,boolean friend){
        return AtlasEntityCatalog.icon(e,friend);
    }
    public static void draw(GuiGraphicsExtractor g){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.gui.screen()!=null||mc.gui.hud.isHidden())return;
        AtlasMinimapConfig.load();
        if(!AtlasMinimapConfig.enabled)return;
        AtlasMinimapStore.sync(mc);
        int r=radius(mc);
        if(r<24)return;
        int side=r*2,w=mc.getWindow().getGuiScaledWidth();
        int x0=AtlasMinimapConfig.left?7:w-side-7,y0=8,cx=x0+r,cy=y0+r;
        boolean round=AtlasMinimapConfig.round,rotate=AtlasMinimapConfig.rotate;
        double yaw=Math.toRadians(mc.player.getYRot());
        double co=Math.cos(yaw),si=Math.sin(yaw);
        double px=mc.player.getX(),pz=mc.player.getZ();
        double worldBlocksPerPixel=2.0/PIXELS_PER_CELL[AtlasMinimapConfig.zoom];
        boolean high=AtlasMinimapConfig.highDetail,smooth=AtlasMinimapConfig.smoothTerrain;
        int alpha=(int)Math.round(255.0*AtlasMinimapConfig.opacity/100.0);
        // Terrain samples cached across frames. Color data still comes from visited client terrain.
        int[] raster=AtlasMinimapRasterCache.snapshot(mc,r,px,pz,yaw,rotate,
                AtlasMinimapConfig.zoom,high,smooth,AtlasMinimapConfig.showUnknownGrid,round);
        int stride=2*r;
        for(int sy=0;sy<stride;sy++){
            int run=0,from=0;
            for(int sx=0;sx<=stride;sx++){
                int c=sx==stride?0:raster[sy*stride+sx];
                int next=c==0?0:(alpha<<24)|(c&0xFFFFFF);
                if(next!=run){
                    if(run!=0)g.fill(x0+from,y0+sy,x0+sx,y0+sy+1,run);
                    from=sx;run=next;
                }
            }
        }
        // Waypoints y entidades usan el mismo transformado que el mapa.
        final double scale=PIXELS_PER_CELL[AtlasMinimapConfig.zoom]/2.0;
        if(AtlasMinimapConfig.waypoints){
            int shown=0;
            String dimension=mc.level.dimension().toString();
            for(Waypoint wp:AtlasWaypointClient.all(mc)){
                if(shown>=48)break;
                if(!wp.visible()||!dimension.equals(wp.dimension()))continue;
                double[] v=orient(wp.x()+.5-px,wp.z()+.5-pz,yaw,rotate);
                double sx=v[0]*scale,sy=v[1]*scale;
                if(!inside(sx,sy,r-9,round))continue;
                int mx=cx+(int)Math.round(sx),my=cy+(int)Math.round(sy);
                Identifier icon=AtlasWaypointIconTextures.of(AtlasWaypointIcons.get(mc,wp.id()),wp.argb());
                g.blit(icon,mx-3,my-3,mx+3,my+3,0,1,0,1);
                if(AtlasMinimapConfig.waypointLabels){
                    String title=wp.name();
                    if(title.length()>12)title=title.substring(0,11)+"…";
                    int textWidth=mc.font.width(title);
                    int labelX=mx+5;
                    if(labelX+textWidth>x0+side-3)labelX=mx-5-textWidth;
                    if(labelX>=x0+3 && labelX+textWidth<=x0+side-3)
                        g.text(mc.font,title,labelX,my-4,0xFFFFE2AB,false);
                }
                shown++;
            }
        }
        int mobCount=0,playerCount=0;
        for(Entity e:mc.level.entitiesForRendering()){
            if(e==mc.player||!e.isAlive())continue;
            boolean player=e instanceof Player,monster=AtlasEntityCatalog.isHostile(e);
            boolean animal=AtlasEntityCatalog.isPassive(e),villager=AtlasEntityCatalog.isVillager(e);
            if(player){if(!AtlasMinimapConfig.players||playerCount++>=40)continue;}
            else{
                if(mobCount++>=65)continue;
                if(monster&&!AtlasMinimapConfig.mobs)continue;
                if(animal&&!AtlasMinimapConfig.animals)continue;
                if(villager&&!AtlasMinimapConfig.villagers)continue;
                if(!monster&&!animal&&!villager)continue;
            }
            double[] v=orient(e.getX()-px,e.getZ()-pz,yaw,rotate);
            double sx=v[0]*scale,sy=v[1]*scale;
            if(!inside(sx,sy,r-9,round))continue;
            int mx=cx+(int)Math.round(sx),my=cy+(int)Math.round(sy);
            String name=e.getName().getString();
            boolean team=player&&mc.player.getTeam()!=null&&mc.player.getTeam()==e.getTeam();
            boolean friend=player&&AtlasMinimapConfig.friends&&(AtlasMinimapConfig.isFriend(name)||team);
            Identifier entity=entityIcon(e,friend);
            int iconSize=AtlasMinimapConfig.entityIconSize;
            int low=iconSize/2,iconHi=iconSize-low;
            // Los amigos usan sus skins cargadas; si falta la skin, usar icono ALN.
            boolean skin=player&&friend&&AtlasPlayerPortrait.drawPlayer(g,(Player)e,mx,my,iconSize);
            if(!skin)g.blit(entity,mx-low,my-low,mx+iconHi,my+iconHi,0f,1f,0f,1f);
            if(friend&&AtlasMinimapConfig.showFriendNames){
                String show=name.length()>9?name.substring(0,8)+"…":name;
                int tw=mc.font.width(show),lx=mx+7;
                if(lx+tw>x0+side-4)lx=mx-6-tw;
                if(lx>=x0+4&&lx+tw<x0+side-4)
                    g.text(mc.font,show,lx,my-5,0xFFE0FFF7,false);
            }
        }
        // Marcador personal: estilo y escala independientes de mobs y amigos.
        AtlasPlayerMarker.draw(g,mc,cx,cy,yaw,rotate,
                AtlasMinimapConfig.playerMarkerStyle,AtlasMinimapConfig.playerMarkerSize);
        ring(g,cx,cy,r,round);
        if(AtlasMinimapConfig.showNorth)compass(g,cx,cy,r,yaw,rotate);
        if(AtlasMinimapConfig.showBrand){
            int labelX=AtlasMinimapConfig.left?x0+4:x0+side-23;
            g.text(mc.font,"ALN",labelX,y0+side+(AtlasMinimapConfig.showCoordinates?20:4),TEXT,false);
        }
        coordinates(g,mc,x0,y0,side);
    }
}
