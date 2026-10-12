package org.chamomilo.wurm.alarmclock;

import java.util.*;

/** One catalogue for built-in identities, parameters, defaults and event presentation. */
public enum AlarmType {
    TIMER(null, Alarm.Kind.TIMER, "timer", "Timer", "Time is up.", Rule.TIMER, 20,
        new Appearance(Chime.TIMER, Alarm.ReminderColor.GOLD, .29f,.23f,.36f,false,false), Activity.NONE),
    FOOD("food", Alarm.Kind.FOOD, "food", "Food", "Eat, you have {percent}% food left.", Rule.PERCENTAGE, 20,
        new Appearance(Chime.FOOD, Alarm.ReminderColor.GOLD, .34f,.22f,.16f,false,false), Activity.NONE),
    WATER("water", Alarm.Kind.WATER, "water", "Water", "Drink, you have {percent}% water left.", Rule.BELOW_PERCENTAGE, 20,
        new Appearance(Chime.WATER, Alarm.ReminderColor.GOLD, .16f,.29f,.35f,false,false), Activity.NONE),
    PRAYER("prayer", Alarm.Kind.TIMER, "prayer", "Prayer", "It's time to pray.", Rule.TIMER, 20,
        new Appearance(Chime.PRAYER, Alarm.ReminderColor.GOLD, .33f,.29f,.16f,false,false), Activity.PRAYER),
    MEDITATION("meditation", Alarm.Kind.TIMER, "meditation", "Meditation", "It's time to meditate.", Rule.TIMER, 30,
        new Appearance(Chime.MEDITATION, Alarm.ReminderColor.LIGHT_BLUE, .22f,.30f,.40f,true,false), Activity.MEDITATION),
    FAVOR("favor", Alarm.Kind.FAVOR, "favor", "Favor", "Favor is full. It's time to cast a spell.", Rule.FULL_FAVOR, 20,
        new Appearance(Chime.FAVOR, Alarm.ReminderColor.MINT, .17f,.34f,.24f,true,true), Activity.NONE);

    public enum Rule { TIMER, PERCENTAGE, BELOW_PERCENTAGE, FULL_FAVOR }
    public enum Activity { NONE, PRAYER, MEDITATION }
    public enum Parameter {
        DELAY("delay", "delay"), REPEAT("repeat", "repeatField"),
        THRESHOLD("threshold", "thresholdField"), STOP("stop", "stopField");
        public final String fieldId, captionKey;
        Parameter(String fieldId, String captionKey) { this.fieldId=fieldId;this.captionKey=captionKey; }
        public String value(Alarm alarm) {
            switch(this) {
                case DELAY: return minutes(alarm.delayMillis);
                case REPEAT: return minutes(alarm.repeatMillis);
                case THRESHOLD: return Integer.toString(alarm.threshold);
                default: return Integer.toString(alarm.stopAt);
            }
        }
    }
    public static final long MAX_DURATION = 365 * 86_400_000L;
    private static final List<AlarmType> EDITABLE = Collections.unmodifiableList(Arrays.asList(TIMER, FOOD, WATER, FAVOR));
    public final String builtInId, nameKey, defaultName, defaultMessage;
    public final Alarm.Kind kind;
    public final Rule rule;
    public final Activity activity;
    public final Appearance appearance;
    public final int defaultThreshold, defaultStop;
    public final long defaultDelayMillis, defaultRepeatMillis;
    private final List<Parameter> parameters;

    AlarmType(String id, Alarm.Kind kind, String nameKey, String name, String message, Rule rule,
              int delayMinutes, Appearance appearance, Activity activity) {
        this.builtInId=id;this.kind=kind;this.nameKey=nameKey;this.defaultName=name;this.defaultMessage=message;
        this.rule=rule;this.appearance=appearance;this.activity=activity;
        defaultDelayMillis=delayMinutes*60_000L;defaultRepeatMillis=60_000L;
        defaultThreshold=rule==Rule.FULL_FAVOR?100:rule==Rule.BELOW_PERCENTAGE?50:20;
        defaultStop=rule==Rule.PERCENTAGE?5:0;
        Parameter[] fields=rule==Rule.TIMER?new Parameter[]{Parameter.DELAY,Parameter.REPEAT}:
            rule==Rule.PERCENTAGE?new Parameter[]{Parameter.THRESHOLD,Parameter.STOP}:
            rule==Rule.BELOW_PERCENTAGE?new Parameter[]{Parameter.THRESHOLD,Parameter.REPEAT}:new Parameter[]{Parameter.REPEAT};
        parameters=Collections.unmodifiableList(Arrays.asList(fields));
    }
    public static List<AlarmType> editable() { return EDITABLE; }
    public static AlarmType forKind(Alarm.Kind kind) {
        for(AlarmType type:EDITABLE) if(type.kind==kind)return type;
        throw new IllegalArgumentException("Unknown alarm kind: "+kind);
    }
    public static AlarmType builtIn(String id) {
        for(AlarmType type:values())if(type.builtInId!=null&&type.builtInId.equals(id))return type;
        return null;
    }
    public static AlarmType forAlarm(Alarm alarm) {
        AlarmType builtIn=builtIn(alarm.id);
        return alarm.builtIn&&builtIn!=null&&builtIn.kind==alarm.kind?builtIn:forKind(alarm.kind);
    }
    public List<Parameter> parameters() { return parameters; }
    public boolean has(Parameter parameter) { return parameters.contains(parameter); }
    public String captionKey(Parameter parameter) {
        if(parameter==Parameter.REPEAT&&rule!=Rule.TIMER)return "favorRepeatField";
        if(parameter==Parameter.THRESHOLD&&rule==Rule.BELOW_PERCENTAGE)return "waterThresholdField";
        return parameter.captionKey;
    }
    public String hintKey() { return rule==Rule.TIMER?"timerHint":rule==Rule.PERCENTAGE?"needsHint":rule==Rule.BELOW_PERCENTAGE?"waterHint":"favorHint"; }
    public void apply(Alarm alarm, Parameter parameter, String value) {
        if(!has(parameter))return;
        switch(parameter) {
            case DELAY: alarm.delayMillis=duration(value,false);break;
            case REPEAT: alarm.repeatMillis=duration(value,rule==Rule.TIMER);break;
            case THRESHOLD: alarm.threshold=Integer.parseInt(value.trim());break;
            case STOP: alarm.stopAt=Integer.parseInt(value.trim());break;
        }
    }
    public void validate(Alarm alarm) {
        if(has(Parameter.THRESHOLD)&&(alarm.threshold<1||alarm.threshold>100))
            throw new IllegalArgumentException("Use a threshold from 1 to 100.");
        if(has(Parameter.STOP)&&(alarm.stopAt<0||alarm.stopAt>alarm.threshold))
            throw new IllegalArgumentException("Use a threshold from 1 to 100 and a stop level from 0 to the threshold.");
        if(has(Parameter.DELAY)&&(alarm.delayMillis<1_000||alarm.delayMillis>MAX_DURATION))
            throw new IllegalArgumentException("Use a positive duration between one second and one year.");
        if(has(Parameter.REPEAT)&&(alarm.repeatMillis<0||alarm.repeatMillis>MAX_DURATION||
            (alarm.repeatMillis>0&&alarm.repeatMillis<1_000)||(rule!=Rule.TIMER&&alarm.repeatMillis==0)))
            throw new IllegalArgumentException("Use a positive repeat interval; only timers support repeat 0.");
    }
    public static List<Alarm> defaults() {
        List<Alarm> alarms=new ArrayList<Alarm>();
        for(AlarmType type:values())if(type.builtInId!=null) {
            Alarm alarm=new Alarm(type.builtInId,type.kind,type.defaultName,type.defaultMessage);
            alarm.builtIn=true;alarms.add(alarm);
        }
        return alarms;
    }
    public static String minutes(long millis) {
        return millis%60_000==0?Long.toString(millis/60_000):Double.toString(millis/60_000.0);
    }
    public static long duration(String value, boolean zeroAllowed) {
        double minutes=Double.parseDouble(value.trim().replace(',','.'));
        if(!Double.isFinite(minutes)||minutes<0||(!zeroAllowed&&minutes==0)||minutes>525600)
            throw new IllegalArgumentException("Invalid minutes");
        long result=Math.round(minutes*60_000);
        if(result<1000&&(!zeroAllowed||minutes!=0))throw new IllegalArgumentException("Minimum duration is one second");
        return result;
    }
    public static final class Appearance {
        public final Chime chime;
        public final Alarm.ReminderColor reminderColor;
        public final float plateRed,plateGreen,plateBlue;
        public final boolean coloredCaption,sharedCardFonts;
        private Appearance(Chime chime,Alarm.ReminderColor color,float red,float green,float blue,boolean caption,boolean fonts) {
            this.chime=chime;reminderColor=color;plateRed=red;plateGreen=green;plateBlue=blue;
            coloredCaption=caption;sharedCardFonts=fonts;
        }
    }
}
