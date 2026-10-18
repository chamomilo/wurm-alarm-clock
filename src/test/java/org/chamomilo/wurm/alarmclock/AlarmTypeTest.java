package org.chamomilo.wurm.alarmclock;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AlarmTypeTest {
    @Test public void existingBuiltInsKeepIdentityDefaultsAndPresentationAfterRenaming() {
        List<Alarm> alarms=AlarmEngine.defaults();
        AlarmType[] types={AlarmType.FOOD,AlarmType.WATER,AlarmType.PRAYER,AlarmType.MEDITATION,AlarmType.FAVOR};
        Chime[] chimes={Chime.FOOD,Chime.WATER,Chime.PRAYER,Chime.MEDITATION,Chime.FAVOR};
        Set<Integer> colors=new HashSet<Integer>();
        for(int i=0;i<alarms.size();i++) {
            Alarm alarm=alarms.get(i);assertSame(types[i],alarm.type());alarm.validate();
            alarm.name="My alarm";alarm.message="My reminder";assertSame(types[i],alarm.type());
            assertSame(chimes[i],Chime.forAlarm(alarm));assertSame(types[i].appearance.reminderColor,alarm.reminderColor());
            assertTrue("Each built-in has its own default color",colors.add(alarm.reminderColor().rgb()));
        }
        assertTrue("Custom timers have a separate default color",colors.add(Alarm.create(Alarm.Kind.TIMER,"Custom","Reminder").reminderColor().rgb()));
        assertEquals(20,alarms.get(0).threshold);assertEquals(50,alarms.get(1).threshold);
        assertEquals(Arrays.asList(AlarmType.Parameter.THRESHOLD,AlarmType.Parameter.REPEAT),AlarmType.WATER.parameters());
        assertEquals(1_200_000,alarms.get(2).delayMillis);assertEquals(1_800_000,alarms.get(3).delayMillis);
        assertEquals(60_000,alarms.get(4).repeatMillis);
    }
    @Test public void customTimerNamedLikeABuiltInDoesNotGainItsAutomaticRestartOrChime() {
        Alarm custom=Alarm.create(Alarm.Kind.TIMER,"Meditation","It's time to pray.");
        assertSame(AlarmType.TIMER,custom.type());assertFalse(custom.isPrayer());assertFalse(custom.isMeditation());
        assertFalse(custom.autoRestart());assertSame(Chime.TIMER,Chime.forAlarm(custom));
    }
    @Test public void inactiveSettingsDoNotInvalidateFoodWaterFavorOrTimers() {
        for(Alarm.Kind kind:new Alarm.Kind[]{Alarm.Kind.FOOD}) {
            Alarm alarm=Alarm.create(kind,"Monitor","Reminder");alarm.delayMillis=-1;alarm.repeatMillis=-1;alarm.validate();
        }
        Alarm water=Alarm.create(Alarm.Kind.WATER,"Water","Reminder");water.delayMillis=-1;water.stopAt=-100;water.validate();
        Alarm favor=Alarm.create(Alarm.Kind.FAVOR,"Favor","Reminder");
        favor.delayMillis=-1;favor.threshold=-100;favor.stopAt=200;favor.validate();
        Alarm timer=Alarm.create(Alarm.Kind.TIMER,"Timer","Reminder");
        timer.threshold=-1;timer.stopAt=200;timer.repeatMillis=0;timer.validate();
    }
    @Test public void activeParametersRejectInvalidDurationsThresholdsAndOneShotFavor() {
        Alarm food=Alarm.create(Alarm.Kind.FOOD,"Food","Reminder");food.stopAt=21;reject(food);
        food.stopAt=5;food.threshold=101;reject(food);
        Alarm water=Alarm.create(Alarm.Kind.WATER,"Water","Reminder");water.repeatMillis=0;reject(water);
        Alarm favor=Alarm.create(Alarm.Kind.FAVOR,"Favor","Reminder");favor.repeatMillis=0;reject(favor);
        favor.repeatMillis=999;reject(favor);favor.repeatMillis=AlarmType.MAX_DURATION+1;reject(favor);
        Alarm timer=Alarm.create(Alarm.Kind.TIMER,"Timer","Reminder");timer.delayMillis=0;reject(timer);
        timer.delayMillis=1000;timer.repeatMillis=-1;reject(timer);timer.repeatMillis=0;timer.validate();
    }
    @Test public void editorParameterParsingUsesTypeRulesAndPreservesInactiveValues() {
        Alarm food=Alarm.create(Alarm.Kind.FOOD,"Food","Reminder");long delay=food.delayMillis;
        food.type().apply(food,AlarmType.Parameter.DELAY,"ignored");assertEquals(delay,food.delayMillis);
        food.type().apply(food,AlarmType.Parameter.THRESHOLD," 25 ");assertEquals(25,food.threshold);
        Alarm favor=Alarm.create(Alarm.Kind.FAVOR,"Favor","Reminder");
        favor.type().apply(favor,AlarmType.Parameter.REPEAT,"1,5");assertEquals(90_000,favor.repeatMillis);
        try {favor.type().apply(favor,AlarmType.Parameter.REPEAT,"0");fail("Favor cannot be one-shot");}catch(IllegalArgumentException expected){}
    }
    @Test public void localizationOnlyTranslatesTheBuiltInsOwnDefaultName() {
        Messages.select("ru");
        try {
            Alarm food=AlarmEngine.defaults().get(0);assertEquals("Еда",Messages.name(food));
            food.name="Prayer";assertEquals("Prayer",Messages.name(food));
            food.name="My food";assertEquals("My food",Messages.name(food));
        } finally {Messages.select("en");}
    }
    private static void reject(Alarm alarm) {
        try {alarm.validate();fail("Invalid active parameter accepted: "+alarm.kind);}catch(IllegalArgumentException expected){}
    }
}
