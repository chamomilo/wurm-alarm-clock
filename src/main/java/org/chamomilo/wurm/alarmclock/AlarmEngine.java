package org.chamomilo.wurm.alarmclock;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** HUD-thread scheduler. No background actions, server commands or catch-up message bursts. */
public final class AlarmEngine {
    private final List<Alarm> alarms = new ArrayList<Alarm>();
    private final Map<String,AlarmRuntimeState> states=new HashMap<String,AlarmRuntimeState>();
    private final BiConsumer<Alarm, String> notify;
    private final Runnable save;
    private final ReminderOptions options;
    private Consumer<Alarm> cancelReminder=alarm->{};
    private Consumer<Alarm> presentationChanged=alarm->{};
    private float food = Float.NaN, water = Float.NaN, favor = Float.NaN, faith = Float.NaN;
    public AlarmEngine(List<Alarm> definitions, BiConsumer<Alarm, String> notify, Runnable save) {
        this(definitions,notify,save,new ReminderOptions());
    }
    public AlarmEngine(List<Alarm> definitions, BiConsumer<Alarm, String> notify, Runnable save, ReminderOptions options) {
        this(definitions,notify,save,options,Collections.emptyMap());
    }
    public AlarmEngine(List<Alarm> definitions, BiConsumer<Alarm, String> notify, Runnable save, ReminderOptions options, Map<String,Long> deadlines) {
        alarms.addAll(definitions); this.notify = notify; this.save = save;
        this.options=options;
        for(Alarm alarm:definitions) {
            AlarmRuntimeState state=new AlarmRuntimeState();
            if(alarm.type().has(AlarmType.Parameter.DELAY))state.dueAt=deadlines.getOrDefault(alarm.id,0L);
            if(state.dueAt<0)throw new IllegalArgumentException("Invalid timer deadline.");
            if(states.put(alarm.id,state)!=null)throw new IllegalArgumentException("Duplicate alarm ID");
        }
    }
    public ReminderOptions options() { return options; }
    /** The current HUD removes already delivered effects when the user disables an alarm. */
    public void onReminderCancelled(Consumer<Alarm> listener) { cancelReminder=Objects.requireNonNull(listener); }
    public void onPresentationChanged(Consumer<Alarm> listener) { presentationChanged=Objects.requireNonNull(listener); }
    public void toggleDelivery(Alarm alarm,Alarm.Delivery delivery) {
        alarm.setDelivery(delivery,!alarm.delivers(delivery));presentationChanged.accept(alarm);save.run();
    }
    public void setColor(Alarm alarm,String color) { alarm.setColor(color);presentationChanged.accept(alarm);save.run(); }
    public void toggleSound() { options.sound=!options.sound;save.run(); }
    public void toggleAnimation() { options.animation=!options.animation;save.run(); }
    public List<Alarm> alarms() { return Collections.unmodifiableList(alarms); }
    public long dueAt(Alarm alarm) { return state(alarm).dueAt; }
    long nextReminderAt(Alarm alarm) { return state(alarm).nextReminder; }
    private AlarmRuntimeState state(Alarm alarm) {
        AlarmRuntimeState state=states.get(alarm.id);
        if(state==null)throw new IllegalArgumentException("Alarm is not registered: "+alarm.id);
        return state;
    }
    public Map<String,Long> timerDeadlines() {
        Map<String,Long> deadlines=new HashMap<String,Long>();
        for(Alarm alarm:alarms)if(alarm.type().has(AlarmType.Parameter.DELAY))deadlines.put(alarm.id,dueAt(alarm));
        return Collections.unmodifiableMap(deadlines);
    }
    public float food() { return food; }
    public float water() { return water; }
    public float favor() { return favor; }
    public float faith() { return faith; }
    public boolean favorKnown() { return Float.isFinite(favor) && favor >= 0 && Float.isFinite(faith) && faith > 0; }
    public boolean favorFull() { return favorKnown() && favor >= faith; }
    public void tick(long now, boolean connected, float food, float water) {
        tick(now, connected, food, water, Float.NaN, Float.NaN);
    }
    public void tick(long now, boolean connected, float food, float water, float favor, float faith) {
        this.food = connected ? food : Float.NaN;
        this.water = connected ? water : Float.NaN;
        this.favor = connected ? favor : Float.NaN;
        this.faith = connected ? faith : Float.NaN;
        if (!connected) {
            for (Alarm alarm : alarms) if (alarm.type().rule == AlarmType.Rule.FULL_FAVOR||alarm.type().rule == AlarmType.Rule.BELOW_PERCENTAGE) state(alarm).nextReminder = 0;
            return;
        }
        boolean changed = false;
        for (Alarm alarm : alarms) {
            if (!alarm.enabled) continue;
            AlarmRuntimeState state=state(alarm);
            if (alarm.type().rule == AlarmType.Rule.FULL_FAVOR) {
                if (!favorFull()) { state.nextReminder = 0; continue; }
                if (now < state.nextReminder) continue;
                notify.accept(alarm, alarm.message);
                state.nextReminder = now + alarm.repeatMillis;
            } else if (alarm.type().rule == AlarmType.Rule.BELOW_PERCENTAGE) {
                if(!Float.isFinite(water)||water<0||water>1||water*100.0>=alarm.threshold) {state.nextReminder=0;continue;}
                if(now<state.nextReminder)continue;
                notify.accept(alarm,alarm.message.replace("{percent}",Integer.toString(percentage(water))));
                state.nextReminder=now+alarm.repeatMillis;
            } else if (alarm.type().rule == AlarmType.Rule.PERCENTAGE) {
                threshold(alarm, state, alarm.kind == Alarm.Kind.FOOD ? food : water);
            } else {
                if (state.dueAt == 0) { restartWithoutSave(alarm, now); changed = true; }
                if (now < state.dueAt || now < state.nextReminder) continue;
                notify.accept(alarm, alarm.message);
                if (alarm.repeatMillis == 0) alarm.enabled = false;
                else state.nextReminder = now + alarm.repeatMillis;
                changed |= alarm.repeatMillis == 0;
            }
        }
        if (changed) save.run();
    }
    private void threshold(Alarm alarm, AlarmRuntimeState state, float remaining) {
        if (!Float.isFinite(remaining) || remaining < 0 || remaining > 1) return;
        double value = remaining * 100.0;
        int percent = percentage(remaining);
        if (value > alarm.threshold + .0001) { state.lastPercent = 101; return; }
        if (percent > state.lastPercent) { state.lastPercent = percent; return; }
        if (percent < alarm.stopAt) { state.lastPercent = percent; return; }
        if (percent < state.lastPercent) {
            state.lastPercent = percent;
            notify.accept(alarm, alarm.message.replace("{percent}", Integer.toString(percent)));
        }
    }
    public static int percentage(float remaining) {
        return Math.max(0, Math.min(100, (int) Math.floor(remaining * 100.0 + .0001)));
    }
    /** Wurm exposes hunger/thirst consumed fractions, the opposite of the remaining HUD bars. */
    public static float remaining(float consumed) {
        return Float.isFinite(consumed) && consumed >= 0 && consumed <= 1 ? 1f - consumed : Float.NaN;
    }
    public void resetSamples() {
        food = water = favor = faith = Float.NaN;
        for (Alarm alarm : alarms) {
            state(alarm).lastPercent = 101;
            if (alarm.type().rule == AlarmType.Rule.FULL_FAVOR||alarm.type().rule == AlarmType.Rule.BELOW_PERCENTAGE) state(alarm).nextReminder = 0;
        }
    }
    public void restart(Alarm alarm, long now) { restartWithoutSave(alarm, now); save.run(); }
    /** Successful server-confirmed prayer starts a new interval without re-enabling disabled alarms. */
    public void prayerCompleted(long now) {
        activityCompleted(now, true);
    }
    /** Only a successfully finished meditation starts the next interval. */
    public void meditationCompleted(long now) {
        activityCompleted(now, false);
    }
    private void activityCompleted(long now, boolean prayer) {
        boolean changed = false;
        for (Alarm alarm : alarms) if ((prayer ? alarm.isPrayer() : alarm.isMeditation()) && alarm.enabled && alarm.autoRestart()) {
            restartWithoutSave(alarm, now); changed = true;
        }
        if (changed) save.run();
    }
    public void togglePrayerAutoRestart(Alarm alarm) {
        if (alarm.isPrayer()) toggleAutoRestart(alarm);
    }
    public void toggleAutoRestart(Alarm alarm) {
        if (alarm.isPrayer()) alarm.autoRestartAfterPrayer = !alarm.autoRestartAfterPrayer;
        else if (alarm.isMeditation()) alarm.autoRestartAfterMeditation = !alarm.autoRestartAfterMeditation;
        else return;
        save.run();
    }
    private void restartWithoutSave(Alarm alarm, long now) {
        AlarmRuntimeState state=state(alarm);
        alarm.enabled = true; state.dueAt = now + alarm.delayMillis; state.nextReminder = 0; state.lastPercent = 101;
    }
    public void toggle(Alarm alarm, long now) {
        AlarmRuntimeState state=state(alarm);state.nextReminder = 0;
        if (alarm.enabled) { alarm.enabled = false; cancelReminder.accept(alarm); }
        else { alarm.enabled = true; if (alarm.type().has(AlarmType.Parameter.DELAY)) restartWithoutSave(alarm, now); else state.lastPercent = 101; }
        save.run();
    }
    public void put(Alarm edited, long now) {
        edited.validate();
        for (int i = 0; i < alarms.size(); i++) if (alarms.get(i).id.equals(edited.id)) {
            // Settings may replace the live alarm while an older editor remains open.
            // Monitor edits do not change the user's current enable/disable choice.
            if(!edited.type().has(AlarmType.Parameter.DELAY))edited.enabled=alarms.get(i).enabled;
            states.put(edited.id,new AlarmRuntimeState());
            if(edited.type().has(AlarmType.Parameter.DELAY))restartWithoutSave(edited,now);
            alarms.set(i, edited); presentationChanged.accept(edited);save.run(); return;
        }
        if (alarms.size() >= 100) throw new IllegalArgumentException("At most 100 alarms can be saved.");
        states.put(edited.id,new AlarmRuntimeState());
        if(edited.type().has(AlarmType.Parameter.DELAY))restartWithoutSave(edited,now);
        alarms.add(edited); save.run();
    }
    /** Changing captions or reminder intervals keeps a running timer's deadline. */
    public void updateSettings(Alarm edited,String field,long now) {
        edited.validate();
        for(int i=0;i<alarms.size();i++)if(alarms.get(i).id.equals(edited.id)) {
            Alarm previous=alarms.get(i);AlarmRuntimeState state=state(previous);
            if(field.equals("delay") || (field.equals("enabled")&&edited.enabled&&!previous.enabled)) {
                if(edited.type().has(AlarmType.Parameter.DELAY))state.dueAt=now+edited.delayMillis;
            }
            if(field.equals("enabled")&&!edited.enabled)cancelReminder.accept(previous);
            boolean presentation=field.equals("color");
            for(Alarm.Delivery delivery:Alarm.Delivery.values())presentation|=field.equals(delivery.key);
            if(!field.equals("name")&&!field.equals("message")&&!presentation) {state.nextReminder=0;state.lastPercent=101;}
            alarms.set(i,edited);if(presentation)presentationChanged.accept(edited);save.run();return;
        }
        throw new IllegalArgumentException("Alarm was removed");
    }
    public void remove(Alarm alarm) {
        if (!alarm.builtIn && alarms.remove(alarm)) { states.remove(alarm.id);cancelReminder.accept(alarm);save.run(); }
    }
    public static List<Alarm> defaults() { return AlarmType.defaults(); }
}
