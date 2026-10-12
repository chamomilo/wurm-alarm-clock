package org.chamomilo.wurm.alarmclock;

import org.chamomilo.wurm.update.*;
import java.util.*;

/** Exposes presentation and every existing alarm's parameters to the common editor. */
final class AlarmModSettings implements SharedModSettings.Provider {
    private final AlarmClockMod mod;
    AlarmModSettings(AlarmClockMod mod) { this.mod=mod; }
    public List<ModSetting> settings() {
        List<ModSetting> specs=new ArrayList<ModSetting>();
        specs.add(ModSetting.bool("Presentation","sound","Reminder sound","Play a chime when an alarm fires; written reminders remain enabled.",true,true));
        specs.add(ModSetting.bool("Presentation","animation","Reminder animation","Show animated reminder effects; written reminders remain enabled.",true,true));
        for(Alarm alarm:mod.engine().alarms()) {
            String prefix=alarm.id+"/",group=alarm.name;
            specs.add(ModSetting.bool(group,prefix+"enabled","Enabled","Enable or disable this alarm. Enabling a timer begins a new countdown.",alarm.enabled,true));
            specs.add(ModSetting.text(group,prefix+"name","Name","Alarm name displayed in the alarm list; 1 to 80 characters.",alarm.name,true));
            specs.add(ModSetting.text(group,prefix+"message","Message","Reminder text; 1 to 180 characters. {percent} inserts the remaining food or water percentage.",alarm.message,true));
            for(AlarmType.Parameter p:alarm.type().parameters()) {
                boolean percentage=p==AlarmType.Parameter.THRESHOLD||p==AlarmType.Parameter.STOP;
                String label=p==AlarmType.Parameter.DELAY?"Delay (minutes)":p==AlarmType.Parameter.REPEAT?"Repeat (minutes)":p==AlarmType.Parameter.THRESHOLD?"Alert threshold (%)":"Stop below (%)";
                String help=p==AlarmType.Parameter.DELAY?"Countdown length. Editing the delay starts a fresh countdown.":p==AlarmType.Parameter.REPEAT?"Minutes between reminders. Only timer alarms allow zero for a single reminder.":p==AlarmType.Parameter.THRESHOLD?"Begin reminders at or below this percentage; must be at least the stop level.":"Stop reminders below this percentage; cannot exceed the alert threshold.";
                if(p==AlarmType.Parameter.THRESHOLD&&alarm.kind==Alarm.Kind.WATER) {
                    label="Alert below (%)";
                    help="Repeat reminders while water is strictly below this percentage. There is no lower cutoff.";
                }
                double min=p==AlarmType.Parameter.THRESHOLD?1:p==AlarmType.Parameter.STOP?0:p==AlarmType.Parameter.REPEAT&&alarm.type().rule==AlarmType.Rule.TIMER?0:1.0/60;
                specs.add(ModSetting.number(group,prefix+p.fieldId,label,help,Double.parseDouble(p.value(alarm)),min,percentage?100:525600,percentage,true));
            }
            if(alarm.isPrayer()||alarm.isMeditation())specs.add(ModSetting.bool(group,prefix+"autoRestart","Restart after activity",
                "Start a new countdown after your character successfully completes this activity.",alarm.autoRestart(),true));
        }
        return specs;
    }
    private Alarm find(String id) { for(Alarm alarm:mod.engine().alarms())if(alarm.id.equals(id))return alarm;throw new IllegalArgumentException("Alarm was removed; reopen Settings."); }
    public String value(String key) {
        if(key.equals("sound"))return Boolean.toString(mod.engine().options().sound);
        if(key.equals("animation"))return Boolean.toString(mod.engine().options().animation);
        int split=key.lastIndexOf('/');Alarm alarm=find(key.substring(0,split));String field=key.substring(split+1);
        if(field.equals("name"))return alarm.name;if(field.equals("message"))return alarm.message;
        if(field.equals("enabled"))return Boolean.toString(alarm.enabled);
        if(field.equals("autoRestart"))return Boolean.toString(alarm.autoRestart());
        for(AlarmType.Parameter p:alarm.type().parameters())if(p.fieldId.equals(field))return p.value(alarm);
        throw new IllegalArgumentException(field);
    }
    public void change(String key,String value) {
        ModSetting spec=null;for(ModSetting candidate:settings())if(candidate.key.equals(key))spec=candidate;
        if(spec==null)throw new IllegalArgumentException(key);value=spec.validate(value);
        AlarmEngine engine=mod.engine();
        if(key.equals("sound")||key.equals("animation")) {
            boolean old=key.equals("sound")?engine.options().sound:engine.options().animation;
            boolean next=Boolean.parseBoolean(value);
            if(key.equals("sound"))engine.options().sound=next;else engine.options().animation=next;
            mod.save();
            if(mod.saveFailed()) {if(key.equals("sound"))engine.options().sound=old;else engine.options().animation=old;throw new IllegalStateException("Cannot save reminder options");}
            mod.refreshSettingsUi();
            return;
        }
        int split=key.lastIndexOf('/');Alarm original=find(key.substring(0,split));Alarm edited=original.copy();String field=key.substring(split+1);
        if(field.equals("name"))edited.name=value;else if(field.equals("message"))edited.message=value;
        else if(field.equals("enabled"))edited.enabled=Boolean.parseBoolean(value);
        else if(field.equals("autoRestart")){edited.autoRestartAfterPrayer=edited.isPrayer()&&Boolean.parseBoolean(value);edited.autoRestartAfterMeditation=edited.isMeditation()&&Boolean.parseBoolean(value);}
        else for(AlarmType.Parameter p:edited.type().parameters())if(p.fieldId.equals(field))edited.type().apply(edited,p,value);
        edited.validate();
        engine.updateSettings(edited,field,System.currentTimeMillis());
        if(mod.saveFailed()){engine.updateSettings(original,field,System.currentTimeMillis());throw new IllegalStateException("Cannot save alarm settings");}
        mod.refreshSettingsUi();
    }
}
