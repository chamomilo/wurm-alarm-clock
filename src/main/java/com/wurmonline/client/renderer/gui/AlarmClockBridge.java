package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.settings.SavePosManager;
import org.chamomilo.wurm.alarmclock.AlarmClockMod;
import org.chamomilo.wurm.alarmclock.AlarmEngine;
import org.chamomilo.wurm.alarmclock.Alarm;
import org.chamomilo.wurm.alarmclock.AlarmSounds;
import org.chamomilo.wurm.alarmclock.Chime;
import org.chamomilo.wurm.alarmclock.Messages;
import java.lang.reflect.*;
import java.util.function.BooleanSupplier;

/** Product HUD registration only; the embedded reference owns all update integration. */
public final class AlarmClockBridge {
    private final HeadsUpDisplay hud;
    private final AlarmClockWindow window;
    private final AlarmClockLauncher launcher;
    private final AlarmClockAlert alert=new AlarmClockAlert();
    private final AlarmSounds sounds;
    private final AlarmEngine engine;
    private boolean alertVisible, soundWasEnabled;
    public AlarmClockBridge(HeadsUpDisplay hud, AlarmClockMod mod) throws ReflectiveOperationException {
        this(hud, mod.engine(), mod::saveFailed);
    }
    AlarmClockBridge(HeadsUpDisplay hud, AlarmEngine engine, BooleanSupplier saveFailed) throws ReflectiveOperationException {
        this.hud = hud;
        this.engine = engine;soundWasEnabled=engine.options().sound;
        sounds=new AlarmSounds(chime->AlarmSounds.play(hud,chime));
        window = new AlarmClockWindow(engine, saveFailed, alarm->sounds.enqueue(Chime.forAlarm(alarm)));
        launcher = new AlarmClockLauncher(this::open, new org.chamomilo.wurm.ui.v1.UiScale(1f));
        launcher.moveToDefault();
        // Native menu toggles bypass show(). Bind the kit's HUD owner before any menu opening.
        window.show(hud);
        window.onClose(() -> { window.disposeEditor(); window.hide(); });
        hud.mainMenu.registerComponent(Messages.t("title"), window);
        hud.hudSettings.registerComponent(Messages.t("launcher"), launcher);
        hud.hideComponent(window);
        Field field = HeadsUpDisplay.class.getDeclaredField("savePosManager"); field.setAccessible(true);
        SavePosManager positions = (SavePosManager) field.get(hud);
        positions.registerAndRefresh(window, "alarmclock.window");
        positions.registerAndRefresh(launcher, "alarmclock.launcher");
        showLauncher(false);
    }
    public void ring(Alarm alarm) {
        if(engine.options().sound)sounds.enqueue(Chime.forAlarm(alarm));
        if(engine.options().animation)alert.ring(alarm);
    }
    public void tick(long now) {
        if(soundWasEnabled&&!engine.options().sound)sounds.clear();
        soundWasEnabled=engine.options().sound;sounds.tick(now);
        if(!engine.options().animation)alert.clear();
        boolean visible=alert.tick(now);
        if(visible!=alertVisible)setAlertVisible(visible);
        alertVisible=visible;
    }
    private void setAlertVisible(boolean visible) {
        // Like the SDK's free HUD tags, join only the render list; preserve native free-look and focus.
        java.util.List<WurmComponent> components=hud.getComponents();
        synchronized(components) { if(visible){if(!components.contains(alert))components.add(alert);}else components.remove(alert); }
    }
    public void stopEffects() { sounds.clear();alert.clear();setAlertVisible(false);alertVisible=false; }
    public void showLauncher(boolean resetPosition) {
        if (resetPosition) launcher.moveToDefault();
        else launcher.ensureVisible();
        hud.showComponent(launcher);
        hud.setActiveWindow(launcher);
    }
    public void open() { window.refresh(); window.show(hud); }
    public void relocalize() {
        window.relocalize();
        for (java.util.Map.Entry<WButton,WurmComponent> entry : hud.mainMenu.getMenuSet())
            if (entry.getValue() == window && !entry.getKey().getLabel().equals(Messages.t("title")))
                entry.getKey().setLabel(Messages.t("title"));
        for (java.util.Map.Entry<WButton,WurmComponent> entry : hud.hudSettings.getMenuSet())
            if (entry.getValue() == launcher && !entry.getKey().getLabel().equals(Messages.t("launcher")))
                entry.getKey().setLabel(Messages.t("launcher"));
    }
    public void dispose() { stopEffects();window.disposeEditor(); hud.removeComponent(window); hud.removeComponent(launcher); }
}
