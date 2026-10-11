package org.chamomilo.wurm.alarmclock;

import java.util.UUID;

/** Saved alarm settings. Scheduling and sampling state belong to AlarmEngine. */
public final class Alarm {
    public enum Kind { FOOD, WATER, TIMER, FAVOR }
    public enum ReminderColor {
        GOLD(1f, .94f, .60f), LIGHT_BLUE(.55f, .85f, 1f), MINT(.60f, .93f, .72f);
        public final float red, green, blue;
        ReminderColor(float red, float green, float blue) { this.red=red; this.green=green; this.blue=blue; }
    }
    public final String id;
    public Kind kind;
    public String name;
    public String message;
    public boolean enabled = true;
    public boolean builtIn;
    public boolean autoRestartAfterPrayer;
    public boolean autoRestartAfterMeditation;
    public int threshold;
    public int stopAt;
    public long delayMillis;
    public long repeatMillis;

    public Alarm(String id, Kind kind, String name, String message) {
        this.id = id; this.kind = kind; this.name = name; this.message = message;
        AlarmType reserved=AlarmType.builtIn(id);
        AlarmType type=reserved!=null&&reserved.kind==kind?reserved:AlarmType.forKind(kind);
        threshold=type.defaultThreshold;stopAt=type.defaultStop;
        delayMillis=type.defaultDelayMillis;repeatMillis=type.defaultRepeatMillis;
        autoRestartAfterPrayer=type.activity==AlarmType.Activity.PRAYER;
        autoRestartAfterMeditation=type.activity==AlarmType.Activity.MEDITATION;
    }
    public static Alarm create(Kind kind, String name, String message) {
        return new Alarm(UUID.randomUUID().toString(), kind, name, message);
    }
    public Alarm copy() {
        Alarm copy = new Alarm(id, kind, name, message);
        copy.enabled = enabled; copy.builtIn = builtIn;
        copy.autoRestartAfterPrayer = autoRestartAfterPrayer;
        copy.autoRestartAfterMeditation = autoRestartAfterMeditation;
        copy.threshold = threshold; copy.stopAt = stopAt;
        copy.delayMillis = delayMillis; copy.repeatMillis = repeatMillis;
        return copy;
    }
    public AlarmType type() { return AlarmType.forAlarm(this); }
    public boolean isPrayer() { return type().activity==AlarmType.Activity.PRAYER; }
    public boolean isMeditation() { return type().activity==AlarmType.Activity.MEDITATION; }
    public boolean autoRestart() { return isPrayer() ? autoRestartAfterPrayer : isMeditation() && autoRestartAfterMeditation; }
    public ReminderColor reminderColor() {
        return type().appearance.reminderColor;
    }
    public void validate() {
        if (id == null || id.isEmpty() || kind == null || name == null || name.trim().isEmpty()
                || name.length() > 80 || message == null || message.trim().isEmpty() || message.length() > 180)
            throw new IllegalArgumentException("Name and message are required (80 / 180 characters maximum).");
        type().validate(this);
    }
}
