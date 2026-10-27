package com.radig.atlasluminanexus.client;

import com.radig.atlasluminanexus.core.Waypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class AtlasPhotoScreen extends Screen {
    private final Waypoint point;
    public AtlasPhotoScreen(Waypoint p){super(Component.literal("Fotografía del waypoint"));point=p;}
    @Override protected void init(){
        addRenderableWidget(Button.builder(Component.literal("Cerrar"),b->Minecraft.getInstance().gui.setScreen(new AtlasScreen()))
          .bounds(width/2-48,height-37,96,19).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick){
        g.fill(0,0,width,height,0xF80C1421);
        int ww=Math.min(width-45,(height-80)*16/9),hh=ww*9/16;
        int x=(width-ww)/2,y=(height-hh)/2;
        g.outline(x-5,y-5,ww+10,hh+10,0xFFCCAA64);
        Identifier image=AtlasPreviewClient.textureId(Minecraft.getInstance(),point.id());
        if(image!=null)g.blit(image,x,y,x+ww,y+hh,0f,1f,0f,1f);
        else g.centeredText(font,"Sin captura",width/2,height/2,0xFFFFFFFF);
        g.centeredText(font,point.name(),width/2,Math.max(12,y-20),0xFFE3BF75);
        super.extractRenderState(g,mx,my,tick);
    }
    @Override public boolean isPauseScreen(){return false;}
}
