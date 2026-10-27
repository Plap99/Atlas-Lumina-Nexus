package com.radig.atlasluminanexus.client;
import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class AtlasDeleteScreen extends Screen {
    private final Waypoint selected;
    private String error="";
    public AtlasDeleteScreen(Waypoint wp){super(Component.literal("Eliminar waypoint"));selected=wp;}
    @Override protected void init(){
        int x=width/2-110,y=height/2+20;
        addRenderableWidget(Button.builder(Component.literal("Eliminar"),b->{
            try{AtlasWaypointClient.delete(Minecraft.getInstance(),selected.id());Minecraft.getInstance().gui.setScreen(new AtlasScreen());}
            catch(Exception e){error=e.getMessage();}
        }).bounds(x,y,105,20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"),b->Minecraft.getInstance().gui.setScreen(new AtlasScreen())).bounds(x+115,y,105,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float t){
        g.fill(width/2-150,height/2-62,width/2+150,height/2+64,0xF50D1425);
        g.outline(width/2-150,height/2-62,300,126,0xFF91733D);
        g.centeredText(font,"¿Eliminar este waypoint?",width/2,height/2-42,0xFFFFD46A);
        g.centeredText(font,selected.name(),width/2,height/2-17,0xFFFFFFFF);
        if(!error.isEmpty())g.centeredText(font,error,width/2,height/2+52,0xFFFF7777);
        super.extractRenderState(g,mx,my,t);
    }
    @Override public boolean isPauseScreen(){return false;}
}
