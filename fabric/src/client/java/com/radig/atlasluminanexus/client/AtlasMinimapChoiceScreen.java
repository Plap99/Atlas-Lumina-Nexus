package com.radig.atlasluminanexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Selector tipo pergamino; cambia solo el ajuste solicitado. */
public final class AtlasMinimapChoiceScreen extends Screen {
    private final boolean markers;
    private final int type;
    private int x,y,w,h;
    private static final String[][] CHOICES={
        {"Redondo","Cuadrado"},
        {"Pequeño","Compacto","Mediano","Grande","Muy grande"},
        {"Muy lejos","Lejos","Normal","Cerca","Muy cerca"},
        {"Izquierda","Derecha"},
        {"Clásico","Detallado","Lumina HD","Lumina Balanced"},
        {"Clásico","Detallado","Lumina HD","Lumina Balanced"}
    };
    public AtlasMinimapChoiceScreen(boolean markers,int type){super(Component.literal("Atlas - Elegir"));this.markers=markers;this.type=type;}
    private int current(){return switch(type){case 0->AtlasMinimapConfig.round?0:1;case 1->AtlasMinimapConfig.size;case 2->AtlasMinimapConfig.zoom;
        case 3->AtlasMinimapConfig.left?0:1;
        case 4->AtlasMinimapConfig.terrainMinimap;
        case 5->AtlasMinimapConfig.terrainWorldMap;
        default->0;};}
    private void set(int n){
        switch(type){case 0->AtlasMinimapConfig.round=n==0;case 1->AtlasMinimapConfig.size=n;case 2->AtlasMinimapConfig.zoom=n;case 3->AtlasMinimapConfig.left=n==0;
        case 4->AtlasMinimapConfig.terrainMinimap=n;
        case 5->AtlasMinimapConfig.terrainWorldMap=n;
        default->{} }
        AtlasMinimapConfig.save();back();
    }
    private void back(){Minecraft.getInstance().gui.setScreen(new AtlasMinimapSettingsScreen(markers));}
    @Override protected void init(){
        w=Math.min(310,width-32);h=Math.min(230,height-20);x=(width-w)/2;y=(height-h)/2;
        String[] labels=CHOICES[type];int col=(w-36)/2;
        for(int i=0;i<labels.length;i++){
            final int index=i;
            int bx=x+14+(i%2)*(col+8),by=y+51+(i/2)*34;
            addRenderableWidget(Button.builder(Component.literal((current()==i?"✓  ":"")+labels[i]),b->set(index)).bounds(bx,by,col,26).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Cancelar"),b->back()).bounds(x+14,y+h-36,w-28,22).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){
        g.fill(x,y,x+w,y+h,0xFFF1E3C3);g.outline(x,y,w,h,0xFFB5904C);
        g.fill(x+8,y+8,x+w-8,y+38,0xFFDEC69E);
        g.centeredText(font,new String[]{"Forma del mapa","Tamaño del minimapa","Zoom del mapa","Posición","Terreno: minimapa","Terreno: mapa mundial"}[type],x+w/2,y+18,0xFF493A24);
        super.extractRenderState(g,mx,my,pt);
    }
    @Override public boolean keyPressed(KeyEvent ev){if(ev.key()==InputConstants.KEY_U||ev.key()==InputConstants.KEY_M){back();return true;}return super.keyPressed(ev);}
    @Override public boolean isPauseScreen(){return false;}
}
