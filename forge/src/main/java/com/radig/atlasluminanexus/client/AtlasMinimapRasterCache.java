package com.radig.atlasluminanexus.client;

import net.minecraft.client.Minecraft;

/** Reutiliza pixels por frame; limite de memoria: 128x128x4 bytes por mapa. */
public final class AtlasMinimapRasterCache {
    private static int[] pixels=new int[0];
    private static int radius=-1,zoom=-1;
    private static int blockX=Integer.MIN_VALUE,blockZ=Integer.MIN_VALUE,angle=Integer.MIN_VALUE;
    private static int mode=-1;
    private static long lastTick=-1000;
    private static String lastDimension="";
    private static final double[] PIXELS_PER_CELL={.72,1.0,1.4,1.85,2.45};
    private AtlasMinimapRasterCache(){}
    public static int[] snapshot(Minecraft mc,int r,double px,double pz,double yaw,boolean rotate,
                                  int zoomNow,boolean high,boolean smooth,boolean grid,boolean round){
        int bx=(int)Math.floor(px),bz=(int)Math.floor(pz),degree=rotate?(int)Math.floor(Math.toDegrees(yaw)/3.0):0;
        int flags=(high?1:0)|(smooth?2:0)|(grid?4:0)|(round?8:0)|(rotate?16:0)|(AtlasMinimapConfig.terrainMinimap<<5);
        long tick=mc.level.getGameTime();
        String dimension=mc.level.dimension().toString();
        int side=2*r;
        if(radius==r&&zoom==zoomNow&&bx==blockX&&bz==blockZ&&angle==degree&&flags==mode
             &&tick>=lastTick&&tick-lastTick<18&&dimension.equals(lastDimension)&&pixels.length==side*side)return pixels;
        radius=r;zoom=zoomNow;blockX=bx;blockZ=bz;angle=degree;mode=flags;lastTick=tick;lastDimension=dimension;
        if(pixels.length!=side*side)pixels=new int[side*side];
        double step=2.0/PIXELS_PER_CELL[zoomNow];
        double co=Math.cos(yaw),si=Math.sin(yaw);
        for(int sy=0;sy<side;sy++)for(int sx=0;sx<side;sx++){
            double rx=(sx-r+.5)*step,rz=(sy-r+.5)*step;
            if(round&&rx*rx+rz*rz>(r+.35)*(r+.35)*step*step){pixels[sy*side+sx]=0;continue;}
            if(!round&&(Math.abs(sx-r+.5)>r||Math.abs(sy-r+.5)>r)){
                pixels[sy*side+sx]=0;continue;
            }
            double wx=rotate?-co*rx+si*rz:rx,wz=rotate?-si*rx-co*rz:rz;
            double mx=px+wx,mz=pz+wz;
            int color=AtlasMinimapVisuals.sample(mx,mz,step,high,smooth,AtlasMinimapConfig.terrainMinimap);
            if(color==0)color=AtlasMinimapVisuals.unknown((int)Math.floor(mx),(int)Math.floor(mz),grid);
            pixels[sy*side+sx]=color;
        }
        return pixels;
    }
    public static void invalidate(){radius=-1;}
}
