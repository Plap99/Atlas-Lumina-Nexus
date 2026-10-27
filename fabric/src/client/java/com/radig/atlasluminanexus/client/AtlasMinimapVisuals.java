package com.radig.atlasluminanexus.client;

/** Color grading and lightweight multisample filtering for ALN minimap tiles. No world access. */
public final class AtlasMinimapVisuals {
    private AtlasMinimapVisuals() {}
    private static int clip(int v){return v<0?0:Math.min(255,v);}
    private static int rgb(int r,int g,int b){return 0xFF000000|(clip(r)<<16)|(clip(g)<<8)|clip(b);}
    public static int blend(int a,int b,int percent){
        int k=Math.max(0,Math.min(100,percent)),inv=100-k;
        return rgb((((a>>16)&255)*k+((b>>16)&255)*inv)/100,
                (((a>>8)&255)*k+((b>>8)&255)*inv)/100,
                ((a&255)*k+(b&255)*inv)/100);
    }
    private static int hi(int x,int z){return AtlasMinimapFineStore.height(AtlasMinimapFineStore.data(x,z));}
    private static int tileColor(int x,int z,boolean high){
        long tile=high?AtlasMinimapFineStore.data(x,z):AtlasMinimapStore.data(Math.floorDiv(x,2),Math.floorDiv(z,2));
        if(tile==0)return 0;
        int c=(int)tile,h=high?AtlasMinimapFineStore.height(tile):AtlasMinimapStore.height(tile);
        int r=(c>>16)&255,g=(c>>8)&255,b=c&255;
        boolean water=high&&AtlasMinimapFineStore.water(tile);
        if(water){
            int depth=AtlasMinimapFineStore.waterDepth(tile);
            // Surface color plus the sea-floor height: shallow coasts are lighter.
            int deep=Math.min(23,Math.max(0,depth-1)*2);
            return rgb(45-deep/3,111-deep/2,154-deep/4);
        }
        // Mild natural-grade palette: preserve the material's real map color.
        if(g>r*1.15 && g>b*1.08){r=(r*95)/100;g=(g*104)/100;b=(b*94)/100;}
        else if(r>g*1.2&&g>b*1.12){r=(r*103)/100;g=(g*102)/100;}
        int light=4;
        if(h!=Integer.MIN_VALUE){
            int west=high?hi(x-1,z):AtlasMinimapStore.height(AtlasMinimapStore.data(Math.floorDiv(x-2,2),Math.floorDiv(z,2)));
            int north=high?hi(x,z-1):AtlasMinimapStore.height(AtlasMinimapStore.data(Math.floorDiv(x,2),Math.floorDiv(z-2,2)));
            if(west!=Integer.MIN_VALUE)light+=Math.max(-13,Math.min(13,(h-west)*3));
            if(north!=Integer.MIN_VALUE)light+=Math.max(-11,Math.min(11,(h-north)*2));
        }
        return rgb(r+light,g+light,b+light);
    }
    /** 2x2 subpixel area sampling keeps fine structures while softening distant aliasing. */
    public static int sample(double wx,double wz,double blocksPerPixel,boolean high,boolean smooth){
        int center=tileColor((int)Math.floor(wx),(int)Math.floor(wz),high);
        if(!smooth||blocksPerPixel<0.80)return center;
        double o=Math.min(.85,blocksPerPixel*.29);
        int[] samples={tileColor((int)Math.floor(wx-o),(int)Math.floor(wz-o),high),
                tileColor((int)Math.floor(wx+o),(int)Math.floor(wz-o),high),
                tileColor((int)Math.floor(wx-o),(int)Math.floor(wz+o),high),
                tileColor((int)Math.floor(wx+o),(int)Math.floor(wz+o),high)};
        int r=0,g=0,b=0,n=0;
        for(int c:samples)if(c!=0){r+=(c>>16)&255;g+=(c>>8)&255;b+=c&255;n++;}
        if(n==0)return center;
        int average=rgb(r/n,g/n,b/n);
        return center==0?average:blend(average,center,70);
    }
    /** Terrain Engine quality: 0 classic, 1 detailed, 2 Lumina HD. */
    public static int sample(double wx,double wz,double blocksPerPixel,boolean high,boolean smooth,int quality){
        if(quality<=AtlasTerrainEngine.CLASSIC)return sample(wx,wz,blocksPerPixel,high,smooth);
        return AtlasTerrainEngine.sample(wx,wz,blocksPerPixel,quality,smooth);
    }
    public static int unknown(int x,int z,boolean grid){
        int base=0xFF1B2839;
        if(grid&&(Math.floorMod(x,8)==0||Math.floorMod(z,8)==0))return 0xFF263247;
        return base;
    }
}
