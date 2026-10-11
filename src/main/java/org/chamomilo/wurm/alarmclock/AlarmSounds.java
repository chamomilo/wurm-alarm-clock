package org.chamomilo.wurm.alarmclock;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.resources.AlarmChimeUrl;
import com.wurmonline.client.sound.SoundEngine;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.*;

/** Serialize short cues so simultaneous alarms remain recognizable, rather than overlapping. */
public final class AlarmSounds {
    private static final Logger LOG=Logger.getLogger(AlarmSounds.class.getName());
    private final Deque<Chime> pending=new ArrayDeque<Chime>();
    private final Consumer<Chime> player;
    private long nextAt;
    private boolean failureLogged;
    public AlarmSounds(Consumer<Chime> player) { this.player=player; }
    public void enqueue(Chime chime) { if(pending.size()<8&&!pending.contains(chime))pending.addLast(chime); }
    public void tick(long now) {
        if(pending.isEmpty()||now<nextAt)return;
        Chime chime=pending.removeFirst();nextAt=now+chime.durationMillis()+250;
        try { player.accept(chime); }
        catch(RuntimeException failure) { if(!failureLogged){LOG.log(Level.WARNING,"Cannot play alarm chime",failure);failureLogged=true;} }
    }
    public void clear() { pending.clear();nextAt=0; }
    public static void play(HeadsUpDisplay hud,Chime chime) {
        if(hud.getWorld()==null)return;
        SoundEngine<?> sound=hud.getWorld().getSoundEngine();
        if(sound!=null&&SoundEngine.getPlayerPosition()!=null)
            sound.play(new AlarmChimeUrl(chime),SoundEngine.getPlayerPosition(),.75f,1f,1f,true,false);
    }
}
