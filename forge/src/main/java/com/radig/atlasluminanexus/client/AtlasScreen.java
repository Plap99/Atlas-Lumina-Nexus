package com.radig.atlasluminanexus.client;

import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Compact atlas with selectable list and restrained parchment-style details. */
public final class AtlasScreen extends Screen {
    private static final int GOLD=0xFFE3BF75, TEXT=0xFFE7DEC8, MUTED=0xFFB4A993, CYAN=0xFF88D9F4;
    private static final String ICON_BASE="textures/gui/icons/";
    private static Identifier icon(String name){return Identifier.fromNamespaceAndPath("atlasluminanexus",ICON_BASE+name+".png");}
    private static final Identifier ICON_DELETE=icon("trash"), ICON_VISIBLE=icon("eye"), ICON_HIDDEN=icon("eye_off"), ICON_HOME=icon("home"), ICON_HOME_OFF=icon("home_off"), ICON_TELEPORT=icon("teleport"), ICON_SETTINGS=icon("gate_settings"), ICON_MOD_SETTINGS=icon("mod_settings");
    private static final int BG=0xF30D1422, DARK=0xF71A2537, PARCHMENT=0xF0D8C49C, INK=0xFF493D2E;
    private EditBox search;
    private UUID selectedId;
    private int scroll=0;
    private boolean draggingScrollbar;
    private static final int ROW_HEIGHT=21;
    private Button edit,del,visible,home,travel,settings,modSettings;

    private int left,top,right,bottom,side,rowsTop,rowsCount;
    private final List<Button> rowButtons=new ArrayList<>();
    public AtlasScreen(){super(Component.literal("Atlas Lumina Nexus"));}
    private List<Waypoint> results(){
        String q=search==null?"":search.getValue().toLowerCase(Locale.ROOT);
        return AtlasWaypointClient.all(Minecraft.getInstance()).stream()
            .filter(w->w.name().toLowerCase(Locale.ROOT).contains(q)||w.category().toLowerCase(Locale.ROOT).contains(q)).toList();
    }
    private Waypoint current(){
        List<Waypoint> a=results();
        for(Waypoint w:a) if(w.id().equals(selectedId))return w;
        if(a.isEmpty())return null;
        selectedId=a.get(0).id();return a.get(0);
    }
    @Override protected void init(){
        int ww=Math.min(width-18,800),hh=Math.min(height-18,430);
        left=(width-ww)/2;right=left+ww;top=(height-hh)/2;bottom=top+hh;
        side=Math.max(165,Math.min(230,ww*32/100));
        search=new EditBox(font,left+14,top+43,side-21,17,Component.literal("Buscar"));
        search.setHint(Component.literal("Buscar..."));search.setResponder(s->{scroll=0;updateRows();});addRenderableWidget(search);
        int buttonWidth=(side-27)/2;
        addRenderableWidget(Button.builder(Component.literal("+ Nuevo"),b->Minecraft.getInstance().gui.setScreen(new AtlasWaypointEditorScreen(null)))
            .bounds(left+14,top+64,buttonWidth,18).build());
        edit=addRenderableWidget(Button.builder(Component.literal("Editar"),b->{Waypoint w=current();if(w!=null)Minecraft.getInstance().gui.setScreen(new AtlasWaypointEditorScreen(w));})
            .bounds(left+19+buttonWidth,top+64,buttonWidth,18).build());
        rowsTop=top+110;rowsCount=Math.max(1,(bottom-rowsTop-14)/ROW_HEIGHT);
        for(int i=0;i<rowsCount;i++){
            final int slot=i;
            Button b=Button.builder(Component.literal(""),btn->{
                List<Waypoint> points=results();int n=scroll+slot;
                if(n<points.size()){selectedId=points.get(n).id();refresh();}
            }).bounds(left+14,rowsTop+i*ROW_HEIGHT,side-30,ROW_HEIGHT-2).build();
            rowButtons.add(addRenderableWidget(b));
        }
        int rx=left+side+15,rw=right-rx-14,gap=6,third=Math.max(20,Math.min(36,(rw-5*gap)/6));
        int step=third+Math.max(gap,(rw-6*third)/5);
        del=addRenderableWidget(Button.builder(Component.empty(),b->{Waypoint w=current();if(w!=null)Minecraft.getInstance().gui.setScreen(new AtlasDeleteScreen(w));})
            .bounds(rx,top+43,third,18).build());
        visible=addRenderableWidget(Button.builder(Component.empty(),b->{Waypoint w=current();if(w==null)return;try{
            AtlasWaypointClient.upsert(Minecraft.getInstance(),new Waypoint(w.id(),w.name(),w.dimension(),w.x(),w.y(),w.z(),w.argb(),w.category(),!w.visible(),w.emergencyDestination()));
            refresh();}catch(Exception ex){System.err.println("[ALN] "+ex);}}).bounds(rx+third+gap,top+43,third,18).build());
        home=addRenderableWidget(Button.builder(Component.empty(),b->{Waypoint w=current();if(w==null)return;try{
            AtlasWaypointClient.setHome(Minecraft.getInstance(),w.id());refresh();}catch(Exception ex){System.err.println("[ALN] "+ex);}})
            .bounds(rx+2*(third+gap),top+43,third,18).build());
        modSettings=addRenderableWidget(Button.builder(Component.empty(), b ->
                Minecraft.getInstance().gui.setScreen(new AtlasSettingsScreen()))
                .bounds(right-14-3*third-2*gap,top+43,third,18).build());
        modSettings.setTooltip(Tooltip.create(Component.literal("Configurar Atlas Lumina Nexus")));
        settings=addRenderableWidget(Button.builder(Component.empty(), b ->
                Minecraft.getInstance().gui.setScreen(new LuminaGateSettingsScreen()))
                .bounds(right-14-2*third-gap,top+43,third,18).build());
        settings.setTooltip(Tooltip.create(Component.literal("Configurar Lumina Gate")));
        travel=addRenderableWidget(Button.builder(Component.empty(), b -> {
            Waypoint w=current();
            if(w!=null)LuminaGateClient.start(w);
        }).bounds(right-14-third,top+43,third,18).build());
        travel.setTooltip(Tooltip.create(Component.literal("Viajar al waypoint | Lumina Gate")));
        del.setTooltip(Tooltip.create(Component.literal("Eliminar waypoint")));
        visible.setTooltip(Tooltip.create(Component.literal("Mostrar u ocultar waypoint")));
        home.setTooltip(Tooltip.create(Component.literal("Establecer destino seguro")));
        refresh();
    }
    private void updateRows(){
        List<Waypoint> a=results();scroll=Math.max(0,Math.min(scroll,Math.max(0,a.size()-rowsCount)));
        for(int i=0;i<rowButtons.size();i++) rowButtons.get(i).visible=scroll+i<a.size();
        refreshButtons();
    }
    private int maxScroll(){return Math.max(0,results().size()-rowsCount);}
    private int scrollX(){return left+side-12;}
    private int scrollTrackHeight(){return rowsCount*ROW_HEIGHT;}
    private void scrollTo(double mouseY){
        int max=maxScroll();if(max==0)return;
        int track=scrollTrackHeight();
        int thumb=Math.max(13,track*rowsCount/Math.max(rowsCount,results().size()));
        double ratio=(mouseY-rowsTop-thumb*0.5)/Math.max(1,track-thumb);
        scroll=Math.max(0,Math.min(max,(int)Math.round(ratio*max)));
        updateRows();
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        if(maxScroll()>0&&event.x()>=scrollX()-2&&event.x()<=scrollX()+7
                &&event.y()>=rowsTop&&event.y()<=rowsTop+scrollTrackHeight()){
            draggingScrollbar=true;scrollTo(event.y());return true;
        }
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){
        if(draggingScrollbar){scrollTo(event.y());return true;}
        return super.mouseDragged(event,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){
        if(draggingScrollbar){draggingScrollbar=false;return true;}
        return super.mouseReleased(event);
    }
    private void refreshButtons(){
        Waypoint w=current();boolean yes=w!=null;
        for(Button b:new Button[]{edit,del,visible,home,travel})b.active=yes;
        
    }
    private void refresh(){updateRows();}
    @Override public boolean mouseScrolled(double mx,double my,double deltaX,double deltaY){
        if(mx>=left&&mx<=left+side&&my>=rowsTop&&my<=bottom-15){scroll+=(deltaY<0?2:-2);updateRows();return true;}
        return super.mouseScrolled(mx,my,deltaX,deltaY);
    }
    private String shortName(String s,int max){return s.length()>max?s.substring(0,max-1)+"…":s;}
    private String fit(String s,int available){
        if(s==null)return "";
        if(font.width(s)<=available)return s;
        while(s.length()>1&&font.width(s+"…")>available)s=s.substring(0,s.length()-1);
        return s+"…";
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        g.fill(left,top,right,bottom,BG);g.outline(left,top,right-left,bottom-top,GOLD);
        g.text(font,"✦ ATLAS LUMINA NEXUS",left+14,top+13,GOLD);
        g.text(font,"Explore • Discover • Connect",left+218,top+13,MUTED);
        g.horizontalLine(left+12,right-12,top+34,0xFF6E5C40);
        int lx=left+10;g.fill(lx,top+39,left+side+2,bottom-10,DARK);
        List<Waypoint> a=results();
        g.text(font,"WAYPOINTS ("+a.size()+")",left+15,top+94,GOLD);
        for(int i=0;i<rowsCount;i++){
            int n=scroll+i;if(n>=a.size())break;Waypoint w=a.get(n);int yy=rowsTop+i*ROW_HEIGHT;
            boolean chosen=w.id().equals(current()==null?null:selectedId);
            if(chosen)g.outline(left+13,yy-1,side-27,ROW_HEIGHT,0xFFB99858);

        }
        if(a.size()>rowsCount){
            int sx=scrollX(),height=scrollTrackHeight();
            int thumb=Math.max(13,height*rowsCount/a.size());
            int offset=(int)Math.round((height-thumb)*scroll/(double)Math.max(1,a.size()-rowsCount));
            g.fill(sx,rowsTop,sx+3,rowsTop+height,0x66566B7B);
            g.fill(sx-1,rowsTop+offset,sx+4,rowsTop+offset+thumb,0xFFC5AB72);
        }
        int rx=left+side+10,rx2=right-10,py=top+72,pbottom=bottom-12;
        // Subtle antique parchment: restrained double line, dark-ink text without drop shadows.
        g.fill(rx,py,rx2,pbottom,0xFFF0E2C3);
        g.outline(rx,py,rx2-rx,pbottom-py,0xFFAA8952);
        g.outline(rx+3,py+3,rx2-rx-6,pbottom-py-6,0xFFCEB788);
        Waypoint w=current();
        if(w!=null){
            int detailsWidth=Math.max(105,(rx2-rx)*42/100);
            int infoX=rx+12, infoRight=rx+detailsWidth;
            int previewX=infoRight+8, previewRight=rx2-12;
            g.fill(rx+8,py+9,rx2-8,py+29,0x33A88743);
            String displayName=fit(w.name(),rx2-rx-(w.emergencyDestination()?58:34));
            int nameWidth=font.width(displayName);
            int headerCenter=(rx+rx2)/2;
            int nameX=headerCenter-(nameWidth+(w.emergencyDestination()?17:0))/2;
            g.text(font,displayName,nameX,py+14,0xFF382917,false);
            if(w.emergencyDestination()) g.blit(ICON_HOME,nameX+nameWidth+4,py+14,nameX+nameWidth+16,py+26,0f,1f,0f,1f);
            g.horizontalLine(infoX,rx2-12,py+31,0xFFAF9566);
            g.text(font,"Categoría",infoX,py+46,0xFF82684D,false);
            g.text(font,fit(w.category(),detailsWidth-20),infoX,py+60,INK,false);
            String dim=w.dimension(); if(dim.contains("overworld"))dim="Overworld";else if(dim.contains("the_nether"))dim="Nether";else if(dim.contains("the_end"))dim="End";
            g.text(font,"Mundo",infoX,py+82,0xFF82684D,false);
            g.text(font,fit(dim,detailsWidth-20),infoX,py+96,INK,false);
            g.text(font,"Coordenadas",infoX,py+116,0xFF82684D,false);
            String xyz=""+(int)Math.floor(w.x())+", "+(int)Math.floor(w.y())+", "+(int)Math.floor(w.z());
            g.text(font,fit(xyz,detailsWidth-20),infoX,py+132,INK,false);

            // 16:9 remembered view, not a world render: it remains available offline.
            int pw=Math.max(40,previewRight-previewX);
            int ph=Math.min((pw*9)/16, Math.max(45,pbottom-py-72));
            int iy=py+50;
            g.fill(previewX-3,iy-3,previewRight+3,iy+ph+3,0xFF55402F);
            g.outline(previewX-3,iy-3,pw+6,ph+6,0xFF9A773E);
            Identifier picture=AtlasPreviewClient.textureId(Minecraft.getInstance(),w.id());
            if(picture!=null){
                g.blit(picture,previewX,iy,previewRight,iy+ph,0f,1f,0f,1f);
            }else{
                g.fill(previewX,iy,previewRight,iy+ph,0xFF363447);
                g.centeredText(font,"Sin captura",(previewX+previewRight)/2,iy+ph/2,MUTED);
            }
            
        }else g.text(font,"Crea tu primer waypoint",rx+12,py+14,INK,false);
        super.extractRenderState(g,mx,my,tick);
        // Draw icons above the vanilla buttons (textures instead of Unicode glyphs).
        drawIcon(g,del,ICON_DELETE);
        Waypoint selected=current();
        drawIcon(g,visible,selected!=null&&selected.visible()?ICON_VISIBLE:ICON_HIDDEN);
        drawIcon(g,home,selected!=null&&selected.emergencyDestination()?ICON_HOME:ICON_HOME_OFF);
        drawIcon(g,modSettings,ICON_MOD_SETTINGS);
        drawIcon(g,settings,ICON_SETTINGS);
        drawIcon(g,travel,ICON_TELEPORT);
        for(int i=0;i<rowsCount;i++){
            int n=scroll+i;if(n>=a.size())break;Waypoint item=a.get(n);int yy=rowsTop+i*ROW_HEIGHT;
            Identifier typeIcon=AtlasWaypointIconTextures.of(
                    AtlasWaypointIcons.get(Minecraft.getInstance(), item.id()), item.argb());
            g.blit(typeIcon,left+18,yy+5,left+30,yy+17,0f,1f,0f,1f);
            if(item.emergencyDestination())
                g.blit(ICON_HOME,left+32,yy+6,left+42,yy+16,0f,1f,0f,1f);
            int textLeft=left+(item.emergencyDestination()?45:34);
            g.text(font,fit(item.name(),side-(item.emergencyDestination()?88:77)),
                    textLeft,yy+6,item.id().equals(selectedId)?GOLD:TEXT,false);
        }
    }
    private void drawIcon(GuiGraphicsExtractor g,Button b,Identifier id){
        if(b==null||!b.visible)return;
        int x=b.getX()+(b.getWidth()-12)/2;
        int y=b.getY()+(b.getHeight()-12)/2;
        g.blit(id,x,y,x+12,y+12,0f,1f,0f,1f);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(event.key()==InputConstants.KEY_U){
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }
        if(event.key()==InputConstants.KEY_M){
            Minecraft.getInstance().gui.setScreen(new AtlasWorldMapScreen());
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean isPauseScreen(){return false;}
}
