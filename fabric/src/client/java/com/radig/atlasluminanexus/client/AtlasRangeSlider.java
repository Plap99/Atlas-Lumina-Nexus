package com.radig.atlasluminanexus.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;

/** Slider real del juego, con rango entero y guardado por arrastre. */
public final class AtlasRangeSlider extends AbstractSliderButton {
    private final String title;
    private final int min,max,step;
    private final String suffix;
    private final IntConsumer changed;
    private int last;
    public AtlasRangeSlider(int x,int y,int w,int h,String title,int min,int max,int step,int initial,String suffix,IntConsumer changed){
        super(x,y,w,h,Component.literal(""),Math.max(0,Math.min(1,(initial-min)/(double)(max-min))));
        this.title=title;this.min=min;this.max=max;this.step=Math.max(1,step);this.suffix=suffix;this.changed=changed;
        this.last=valueInt();updateMessage();
    }
    public int valueInt(){int n=min+(int)Math.round((max-min)*value/step)*step;return Math.max(min,Math.min(max,n));}
    @Override protected void updateMessage(){setMessage(Component.literal(title+": "+valueInt()+suffix));}
    @Override protected void applyValue(){int n=valueInt();if(n!=last){last=n;changed.accept(n);}}
}
