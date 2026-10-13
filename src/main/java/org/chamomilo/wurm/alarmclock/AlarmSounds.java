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
    private final Deque<PendingSound> pending=new ArrayDeque<PendingSound>();
    private final Consumer<Chime> player;
    private long nextAt;
    private boolean failureLogged;
    public AlarmSounds(Consumer<Chime> player) { this.player=player; }
    /** Explicit sound previews remain available even for disabled alarms. */
    public void enqueue(Chime chime) { enqueue(null,chime); }
    public void enqueue(Alarm alarm) { enqueue(alarm.id,Chime.forAlarm(alarm)); }
    private void enqueue(String alarmId,Chime chime) {
        for(PendingSound queued:pending)
            if(Objects.equals(queued.alarmId,alarmId)&&queued.chime==chime)return;
        pending.addLast(new PendingSound(alarmId,chime));
    }
    public void cancel(String alarmId) { pending.removeIf(queued->alarmId.equals(queued.alarmId)); }
    public void tick(long now) {
        if(pending.isEmpty()||now<nextAt)return;
        Chime chime=pending.removeFirst().chime;nextAt=now+chime.durationMillis()+250;
        try { player.accept(chime); }
        catch(RuntimeException failure) { if(!failureLogged){LOG.log(Level.WARNING,"Cannot play alarm chime",failure);failureLogged=true;} }
    }
    public void clear() { pending.clear();nextAt=0; }
    private static final class PendingSound {
        final String alarmId;
        final Chime chime;
        PendingSound(String alarmId,Chime chime) { this.alarmId=alarmId;this.chime=chime; }
    }
    public static void play(HeadsUpDisplay hud,Chime chime) {
        if(hud.getWorld()==null)return;
        SoundEngine<?> sound=hud.getWorld().getSoundEngine();
        if(sound!=null&&SoundEngine.getPlayerPosition()!=null)
            sound.play(new AlarmChimeUrl(chime),SoundEngine.getPlayerPosition(),.75f,1f,1f,true,false);
    }
}
