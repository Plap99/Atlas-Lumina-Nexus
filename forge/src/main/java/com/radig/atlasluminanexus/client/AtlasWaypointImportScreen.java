package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.*;

/** Confirma claramente si importar sumando o reemplazando todos los waypoints actuales. */
public final class AtlasWaypointImportScreen extends Screen {
    private EditBox source;
    private String status="";
    private int x,y,w,h;
    public AtlasWaypointImportScreen(){super(Component.literal("Atlas - Importar waypoints"));}
    @Override protected void init(){
        w=Math.min(420,width-12);h=Math.min(204,height-10);x=(width-w)/2;y=(height-h)/2;
        int left=x+13,contentW=w-26;
        source=new EditBox(font,left,y+54,contentW,19,Component.literal("Archivo ZIP de ALN"));
        source.setMaxLength(700);
        source.setHint(Component.literal("Ruta de respaldo .zip"));addRenderableWidget(source);
        Path game=Minecraft.getInstance().gameDirectory.toPath();
        Path folder=game.resolve("config/atlasluminanexus/exports");
        try(var list=Files.list(folder)){
            list.filter(p->p.toString().endsWith(".zip")).max((a,b)->{
                try{return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b));}
                catch(Exception e){return 0;}
            }).ifPresent(p->source.setValue(p.toAbsolutePath().toString()));
        }catch(Exception ignored){}
        int half=(contentW-9)/2;
        addRenderableWidget(Button.builder(Component.literal("Agregar"),b->apply(false)).bounds(left,y+99,half,21).build());
        addRenderableWidget(Button.builder(Component.literal("Reemplazar TODOS"),b->apply(true)).bounds(left+half+9,y+99,half,21).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"),b->Minecraft.getInstance().gui.setScreen(new AtlasMinimapSettingsScreen(0)))
            .bounds(left,y+h-32,contentW,20).build());
    }
    private void apply(boolean replace){
        String path=source.getValue().trim();
        if(path.isEmpty()){status="Selecciona un ZIP de ALN";return;}
        try{
            status=AtlasWaypointTransfer.importArchive(Minecraft.getInstance(),Path.of(path),replace);
        }catch(Exception ex){status="Error: "+ex.getMessage();}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float t){
        g.fill(x,y,x+w,y+h,0xFA0E1728);g.outline(x,y,w,h,0xFFE0BA72);
        g.text(font,"IMPORTAR WAYPOINTS",x+13,y+13,0xFFE0BA72,false);
        g.text(font,"Selecciona un respaldo .zip de ALN",x+13,y+35,0xFFB6AD99,false);
        g.text(font,"Agregar permite duplicados. Reemplazar genera respaldo.",x+13,y+132,0xFFB6AD99,false);
        if(!status.isBlank())g.text(font,status.length()>64?status.substring(0,61)+"...":status,x+13,y+155,0xFFFFD46A,false);
        super.extractRenderState(g,mouseX,mouseY,t);
    }
    @Override public boolean isPauseScreen(){return false;}
}
