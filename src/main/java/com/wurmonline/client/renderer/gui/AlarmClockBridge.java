package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
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
    private final AlarmClockScreenMessages messages=new AlarmClockScreenMessages();
    private final AlarmSounds sounds;
    private final AlarmEngine engine;
    private boolean alertVisible;
    public AlarmClockBridge(HeadsUpDisplay hud, AlarmClockMod mod) throws ReflectiveOperationException {
        this(hud, mod.engine(), mod::saveFailed);
    }
    AlarmClockBridge(HeadsUpDisplay hud, AlarmEngine engine, BooleanSupplier saveFailed) throws ReflectiveOperationException {
        this.hud = hud;
        this.engine = engine;
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
        java.util.List<WurmComponent> components=hud.getComponents();
        synchronized(components) { components.add(messages); }
        showLauncher(false);
        engine.onReminderCancelled(this::cancelReminder);
        engine.onPresentationChanged(this::syncPresentation);
    }
    /** Reminder decorations render in a final pass; native picking still skips these layers. */
    public static boolean isReminderOverlay(WurmComponent component) {
        return component instanceof AlarmClockScreenMessages || component instanceof AlarmClockAlert;
    }
    public static void renderReminderOverlays(HeadsUpDisplay hud, Queue queue, float alpha) {
        java.util.List<WurmComponent> components=hud.getComponents();
        synchronized(components) {
            for(WurmComponent component:components)
                if(isReminderOverlay(component))component.render(queue,alpha);
        }
    }
    public void remind(Alarm alarm, String message) {
        if(alarm.onScreenMessage)messages.show(alarm,message);
        if(alarm.eventMessage) {
            Alarm.ReminderColor color=alarm.reminderColor();
            hud.textMessage(":Event",color.red*255f,color.green*255f,color.blue*255f,message);
        }
        ring(alarm);
    }
    public void ring(Alarm alarm) {
        if(alarm.chime)sounds.enqueue(alarm);
        if(alarm.animation)alert.ring(alarm);
    }
    public void tick(long now) {
        sounds.tick(now);
        boolean visible=alert.tick(now);
        if(visible!=alertVisible)setAlertVisible(visible);
        alertVisible=visible;
    }
    private void syncPresentation(Alarm alarm) {
        if(!alarm.onScreenMessage)messages.cancel(alarm.id);
        if(!alarm.chime)sounds.cancel(alarm.id);
        if(!alarm.animation)alert.cancel(alarm.id);
        alertVisible=alert.tick(System.currentTimeMillis());setAlertVisible(alertVisible);
    }
    private void setAlertVisible(boolean visible) {
        // Register for rendering without changing native window/menu/focus state.
        // The decorative overlays opt out of input through isAvailable().
        java.util.List<WurmComponent> components=hud.getComponents();
        synchronized(components) { if(visible){if(!components.contains(alert))components.add(alert);}else components.remove(alert); }
    }
    private void cancelReminder(Alarm alarm) {
        messages.cancel(alarm.id);sounds.cancel(alarm.id);alert.cancel(alarm.id);
        alertVisible=alert.tick(System.currentTimeMillis());setAlertVisible(alertVisible);
    }
    public void stopEffects() { sounds.clear();alert.clear();messages.clear();setAlertVisible(false);alertVisible=false; }
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
    public void dispose() {
        engine.onReminderCancelled(alarm->{});
        engine.onPresentationChanged(alarm->{});
        stopEffects();window.disposeEditor();hud.removeComponent(window);hud.removeComponent(launcher);
        java.util.List<WurmComponent> components=hud.getComponents();
        synchronized(components) { components.remove(messages); }
    }
}
