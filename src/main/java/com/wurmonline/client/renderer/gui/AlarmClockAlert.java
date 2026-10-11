package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import org.chamomilo.wurm.alarmclock.*;
import org.chamomilo.wurm.ui.v1.*;
import java.util.*;

/** Temporary click-through HUD alert; keeps rendering when the list window is closed. */
final class AlarmClockAlert extends StaticComponent {
    private final Deque<Alarm> pending=new ArrayDeque<Alarm>();
    private final ChamomiloUiV1Canvas canvas=new ChamomiloUiV1Canvas(this);
    private final ChamomiloUiV1Tag caption=new ChamomiloUiV1Tag("alarmclock.ringing.caption","");
    private Alarm active;
    private long started;
    AlarmClockAlert() { super("Alarm Clock ringing");setSize(300,128);caption.setMaximumWidth(292); }
    void ring(Alarm alarm) {
        for(Alarm queued:pending)if(queued.id.equals(alarm.id))return;
        if(pending.size()<8)pending.addLast(alarm.copy());
    }
    boolean tick(long now) {
        if(active!=null&&now-started>=3000)active=null;
        if(active==null&&!pending.isEmpty()){active=pending.removeFirst();started=now;}
        setPosition(Math.max(0,(SCREEN_WIDTH-width)/2),Math.max(0,SCREEN_HEIGHT/4-height-18));
        if(active!=null) {
            String name=Messages.name(active);if(!caption.value().equals(name))caption.setText(name);
            caption.setPosition(x+(width-caption.width)/2,y+99);
        }
        return active!=null;
    }
    void clear() { active=null;pending.clear(); }
    @Override public StaticComponent getComponentAt(int mx,int my) { return null; }
    @Override protected void renderComponent(Queue queue,float ignoredAlpha) {
        if(active==null)return;
        double elapsed=Math.max(0,System.currentTimeMillis()-started)/1000.0;
        double ease=Math.min(1,elapsed/.15)*Math.min(1,Math.max(0,(3-elapsed)/.3));
        int size=(int)Math.round(64+5*Math.sin(elapsed*5)*ease),cx=x+width/2,cy=y+49;
        Alarm.ReminderColor tint=active.reminderColor();UiColor color=new UiColor(tint.red,tint.green,tint.blue);
        canvas.begin(queue);
        AlarmClockGlyph.paint(canvas,color,cx,cy,size,.14*Math.sin(elapsed*24)*ease);
        int spread=(int)Math.round(7+4*Math.sin(elapsed*8));
        for(int side:new int[]{-1,1})for(int ray=-1;ray<=1;ray++)
            AlarmClockGlyph.stroke(canvas,color,cx+side*(43+spread),cy+ray*18,cx+side*(53+spread),cy+ray*22,2);
        caption.render(queue,1f);
    }
}
