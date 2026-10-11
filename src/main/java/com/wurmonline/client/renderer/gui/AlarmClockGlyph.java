package com.wurmonline.client.renderer.gui;

import org.chamomilo.wurm.ui.v1.*;

/** One vector clock for the launcher and ringing overlay, with optional smooth rotation. */
final class AlarmClockGlyph {
    private static final int[][] LINES={{7,4,17,4},{4,7,4,17},{20,7,20,17},{7,20,17,20},
        {4,7,7,4},{17,4,20,7},{4,17,7,20},{17,20,20,17},
        {12,7,12,12},{12,12,16,14},{2,3,6,0},{18,0,22,3},{7,21,5,24},{17,21,19,24}};
    private AlarmClockGlyph() { }
    static void paint(UiCanvas canvas,UiColor color,int cx,int cy,int size,double rotation) {
        double factor=size/24.0, c=Math.cos(rotation),s=Math.sin(rotation);
        int stroke=Math.max(1,(int)Math.round(size/24.0));
        for(int[] line:LINES) {
            int x0=cx+(int)Math.round(((line[0]-12)*c-(line[1]-12)*s)*factor);
            int y0=cy+(int)Math.round(((line[0]-12)*s+(line[1]-12)*c)*factor);
            int x1=cx+(int)Math.round(((line[2]-12)*c-(line[3]-12)*s)*factor);
            int y1=cy+(int)Math.round(((line[2]-12)*s+(line[3]-12)*c)*factor);
            stroke(canvas,color,x0,y0,x1,y1,stroke);
        }
    }
    static void stroke(UiCanvas canvas,UiColor color,int x0,int y0,int x1,int y1,int thickness) {
        int dx=Math.abs(x1-x0),dy=-Math.abs(y1-y0),sx=x0<x1?1:-1,sy=y0<y1?1:-1,error=dx+dy;
        while(true) {
            canvas.fill(color,1f,x0-thickness/2,y0-thickness/2,thickness,thickness);
            if(x0==x1&&y0==y1)break;
            int e=2*error;if(e>=dy){error+=dy;x0+=sx;}if(e<=dx){error+=dx;y0+=sy;}
        }
    }
}
