package com.radig.atlasluminanexus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Configuración unificada: General, Mapa, Marcadores y Jugador. */
public final class AtlasMinimapSettingsScreen extends Screen {
    private static final int GOLD=0xFFE0BA72,MUTED=0xFFB6AD99;
    private static final String[] TABS={"General","Mapa","Marcadores","Jugador"};
    private static final String[] STYLES={"Cuerpo completo","Cabeza","Flecha","Punto"};
    private static final String[] COLOR_NAMES={"Dorado","Cian","Morado","Verde","Rojo","Blanco"};
    private int tab,x,y,pw,ph,tabY,tabW;
    private int mapPage=0; // 0: Vista, 1: Terreno. Conserva la pestaña al reconstruir widgets.
    private EditBox friendInput;
    private String notice="";
    public AtlasMinimapSettingsScreen(){this(0);}
    // Compatibilidad con las ventanas de selección de los pasos anteriores.
    public AtlasMinimapSettingsScreen(boolean markers){this(markers?2:1);}
    public AtlasMinimapSettingsScreen(int selectedTab){
        super(Component.literal("Atlas - Configuración"));
        tab=Math.max(0,Math.min(3,selectedTab));
    }
    private Button button(String label,int bx,int by,int bw,int bh,Runnable cb){
        return addRenderableWidget(Button.builder(Component.literal(label),b->cb.run()).bounds(bx,by,bw,bh).build());
    }
    private void toggle(String label,boolean on,int bx,int by,int bw,Runnable cb){
        button(label+": "+(on?"Sí":"No"),bx,by,bw,18,()->{cb.run();AtlasMinimapConfig.save();rebuildWidgets();});
    }
    private void choose(String label,int code,int bx,int by,int bw){
        button(label,bx,by,bw,18,()->Minecraft.getInstance().gui.setScreen(new AtlasMinimapChoiceScreen(false,code)));
    }
    @Override protected void init(){
        AtlasMinimapConfig.load();
        AtlasDisplayConfig.load();
        pw=Math.min(460,width-12);ph=Math.min(302,height-8);
        x=(width-pw)/2;y=(height-ph)/2;
        int lx=x+12,bw=pw-24,col=(bw-8)/2,rx=lx+col+8;
        tabY=y+34;tabW=(bw-12)/4;
        for(int i=0;i<4;i++){
            final int target=i;
            button(TABS[i],lx+i*(tabW+4),tabY,tabW,20,()->{tab=target;rebuildWidgets();});
        }
        int start=y+63,step=23,bottom=y+ph-28;
        if(tab==0){
            button("Exportar waypoints (ZIP)",lx,start,bw,20,()->{
                try{notice="Exportado: "+AtlasWaypointTransfer.exportCurrent(Minecraft.getInstance()).getFileName();}
                catch(Exception ex){notice="Error al exportar: "+ex.getMessage();}
            });
            button("Importar waypoints...",lx,start+30,bw,20,()->Minecraft.getInstance().gui.setScreen(new AtlasWaypointImportScreen()));
            button("Carpeta de respaldos",lx,start+60,bw,20,()->{
                notice="config/atlasluminanexus/exports/";
            });
        }else if(tab==1){
            // Las dos subpestañas impiden que el deslizador quede tapado por el pie
            // en pantallas con escala de interfaz grande.
            int sectionY=y+60;
            button("Vista y controles",lx,sectionY,col,19,()->{mapPage=0;rebuildWidgets();});
            button("Terreno y calidad",rx,sectionY,col,19,()->{mapPage=1;rebuildWidgets();});
            int viewStart=y+87;
            int viewStep=Math.max(17,Math.min(23,(bottom-viewStart-20)/4));
            if(mapPage==0){
                toggle("Minimapa",AtlasMinimapConfig.enabled,lx,viewStart,col,()->AtlasMinimapConfig.enabled=!AtlasMinimapConfig.enabled);
                toggle("Mapa gira",AtlasMinimapConfig.rotate,rx,viewStart,col,()->AtlasMinimapConfig.rotate=!AtlasMinimapConfig.rotate);
                choose("Forma: "+(AtlasMinimapConfig.round?"Redondo":"Cuadrado")+" >",0,lx,viewStart+viewStep,col);
                toggle("Mostrar norte",AtlasMinimapConfig.showNorth,rx,viewStart+viewStep,col,()->AtlasMinimapConfig.showNorth=!AtlasMinimapConfig.showNorth);
                choose("Tamaño: "+new String[]{"Pequeño","Compacto","Mediano","Grande","Muy grande"}[AtlasMinimapConfig.size]+" >",1,lx,viewStart+viewStep*2,col);
                toggle("Coordenadas",AtlasMinimapConfig.showCoordinates,rx,viewStart+viewStep*2,col,()->AtlasMinimapConfig.showCoordinates=!AtlasMinimapConfig.showCoordinates);
                choose("Zoom: "+new String[]{"Muy lejos","Lejos","Normal","Cerca","Muy cerca"}[AtlasMinimapConfig.zoom]+" >",2,lx,viewStart+viewStep*3,col);
                toggle("Niebla",AtlasMinimapConfig.showUnknownGrid,rx,viewStart+viewStep*3,col,()->AtlasMinimapConfig.showUnknownGrid=!AtlasMinimapConfig.showUnknownGrid);
                addRenderableWidget(new AtlasRangeSlider(lx,viewStart+viewStep*4,bw,18,
                    "Opacidad del minimapa",30,100,1,AtlasMinimapConfig.opacity,"%",n->{
                    AtlasMinimapConfig.opacity=n;AtlasMinimapConfig.save();
                }));
            }else{
                choose("Posición: "+(AtlasMinimapConfig.left?"Izquierda":"Derecha")+" >",3,lx,viewStart,col);
                toggle("Marco Lumina",AtlasMinimapConfig.luminaFrame,rx,viewStart,col,()->AtlasMinimapConfig.luminaFrame=!AtlasMinimapConfig.luminaFrame);
                choose("Mini: "+AtlasTerrainEngine.NAMES[AtlasMinimapConfig.terrainMinimap]+" >",4,lx,viewStart+viewStep,col);
                choose("Mundial: "+AtlasTerrainEngine.NAMES[AtlasMinimapConfig.terrainWorldMap]+" >",5,rx,viewStart+viewStep,col);
                toggle("Suavizado",AtlasMinimapConfig.smoothTerrain,lx,viewStart+viewStep*2,col,()->AtlasMinimapConfig.smoothTerrain=!AtlasMinimapConfig.smoothTerrain);
                toggle("Etiqueta ALN",AtlasMinimapConfig.showBrand,rx,viewStart+viewStep*2,col,()->AtlasMinimapConfig.showBrand=!AtlasMinimapConfig.showBrand);
            }
        }else if(tab==2){
            toggle("Waypoints",AtlasMinimapConfig.waypoints,lx,start,col,()->AtlasMinimapConfig.waypoints=!AtlasMinimapConfig.waypoints);
            toggle("Nombres",AtlasMinimapConfig.waypointLabels,rx,start,col,()->AtlasMinimapConfig.waypointLabels=!AtlasMinimapConfig.waypointLabels);
            toggle("Jugadores",AtlasMinimapConfig.players,lx,start+step,col,()->AtlasMinimapConfig.players=!AtlasMinimapConfig.players);
            toggle("Amigos",AtlasMinimapConfig.friends,rx,start+step,col,()->AtlasMinimapConfig.friends=!AtlasMinimapConfig.friends);
            toggle("Nombre de amigos",AtlasMinimapConfig.showFriendNames,lx,start+step*2,col,()->AtlasMinimapConfig.showFriendNames=!AtlasMinimapConfig.showFriendNames);
            toggle("Hostiles",AtlasMinimapConfig.mobs,rx,start+step*2,col,()->AtlasMinimapConfig.mobs=!AtlasMinimapConfig.mobs);
            toggle("Animales",AtlasMinimapConfig.animals,lx,start+step*3,col,()->AtlasMinimapConfig.animals=!AtlasMinimapConfig.animals);
            toggle("Aldeanos",AtlasMinimapConfig.villagers,rx,start+step*3,col,()->AtlasMinimapConfig.villagers=!AtlasMinimapConfig.villagers);
            addRenderableWidget(new AtlasRangeSlider(lx,start+step*4,bw,18,"Tamaño criaturas",3,10,1,AtlasMinimapConfig.entityIconSize," px",n->{AtlasMinimapConfig.entityIconSize=n;AtlasMinimapConfig.save();}));
            addRenderableWidget(new AtlasRangeSlider(lx,start+step*5,col,18,"Iconos mundo",4,16,1,AtlasDisplayConfig.iconSize()," px",AtlasDisplayConfig::setIconSize));
            addRenderableWidget(new AtlasRangeSlider(rx,start+step*5,col,18,"Letras mundo",30,150,5,AtlasDisplayConfig.textPercent(),"%",AtlasDisplayConfig::setTextPercent));
            if(start+step*6+20<bottom){
                friendInput=new EditBox(font,lx,start+step*6,bw-111,19,Component.literal("Nombre de amigo"));
                friendInput.setHint(Component.literal("Añadir o quitar amigo"));addRenderableWidget(friendInput);
                button("+",lx+bw-105,start+step*6,48,19,()->{AtlasMinimapConfig.addFriends(friendInput.getValue());rebuildWidgets();});
                button("−",lx+bw-51,start+step*6,51,19,()->{AtlasMinimapConfig.removeFriend(friendInput.getValue());rebuildWidgets();});
            }
        }else{
            for(int i=0;i<4;i++){
                final int selected=i;
                int bx=i%2==0?lx:rx,by=start+(i/2)*28;
                button((AtlasMinimapConfig.playerMarkerStyle==i?"✓ ":"")+STYLES[i],bx,by,col,22,()->{
                    AtlasMinimapConfig.playerMarkerStyle=selected;
                    AtlasMinimapConfig.animatedAvatar=selected==0;
                    AtlasMinimapConfig.save();rebuildWidgets();
                });
            }
            int sy=start+65;
            if(AtlasMinimapConfig.playerMarkerStyle<2){
                toggle("Mostrar equipo",AtlasMinimapConfig.playerShowEquipment,lx,sy,bw,()->AtlasMinimapConfig.playerShowEquipment=!AtlasMinimapConfig.playerShowEquipment);
            }else{
                int colorsY=sy+1,cell=Math.min(28,(bw-25)/6);
                for(int i=0;i<6;i++){
                    final int chosen=i;
                    button(" ",lx+i*(cell+4),colorsY,cell,18,()->{
                        AtlasMinimapConfig.playerMarkerColor=chosen;AtlasMinimapConfig.save();rebuildWidgets();
                    });
                }
            }
            addRenderableWidget(new AtlasRangeSlider(lx,start+93,bw,18,"Tamaño del jugador",1,28,1,AtlasMinimapConfig.playerMarkerSize," px",n->{
                AtlasMinimapConfig.playerMarkerSize=n;AtlasMinimapConfig.save();
            }));
        }
        button("Volver al Atlas",lx,bottom,bw,20,()->Minecraft.getInstance().gui.setScreen(new AtlasScreen()));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float partial){
        g.fill(x,y,x+pw,y+ph,0xFA0E1728);g.outline(x,y,pw,ph,GOLD);
        g.text(font,"ATLAS  |  CONFIGURACIÓN",x+12,y+11,GOLD,false);
        int tx=x+12+(tabW+4)*tab;
        g.fill(tx,tabY+21,tx+tabW,tabY+23,GOLD);
        if(tab==1){
            int full=pw-24,col=(full-8)/2,sx=x+12+(mapPage==0?0:col+8);
            g.fill(sx,y+79,sx+col,y+81,GOLD); // subpestaña activa
        }
        if(tab==0){
            g.text(font,"Respaldo del mundo actual, con fotos e iconos.",x+12,y+163,MUTED,false);
            g.text(font,"Importar: agregar duplicados o reemplazar.",x+12,y+177,MUTED,false);
        }
        if(tab==2 && ph>=268){
            String names=String.join(", ",AtlasMinimapConfig.friendNames);
            if(names.length()>49)names=names.substring(0,46)+"...";
            g.text(font,"Amigos manuales: "+(names.isEmpty()?"ninguno":names),x+12,y+ph-47,MUTED,false);
        }
        if(tab==3){
            // Título de la paleta: las muestras deben pintarse DESPUÉS de los botones.
            if(AtlasMinimapConfig.playerMarkerStyle>=2){
                g.text(font,"Color del marcador",x+12,y+63+66-13,MUTED,false);
            }
            g.text(font,"Mínimo visual: cuerpo 10 px · cabeza 8 px",x+12,y+ph-47,MUTED,false);
        }
        if(!notice.isBlank()){
            String shortNotice=notice.length()>65?notice.substring(0,62)+"...":notice;
            g.text(font,shortNotice,x+12,y+ph-47,GOLD,false);
        }
        super.extractRenderState(g,mx,my,partial);
        // Las texturas de los botones se dibujan en super: antes ocultaban la paleta.
        // Pintar las muestras AL FINAL hace visible el color real, también en Forge.
        if(tab==3 && AtlasMinimapConfig.playerMarkerStyle>=2){
            int bw=pw-24,cell=Math.min(28,(bw-25)/6),sy=y+63+66;
            int hovered=-1;
            for(int i=0;i<COLOR_NAMES.length;i++){
                int cx=x+12+i*(cell+4),color=AtlasPlayerMarker.COLORS[i];
                g.fill(cx+5,sy+4,cx+cell-5,sy+14,color);
                g.outline(cx+4,sy+3,cell-8,12,0xFF292929);
                if(i==AtlasMinimapConfig.playerMarkerColor){
                    g.outline(cx-2,sy-2,cell+4,22,GOLD);
                }
                if(mx>=cx && mx<cx+cell && my>=sy && my<sy+18)hovered=i;
            }
            int shown=hovered>=0?hovered:AtlasMinimapConfig.playerMarkerColor;
            g.text(font,(hovered>=0?"Elegir: ":"Seleccionado: ")+COLOR_NAMES[shown],x+12,y+ph-68,MUTED,false);
        }
    }
    @Override public boolean keyPressed(KeyEvent ev){
        if(ev.key()==InputConstants.KEY_U){Minecraft.getInstance().gui.setScreen(new AtlasScreen());return true;}
        if(ev.key()==InputConstants.KEY_M){Minecraft.getInstance().gui.setScreen(new AtlasWorldMapScreen());return true;}
        return super.keyPressed(ev);
    }
    @Override public boolean isPauseScreen(){return false;}
}
