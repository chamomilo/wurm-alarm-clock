package org.chamomilo.wurm.alarmclock;

import java.util.UUID;

/** Saved alarm settings. Scheduling and sampling state belong to AlarmEngine. */
public final class Alarm {
    public enum Kind { FOOD, WATER, TIMER, FAVOR }
    public enum Delivery {
        EVENT_MESSAGE("eventMessage"), ON_SCREEN_MESSAGE("onScreenMessage"), ANIMATION("alarmAnimation"), CHIME("chime");
        public final String key;
        Delivery(String key) { this.key=key; }
    }
    public static final class ReminderColor {
        public static final ReminderColor GOLD=new ReminderColor(1f,.94f,.60f),
            LIGHT_BLUE=new ReminderColor(.55f,.85f,1f), MINT=new ReminderColor(.60f,.93f,.72f),
            PINK=fromRgb(0xFFABCC), ORANGE=fromRgb(0xFFBD80), LAVENDER=fromRgb(0xC5ADFF);
        public final float red, green, blue;
        ReminderColor(float red, float green, float blue) { this.red=red; this.green=green; this.blue=blue; }
        public int rgb() { return Math.round(red*255)<<16 | Math.round(green*255)<<8 | Math.round(blue*255); }
        public static ReminderColor fromRgb(int rgb) { return new ReminderColor((rgb>>16&255)/255f,(rgb>>8&255)/255f,(rgb&255)/255f); }
    }
    public final String id;
    public Kind kind;
    public String name;
    public String message;
    public boolean enabled = true;
    public boolean builtIn;
    public boolean eventMessage=true, onScreenMessage=true, animation=true, chime=true;
    /** Null follows the type's default tint. */
    public Integer colorRgb;
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
        copy.eventMessage=eventMessage;copy.onScreenMessage=onScreenMessage;copy.animation=animation;copy.chime=chime;copy.colorRgb=colorRgb;
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
        return colorRgb==null?type().appearance.reminderColor:ReminderColor.fromRgb(colorRgb);
    }
    public String colorHex() { return String.format(java.util.Locale.ROOT,"#%06X",reminderColor().rgb()); }
    public void setColor(String value) {
        String clean=value.trim();
        if(!clean.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException("Use #RRGGBB.");
        colorRgb=Integer.parseInt(clean.substring(1),16);
    }
    public boolean delivers(Delivery delivery) {
        switch(delivery) {case EVENT_MESSAGE:return eventMessage;case ON_SCREEN_MESSAGE:return onScreenMessage;case ANIMATION:return animation;default:return chime;}
    }
    public void setDelivery(Delivery delivery,boolean enabled) {
        switch(delivery) {case EVENT_MESSAGE:eventMessage=enabled;break;case ON_SCREEN_MESSAGE:onScreenMessage=enabled;break;case ANIMATION:animation=enabled;break;default:chime=enabled;}
    }
    public void validate() {
        if (id == null || id.isEmpty() || kind == null || name == null || name.trim().isEmpty()
                || name.length() > 80 || message == null || message.trim().isEmpty() || message.length() > 180)
            throw new IllegalArgumentException("Name and message are required (80 / 180 characters maximum).");
        type().validate(this);
        if(colorRgb!=null&&(colorRgb<0||colorRgb>0xFFFFFF))throw new IllegalArgumentException("Invalid reminder color.");
    }
}
