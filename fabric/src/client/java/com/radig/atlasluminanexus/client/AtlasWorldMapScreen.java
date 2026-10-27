package com.radig.atlasluminanexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;

/** Mapa mundial de ALN con textura GPU y desplazamiento continuo. */
public final class AtlasWorldMapScreen extends Screen {
    private static final int GOLD=0xFFE8C782, MUTED=0xFFB6B8C2, BACK=0xF20B1424;
    private static final Identifier CREEPER_ON=Identifier.fromNamespaceAndPath("atlasluminanexus","textures/gui/minimap/entities/creeper.png");
    private static final Identifier CREEPER_OFF=Identifier.fromNamespaceAndPath("atlasluminanexus","textures/gui/minimap/entities/creeper_off.png");
    private static final int CELL=2;
    // 31 fine zoom levels, from 16 to 0.5 blocks per raster cell.
    private static final int MAX_ZOOM=30;
    private static double blocksAt(int n){return 16.0*Math.pow(1.0/32.0, n/(double)MAX_ZOOM);}
    private double centerX,centerZ;
    private boolean initialized,dragging;
    private int zoom=18;
    private boolean legendOpen;
    private int legendPage;
    private int x,y,w,h;
    private Button entityButton,legendButton;
    private int entityButtonX,legendButtonX,recenterButtonX;
    private final AtlasWorldMapTexture terrainTexture=new AtlasWorldMapTexture();

    public AtlasWorldMapScreen(){super(Component.literal("Atlas Lumina Nexus - Mapa"));}
    private double blockPerCell(){return blocksAt(zoom);}
    private void invalidate(){terrainTexture.invalidate();}
    private void recenter(){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=null){centerX=mc.player.getX();centerZ=mc.player.getZ();initialized=true;invalidate();}
    }
    @Override protected void init(){
        AtlasMinimapConfig.load();
        if(!initialized)recenter();
        x=12;y=37;w=Math.max(120,width-24);h=Math.max(80,height-53);
        int by=9,button=22,gap=6;
        int right=width-12;
        recenterButtonX=right-button;
        legendButtonX=recenterButtonX-gap-button;
        entityButtonX=legendButtonX-gap-button;
        int sliderX=entityButtonX-gap-133;
        addRenderableWidget(new AtlasRangeSlider(sliderX,by,133,button,
                "Entidades",3,10,1,AtlasMinimapConfig.entityIconSize," px",n->{
                    AtlasMinimapConfig.entityIconSize=n;AtlasMinimapConfig.save();
                }));
        entityButton=addRenderableWidget(Button.builder(Component.empty(),b->{
            AtlasMinimapConfig.worldMapEntities=!AtlasMinimapConfig.worldMapEntities;
            AtlasMinimapConfig.save();
            entityButton.setTooltip(Tooltip.create(Component.literal(
                    AtlasMinimapConfig.worldMapEntities?"Ocultar entidades":"Mostrar entidades")));
        }).bounds(entityButtonX,by,button,button).build());
        entityButton.setTooltip(Tooltip.create(Component.literal("Mostrar u ocultar entidades")));
        legendButton=addRenderableWidget(Button.builder(Component.literal("?"),b->{
            legendOpen=!legendOpen;
            legendPage=0;
        }).bounds(legendButtonX,by,button,button).build());
        legendButton.setTooltip(Tooltip.create(Component.literal("Catálogo de entidades")));
        Button recenterButton=addRenderableWidget(Button.builder(Component.literal("⌖"),b->recenter())
                .bounds(recenterButtonX,by,button,button).build());
        recenterButton.setTooltip(Tooltip.create(Component.literal("Centrar en jugador")));
    }

    /** Anchored zoom: the terrain beneath the mouse remains beneath the mouse. */
    private void changeZoom(int steps,double mx,double my){
        int next=Math.max(0,Math.min(MAX_ZOOM,zoom+steps));
        if(next==zoom)return;
        double bx=centerX+(mx-(x+w/2.0))*blockPerCell()/CELL;
        double bz=centerZ+(my-(y+h/2.0))*blockPerCell()/CELL;
        zoom=next;
        centerX=bx-(mx-(x+w/2.0))*blockPerCell()/CELL;
        centerZ=bz-(my-(y+h/2.0))*blockPerCell()/CELL;
        invalidate();
    }
    /** Se adapta al alto disponible; nunca deja filas fuera del marco. */
    private record LegendLayout(int left,int top,int width,int height,int rows,int cols){
        int perPage(){return rows*cols;}
    }
    private LegendLayout legendLayout(){
        int panelW=Math.min(308,Math.max(140,w-20));
        int availableH=Math.max(48,h-16);
        int rows=Math.max(1,Math.min(9,(availableH-48)/19));
        int panelH=Math.min(availableH,48+rows*19);
        int columns=panelW>=260?2:1;
        return new LegendLayout(x+w-panelW-8,y+8,panelW,panelH,rows,columns);
    }
    private boolean onLegend(double mx,double my){
        LegendLayout l=legendLayout();
        return legendOpen&&mx>=l.left()&&mx<l.left()+l.width()
                &&my>=l.top()&&my<l.top()+l.height();
    }
    private boolean onMap(double mx,double my){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    /** Desplazar el mapa mueve el centro en direccion contraria al movimiento del mouse. */
    private void panPixels(double deltaX,double deltaY){
        centerX-=deltaX*blockPerCell()/CELL;
        centerZ-=deltaY*blockPerCell()/CELL;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(onLegend(event.x(),event.y()))return true;
        if(onMap(event.x(),event.y())){dragging=true;return true;}
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        if(dragging){panPixels(dx,dy);return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){
        if(dragging){dragging=false;return true;}
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double mx,double my,double dx,double dy){
        if(onLegend(mx,my)&&dy!=0){
            Minecraft mc=Minecraft.getInstance();
            if(mc.level!=null){
                int total=AtlasEntityCatalog.forDimension(mc.level.dimension().toString()).length;
                int perPage=legendLayout().perPage();
                int pages=Math.max(1,(total+perPage-1)/perPage);
                legendPage=Math.max(0,Math.min(pages-1,legendPage+(dy<0?1:-1)));
            }
            return true;
        }
        if(onMap(mx,my)&&dy!=0){changeZoom((dy>0?1:-1)*Math.max(1,(int)Math.round(Math.abs(dy))),mx,my);return true;}
        return super.mouseScrolled(mx,my,dx,dy);
    }
    @Override public boolean keyPressed(KeyEvent event){
        switch(event.key()){
            case InputConstants.KEY_M->{Minecraft.getInstance().gui.setScreen(null);return true;}
            case InputConstants.KEY_U->{Minecraft.getInstance().gui.setScreen(new AtlasScreen());return true;}
            case InputConstants.KEY_LEFT->{panPixels(-CELL*24,0);return true;}
            case InputConstants.KEY_RIGHT->{panPixels(CELL*24,0);return true;}
            case InputConstants.KEY_UP->{panPixels(0,-CELL*24);return true;}
            case InputConstants.KEY_DOWN->{panPixels(0,CELL*24);return true;}
            default -> {}
        }
        return super.keyPressed(event);
    }
    private int screenX(double wx){return x+w/2+(int)Math.round((wx-centerX)/blockPerCell()*CELL);}
    private int screenY(double wz){return y+h/2+(int)Math.round((wz-centerZ)/blockPerCell()*CELL);}
    private boolean inViewport(int px,int py,int margin){
        return px>=x+margin&&px<x+w-margin&&py>=y+margin&&py<y+h-margin;
    }
    private void drawWaypoints(GuiGraphicsExtractor g,Minecraft mc){
        if(!AtlasMinimapConfig.waypoints)return;
        String dimension=mc.level.dimension().toString();
        int drawn=0;
        for(Waypoint wp:AtlasWaypointClient.all(mc)){
            if(drawn>=180)break;
            if(!wp.visible()||!dimension.equals(wp.dimension()))continue;
            int px=screenX(wp.x()+.5),py=screenY(wp.z()+.5);
            if(!inViewport(px,py,12))continue;
            Identifier icon=AtlasWaypointIconTextures.of(AtlasWaypointIcons.get(mc,wp.id()),wp.argb());
            int iconSize=AtlasMinimapConfig.entityIconSize;
            int lo=iconSize/2,hi=iconSize-lo;
            g.blit(icon,px-lo,py-lo,px+hi,py+hi,0,1,0,1);
            String title=wp.name().length()>24?wp.name().substring(0,23)+"…":wp.name();
            float textScale=Math.max(.48f,Math.min(1f,AtlasMinimapConfig.entityIconSize/8f));
            int tw=font.width(title);
            if(px+Math.ceil(tw*textScale)+iconSize+3<x+w){
                g.pose().pushMatrix();
                g.pose().translate(px+iconSize/2f+4,py-4);
                g.pose().scale(textScale,textScale);
                g.text(font,title,0,0,0xFFFFFFFF,false);
                g.pose().popMatrix();
            }
            drawn++;
        }
    }
    private void drawEntities(GuiGraphicsExtractor g,Minecraft mc){
        if(!AtlasMinimapConfig.worldMapEntities)return;
        int count=0;
        for(Entity entity:mc.level.entitiesForRendering()){
            if(entity==mc.player||!entity.isAlive())continue;
            boolean player=entity instanceof Player,monster=AtlasEntityCatalog.isHostile(entity);
            boolean animal=AtlasEntityCatalog.isPassive(entity),villager=AtlasEntityCatalog.isVillager(entity);
            if(player){if(!AtlasMinimapConfig.players)continue;}
            else if((monster&&!AtlasMinimapConfig.mobs)||(animal&&!AtlasMinimapConfig.animals)
                    ||(villager&&!AtlasMinimapConfig.villagers)||(!monster&&!animal&&!villager))continue;
            if(++count>180)break;
            int px=screenX(entity.getX()),py=screenY(entity.getZ());
            int iconSize=AtlasMinimapConfig.entityIconSize;
            if(!inViewport(px,py,iconSize))continue;
            boolean team=player&&mc.player.getTeam()!=null&&mc.player.getTeam()==entity.getTeam();
            boolean friend=player&&AtlasMinimapConfig.friends&&(
                    AtlasMinimapConfig.isFriend(entity.getName().getString())||team);
            Identifier icon=AtlasMinimapHud.entityIcon(entity,friend);
            int lo=iconSize/2,hi=iconSize-lo;
            boolean skin=player&&friend&&AtlasPlayerPortrait.drawPlayer(g,(Player)entity,px,py,iconSize);
            if(!skin)g.blit(icon,px-lo,py-lo,px+hi,py+hi,0,1,0,1);
            if(friend&&AtlasMinimapConfig.showFriendNames){
                String name=entity.getName().getString();
                if(name.length()>16)name=name.substring(0,15)+"…";
                if(px+font.width(name)+8<x+w)g.text(font,name,px+6,py-4,0xFFE6FFF7,false);
            }
        }
    }
    /** Tooltip del tipo real de criatura bajo el mouse (sin depender del icono genérico). */
    private void drawEntityTooltip(GuiGraphicsExtractor g,Minecraft mc,int mouseX,int mouseY){
        if(dragging||legendOpen||!AtlasMinimapConfig.worldMapEntities
                ||!onMap(mouseX,mouseY)||mc.level==null||mc.player==null)return;
        Entity hovered=null;
        double nearest=Double.MAX_VALUE;
        int count=0;
        int iconSize=AtlasMinimapConfig.entityIconSize;
        int radius=Math.max(8,iconSize/2+4); // Fácil acertar incluso a iconos de 3 px.
        for(Entity entity:mc.level.entitiesForRendering()){
            if(entity==mc.player||!entity.isAlive())continue;
            boolean player=entity instanceof Player;
            boolean monster=AtlasEntityCatalog.isHostile(entity);
            boolean animal=AtlasEntityCatalog.isPassive(entity);
            boolean villager=AtlasEntityCatalog.isVillager(entity);
            if(player){if(!AtlasMinimapConfig.players)continue;}
            else if((monster&&!AtlasMinimapConfig.mobs)
                    ||(animal&&!AtlasMinimapConfig.animals)
                    ||(villager&&!AtlasMinimapConfig.villagers)
                    ||(!monster&&!animal&&!villager))continue;
            if(++count>180)break;
            int px=screenX(entity.getX()),py=screenY(entity.getZ());
            if(!inViewport(px,py,iconSize))continue;
            double dx=mouseX-px,dy=mouseY-py;
            double distanceSquared=dx*dx+dy*dy;
            if(distanceSquared<=radius*radius&&distanceSquared<nearest){
                hovered=entity;
                nearest=distanceSquared;
            }
        }
        if(hovered==null)return;
        // getDescription() usa el nombre localizado de la especie: Araña, Creeper, Panda, etc.
        String type=hovered.getType().getDescription().getString();
        String label=hovered instanceof Player?hovered.getName().getString():type;
        if(!(hovered instanceof Player)&&hovered.hasCustomName()){
            label=type+" · "+hovered.getName().getString();
        }
        int dist=(int)Math.round(mc.player.distanceTo(hovered));
        // Diferencia vertical en bloques (pies del jugador vs. pies de la entidad).
        // Un valor positivo significa que la criatura está POR ENCIMA de nosotros.
        int playerY=(int)Math.floor(mc.player.getY());
        int entityY=(int)Math.floor(hovered.getY());
        int deltaY=entityY-playerY;
        String heightInfo=deltaY>0?"↑ "+deltaY+" bloques"
                :deltaY<0?"↓ "+(-deltaY)+" bloques":"Mismo nivel";
        String extra=dist+" m  ·  "+heightInfo;

        // Texto 24% más pequeño que el original; fondo ajustado al contenido.
        final float scale=.76f;
        final int padding=6;
        int available=Math.max(1,w-20);
        int tooltipW=Math.min(available,(int)Math.ceil(
                Math.max(font.width(label),font.width(extra))*scale)+padding*2);
        if(tooltipW<24)return;
        int allowedTextWidth=(int)((tooltipW-padding*2)/scale);
        while(label.length()>1&&font.width(label)>allowedTextWidth){
            label=label.substring(0,label.length()-1);
        }
        if(font.width(label)>allowedTextWidth)label="…";
        while(extra.length()>1&&font.width(extra)>allowedTextWidth){
            extra=extra.substring(0,extra.length()-1);
        }
        int tooltipH=23;
        int tx=Math.max(x+4,Math.min(mouseX+11,x+w-tooltipW-4));
        int ty=Math.max(y+4,Math.min(mouseY-tooltipH-6,y+h-tooltipH-4));
        g.fill(tx,ty,tx+tooltipW,ty+tooltipH,0xED0D1929);
        g.outline(tx,ty,tooltipW,tooltipH,0xFFE8C782);
        g.pose().pushMatrix();
        g.pose().translate(tx+padding,ty+4);
        g.pose().scale(scale,scale);
        g.text(font,label,0,0,0xFFF8EBD2,false);
        g.text(font,extra,0,12,0xFFB5C2D0,false);
        g.pose().popMatrix();
    }

    private void drawZoomMeter(GuiGraphicsExtractor g){
        int percent=(int)Math.round(100.0*zoom/MAX_ZOOM);
        int bx=x+10,by=y+h-17,bw=46;
        String label=percent+"%";
        int textW=font.width(label);
        int right=bx+bw+5+textW+4;
        // Fondo y padding mínimos; el ancho se adapta al porcentaje mostrado.
        g.fill(bx-4,by-3,right,by+11,0xAB0C1A2B);
        g.fill(bx,by+3,bx+bw,by+6,0xFF364152);
        g.fill(bx,by+3,bx+(int)Math.round(bw*percent/100.0),by+6,0xFFE5C782);
        g.text(font,label,bx+bw+5,by,0xFFF6E5B7,false);
    }
    private void drawLegend(GuiGraphicsExtractor g,Minecraft mc){
        if(!legendOpen||mc.level==null)return;
        LegendLayout l=legendLayout();
        int lx=l.left(),ly=l.top(),panelW=l.width(),panelH=l.height();
        g.fill(lx,ly,lx+panelW,ly+panelH,0xF20E1929);
        g.outline(lx,ly,panelW,panelH,GOLD);
        String dimension=mc.level.dimension().toString();
        AtlasEntityCatalog.Entry[] entries=AtlasEntityCatalog.forDimension(dimension);
        int perPage=l.perPage();
        int pages=Math.max(1,(entries.length+perPage-1)/perPage);
        legendPage=Math.max(0,Math.min(pages-1,legendPage));
        String header="CRIATURAS · "+AtlasEntityCatalog.dimensionTitle(dimension);
        while(header.length()>3&&font.width(header)>panelW-15)header=header.substring(0,header.length()-2)+"…";
        g.text(font,header,lx+8,ly+8,GOLD,false);
        int start=legendPage*perPage;
        int colWidth=(panelW-20)/l.cols();
        for(int i=start;i<Math.min(entries.length,start+perPage);i++){
            int local=i-start,col=local/l.rows(),row=local%l.rows();
            int xx=lx+9+col*colWidth,yy=ly+25+row*19;
            AtlasEntityCatalog.Entry entry=entries[i];
            Identifier tex=AtlasEntityCatalog.icon(entry.key());
            g.blit(tex,xx,yy,xx+12,yy+12,0,1,0,1);
            String name=entry.label();
            int available=Math.max(15,colWidth-21);
            if(font.width(name)>available){
                while(name.length()>1&&font.width(name+"…")>available)
                    name=name.substring(0,name.length()-1);
                name+="…";
            }
            g.text(font,name,xx+17,yy+2,0xFFE9DCC8,false);
        }
        int footerY=ly+panelH-18;
        g.fill(lx+8,footerY-4,lx+panelW-8,footerY-3,0x806C5D45);
        String footer="Rueda: pág.   "+(legendPage+1)+" / "+pages;
        g.text(font,footer,lx+9,footerY,0xFFB9B9C4,false);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        Minecraft mc=Minecraft.getInstance();
        g.fill(0,0,width,height,BACK);
        g.text(font,"ATLAS LUMINA NEXUS  |  MAPA MUNDIAL",14,15,GOLD,false);
        g.fill(x-2,y-2,x+w+2,y+h+2,0xFF182337);
        if(mc.player!=null&&mc.level!=null){
            AtlasMinimapConfig.load();AtlasMinimapStore.sync(mc);
            // Una sola textura GPU para todo el terreno, con caché fuera de pantalla.
            // El arrastre desplaza coordenadas UV sin muestrear cada frame.
            g.enableScissor(x,y,x+w,y+h);
            terrainTexture.draw(g,mc,x,y,w,h,centerX,centerZ,blockPerCell(),zoom);
            drawWaypoints(g,mc);
            drawEntities(g,mc);
            int px=screenX(mc.player.getX()),py=screenY(mc.player.getZ());
            if(inViewport(px,py,10)){
                AtlasPlayerPortrait.draw(g,mc,px,py,AtlasMinimapConfig.entityIconSize,
                        Math.toRadians(mc.player.getYRot()),true);
            }
            g.disableScissor();
        }
        g.outline(x-2,y-2,w+4,h+4,GOLD);
        drawZoomMeter(g);
        if(mc.player!=null&&mc.level!=null){
            drawLegend(g,mc);
            drawEntityTooltip(g,mc,mouseX,mouseY);
        }
        // Draw icon images over the compact toolbar buttons, leaving UI controls clickable.
        Identifier visibility=AtlasMinimapConfig.worldMapEntities?CREEPER_ON:CREEPER_OFF;
        super.extractRenderState(g,mouseX,mouseY,delta);
        g.blit(visibility,entityButtonX+4,13,entityButtonX+18,27,0,1,0,1);
    }
    @Override public void removed(){terrainTexture.close();super.removed();}
    @Override public boolean isPauseScreen(){return false;}
}
