package org.chamomilo.wurm.alarmclock;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AlarmEngineTest {
    @Test public void settingsEditsKeepTimerDeadlineExceptForAnExplicitDelayOrReenable() {
        Alarm timer=Alarm.create(Alarm.Kind.TIMER,"Test","Message");
        AlarmEngine engine=new AlarmEngine(Arrays.asList(timer),(a,m)->{},()->{});
        engine.restart(timer,1000);long due=engine.dueAt(timer);
        Alarm edited=timer.copy();edited.name="Renamed";engine.updateSettings(edited,"name",2000);
        assertEquals(due,engine.dueAt(edited));
        Alarm repeated=edited.copy();repeated.repeatMillis=120000;engine.updateSettings(repeated,"repeat",3000);
        assertEquals(due,engine.dueAt(repeated));
        Alarm disabled=repeated.copy();disabled.enabled=false;engine.updateSettings(disabled,"enabled",4000);
        assertFalse(engine.alarms().get(0).enabled);assertEquals(due,engine.dueAt(disabled));
        Alarm enabled=disabled.copy();enabled.enabled=true;engine.updateSettings(enabled,"enabled",5000);
        assertEquals(5000+enabled.delayMillis,engine.dueAt(enabled));
        Alarm delayed=enabled.copy();delayed.delayMillis=60000;engine.updateSettings(delayed,"delay",6000);
        assertEquals(66000,engine.dueAt(delayed));
    }
    @Test public void foodTriggersAtEveryWholePercentThroughFive() {
        List<String> messages=new ArrayList<String>();
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->messages.add(m),()->{});
        engine.alarms().get(2).enabled=false;
        engine.tick(1000,true,.205f,.8f); assertTrue(messages.isEmpty());
        for(int percent=20;percent>=5;percent--) {
            engine.tick(2000,true,percent/100f,.8f);
            engine.tick(3000,true,percent/100f,.8f); // unchanged samples never repeat
        }
        assertEquals(16,messages.size());
        assertEquals("Eat, you have 20% food left.",messages.get(0));
        assertEquals("Eat, you have 5% food left.",messages.get(15));
        engine.tick(4000,true,.04f,.8f);assertEquals(16,messages.size());
    }
    @Test public void waterRepeatsEveryMinuteStrictlyBelowFiftyWithoutALowerCutoff() {
        Alarm water=AlarmEngine.defaults().get(1);water.stopAt=100;
        List<String> messages=new ArrayList<String>();
        AlarmEngine engine=new AlarmEngine(Collections.singletonList(water),(a,m)->messages.add(m),()->fail("Samples must not save settings"));
        engine.tick(1000,true,1,.5f);assertTrue(messages.isEmpty());
        engine.tick(1250,true,1,.49f);assertEquals(Arrays.asList("Drink, you have 49% water left."),messages);
        engine.tick(61_249,true,1,.2f);assertEquals(1,messages.size());
        engine.tick(61_250,true,1,.2f);assertEquals(2,messages.size());
        engine.tick(121_250,true,1,0);assertEquals("Drink, you have 0% water left.",messages.get(2));
        engine.tick(900_000,true,1,0);assertEquals(4,messages.size());
        engine.tick(900_250,true,1,0);assertEquals(4,messages.size());
        engine.tick(901_000,true,1,.5f);engine.tick(901_250,true,1,.49f);assertEquals(5,messages.size());
    }
    @Test public void waterDisableReconnectAndInvalidSamplesKeepRepeatsControlled() {
        Alarm water=AlarmEngine.defaults().get(1);List<String> messages=new ArrayList<String>();
        AlarmEngine engine=new AlarmEngine(Collections.singletonList(water),(a,m)->messages.add(m),()->{});
        for(float sample:new float[]{Float.NaN,-1,1.1f,Float.POSITIVE_INFINITY})engine.tick(1000,true,1,sample);
        engine.tick(1000,false,1,.1f);assertTrue(messages.isEmpty());
        engine.tick(2000,true,1,.1f);assertEquals(1,messages.size());
        engine.toggle(water,3000);engine.tick(90_000,true,1,0);assertEquals(1,messages.size());
        engine.toggle(water,91_000);engine.tick(91_000,true,1,0);assertEquals(2,messages.size());
        engine.tick(92_000,false,1,0);engine.tick(93_000,true,1,0);assertEquals(3,messages.size());
        engine.resetSamples();engine.tick(94_000,true,1,0);assertEquals(4,messages.size());
    }
    @Test public void remainingMatchesInverseWurmReadingsAndFloatBoundaryIsStable() {
        assertEquals(.2f,AlarmEngine.remaining(.8f),.00001f);
        assertEquals(20,AlarmEngine.percentage(AlarmEngine.remaining(.8f)));
        assertEquals(5,AlarmEngine.percentage(AlarmEngine.remaining(.95f)));
        assertTrue(Float.isNaN(AlarmEngine.remaining(Float.NaN)));
        assertTrue(Float.isNaN(AlarmEngine.remaining(-1)));
    }
    @Test public void refillRearmsAndSampleJumpsDoNotBurst() {
        List<String> messages=new ArrayList<String>();
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->messages.add(m),()->{});
        engine.alarms().get(2).enabled=false;
        engine.tick(1000,true,.2f,Float.NaN);engine.tick(1000,true,.1f,Float.NaN);
        assertEquals(2,messages.size());
        engine.tick(1000,true,.15f,Float.NaN);assertEquals(2,messages.size()); // refill is not a falling edge
        engine.tick(1000,true,.14f,Float.NaN);assertEquals(3,messages.size());
        engine.tick(1000,true,.8f,Float.NaN);engine.tick(1000,true,.2f,Float.NaN);
        assertEquals(4,messages.size());
    }
    @Test public void prayerWaitsTwentyMinutesThenRepeatsEveryMinuteUntilRestart() {
        List<String> messages=new ArrayList<String>();int[] saves={0};
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->messages.add(m),()->saves[0]++);
        engine.alarms().get(3).enabled=false;
        Alarm prayer=engine.alarms().get(2);
        long start=1000;
        engine.tick(start,true,Float.NaN,Float.NaN);
        assertEquals(start+1_200_000,engine.dueAt(prayer));assertEquals(1,saves[0]);
        engine.tick(engine.dueAt(prayer)-1,true,Float.NaN,Float.NaN);assertTrue(messages.isEmpty());
        engine.tick(engine.dueAt(prayer),true,Float.NaN,Float.NaN);assertEquals(Arrays.asList("It's time to pray."),messages);
        engine.tick(engine.dueAt(prayer)+59_999,true,Float.NaN,Float.NaN);assertEquals(1,messages.size());
        engine.tick(engine.dueAt(prayer)+60_000,true,Float.NaN,Float.NaN);assertEquals(2,messages.size());
        long reset=engine.dueAt(prayer)+60_000;engine.restart(prayer,reset);
        engine.tick(reset+60_000,true,Float.NaN,Float.NaN);assertEquals(2,messages.size());
        engine.tick(reset+1_200_000,true,Float.NaN,Float.NaN);assertEquals(3,messages.size());
        engine.toggle(prayer,reset);engine.tick(reset+10_000_000,true,Float.NaN,Float.NaN);assertEquals(3,messages.size());
    }
    @Test public void completedPrayerSilencesOverdueRemindersAndStartsAFreshConfiguredInterval() {
        List<String> messages=new ArrayList<String>();int[] saves={0};
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->messages.add(m),()->saves[0]++);
        engine.alarms().get(3).enabled=false;
        Alarm prayer=engine.alarms().get(2);assertTrue(prayer.autoRestartAfterPrayer);
        prayer.delayMillis=600_000;
        engine.tick(1000,true,Float.NaN,Float.NaN);
        engine.tick(engine.dueAt(prayer),true,Float.NaN,Float.NaN);assertEquals(1,messages.size());
        long completed=engine.dueAt(prayer)+30_000;
        engine.prayerCompleted(completed);
        assertEquals(completed+600_000,engine.dueAt(prayer));assertEquals(2,saves[0]);
        engine.tick(completed+60_000,true,Float.NaN,Float.NaN);assertEquals(1,messages.size());
        engine.tick(engine.dueAt(prayer),true,Float.NaN,Float.NaN);assertEquals(2,messages.size());
        long early=engine.dueAt(prayer)+1;engine.prayerCompleted(early);
        assertEquals(early+600_000,engine.dueAt(prayer));
    }
    @Test public void automaticRestartRespectsCheckboxDisabledAlarmAndCustomTimers() {
        int[] saves={0};AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->{},()->saves[0]++);
        Alarm prayer=engine.alarms().get(2);engine.restart(prayer,1000);
        Alarm custom=Alarm.create(Alarm.Kind.TIMER,"Prayer","Custom reminder");custom.autoRestartAfterPrayer=true;
        engine.put(custom,1000);long customDue=engine.dueAt(custom);
        engine.togglePrayerAutoRestart(prayer);assertFalse(prayer.autoRestartAfterPrayer);
        long old=engine.dueAt(prayer);int saved=saves[0];engine.prayerCompleted(2000);
        assertEquals(old,engine.dueAt(prayer));assertEquals(customDue,engine.dueAt(custom));assertEquals(saved,saves[0]);
        engine.restart(prayer,3000);assertFalse(prayer.autoRestartAfterPrayer); // manual restart retains choice
        engine.togglePrayerAutoRestart(prayer);engine.toggle(prayer,3000);assertFalse(prayer.enabled);
        old=engine.dueAt(prayer);saved=saves[0];engine.prayerCompleted(4000);
        assertEquals(old,engine.dueAt(prayer));assertFalse(prayer.enabled);assertEquals(saved,saves[0]);
        assertTrue(prayer.copy().autoRestartAfterPrayer);
    }
    @Test public void disconnectedOrMissingSamplesAreSilentAndReconnectHasNoCatchupBurst() {
        List<String> messages=new ArrayList<String>();
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->messages.add(m),()->{});
        engine.alarms().get(3).enabled=false;
        Alarm prayer=engine.alarms().get(2);
        engine.tick(1000,false,.2f,.2f);assertEquals(0,engine.dueAt(prayer));assertTrue(messages.isEmpty());
        engine.tick(1000,true,Float.NaN,Float.NaN);
        engine.tick(9_000_000,false,.2f,.2f);assertTrue(messages.isEmpty());
        engine.tick(9_000_000,true,Float.NaN,Float.NaN);assertEquals(1,messages.size());
        engine.tick(9_000_001,true,Float.NaN,Float.NaN);assertEquals(1,messages.size());
    }
    @Test public void customOneShotAlarmCanBeAddedEditedAndDeleted() {
        List<String> messages=new ArrayList<String>();
        AlarmEngine engine=new AlarmEngine(Collections.emptyList(),(a,m)->messages.add(m),()->{});
        Alarm custom=Alarm.create(Alarm.Kind.TIMER,"Test","Custom message");custom.delayMillis=1000;custom.repeatMillis=0;
        engine.put(custom,1000);engine.tick(1999,true,1,1);assertTrue(messages.isEmpty());
        engine.tick(2000,true,1,1);assertEquals(Arrays.asList("Custom message"),messages);assertFalse(custom.enabled);
        engine.tick(9999,true,1,1);assertEquals(1,messages.size());
        engine.toggle(custom,10000);assertEquals(11000,engine.dueAt(custom));
        Alarm replacement=custom.copy();replacement.message="Changed";engine.put(replacement,10000);
        assertEquals(1,engine.alarms().size());engine.tick(11000,true,1,1);assertEquals("Changed",messages.get(1));
        engine.remove(replacement);assertTrue(engine.alarms().isEmpty());
    }
    @Test public void builtInsCannotBeDeletedAndInvalidDefinitionsAreRejected() {
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->{},()->{});
        engine.remove(engine.alarms().get(0));assertEquals(5,engine.alarms().size());
        Alarm invalid=Alarm.create(Alarm.Kind.FOOD,"Invalid","Hi");invalid.stopAt=21;
        try {engine.put(invalid,1);fail("Invalid threshold accepted");}catch(IllegalArgumentException expected){}
    }
    @Test public void meditationWaitsThirtyMinutesRepeatsAndRestartsOnlyOnItsOwnCompletion() {
        List<String> messages=new ArrayList<String>();List<Alarm> senders=new ArrayList<Alarm>();int[] saves={0};
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->{senders.add(a);messages.add(m);},()->saves[0]++);
        Alarm prayer=engine.alarms().get(2), meditation=engine.alarms().get(3);
        prayer.enabled=false;
        assertTrue(meditation.isMeditation());assertTrue(meditation.autoRestartAfterMeditation);
        assertEquals(1_800_000,meditation.delayMillis);
        engine.tick(1000,true,Float.NaN,Float.NaN);
        assertEquals(1_801_000,engine.dueAt(meditation));
        engine.tick(engine.dueAt(meditation)-1,true,Float.NaN,Float.NaN);assertTrue(messages.isEmpty());
        engine.tick(engine.dueAt(meditation),true,Float.NaN,Float.NaN);
        assertEquals(Arrays.asList("It's time to meditate."),messages);assertSame(meditation,senders.get(0));
        assertEquals(Alarm.ReminderColor.LAVENDER,senders.get(0).reminderColor());
        assertNotEquals(prayer.reminderColor(),meditation.reminderColor());
        engine.tick(engine.dueAt(meditation)+59_999,true,Float.NaN,Float.NaN);assertEquals(1,messages.size());
        engine.tick(engine.dueAt(meditation)+60_000,true,Float.NaN,Float.NaN);assertEquals(2,messages.size());
        long old=engine.dueAt(meditation);engine.prayerCompleted(old+70_000);assertEquals(old,engine.dueAt(meditation));
        long finished=old+90_000;int saved=saves[0];engine.meditationCompleted(finished);
        assertEquals(finished+1_800_000,engine.dueAt(meditation));assertEquals(saved+1,saves[0]);
        assertEquals(0,engine.nextReminderAt(meditation));assertEquals(0,engine.dueAt(prayer));
        engine.tick(finished+60_000,true,Float.NaN,Float.NaN);assertEquals(2,messages.size());
        engine.tick(engine.dueAt(meditation),true,Float.NaN,Float.NaN);assertEquals(3,messages.size());
        meditation.delayMillis=2_400_000;engine.meditationCompleted(finished+100_000);
        assertEquals(finished+2_500_000,engine.dueAt(meditation)); // use edited duration
    }
    @Test public void meditationCheckboxAndDisabledAlarmPreventRestartWithoutAffectingPrayer() {
        int[] saves={0};AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->{},()->saves[0]++);
        Alarm prayer=engine.alarms().get(2), meditation=engine.alarms().get(3);
        engine.restart(prayer,1000);engine.restart(meditation,1000);long prayerDue=engine.dueAt(prayer);
        Alarm custom=Alarm.create(Alarm.Kind.TIMER,"Meditation","It's time to meditate.");
        custom.autoRestartAfterMeditation=true;engine.put(custom,1000);long customDue=engine.dueAt(custom);
        engine.toggleAutoRestart(meditation);long old=engine.dueAt(meditation);int saved=saves[0];
        assertFalse(meditation.autoRestartAfterMeditation);assertFalse(meditation.copy().autoRestartAfterMeditation);
        engine.meditationCompleted(2000);
        assertEquals(old,engine.dueAt(meditation));assertEquals(prayerDue,engine.dueAt(prayer));assertEquals(customDue,engine.dueAt(custom));assertEquals(saved,saves[0]);
        engine.restart(meditation,3000);assertFalse(meditation.autoRestartAfterMeditation);
        engine.toggleAutoRestart(meditation);engine.toggle(meditation,3000);
        old=engine.dueAt(meditation);saved=saves[0];engine.meditationCompleted(4000);
        assertFalse(meditation.enabled);assertEquals(old,engine.dueAt(meditation));assertEquals(saved,saves[0]);
        assertTrue(meditation.copy().autoRestartAfterMeditation);
        assertEquals(Alarm.ReminderColor.ORANGE,custom.reminderColor());
    }
    @Test public void favorAtCurrentFaithNotOneHundredRepeatsEachMinuteWithoutCatchupBursts() {
        Alarm favor=AlarmEngine.defaults().get(4);List<String> messages=new ArrayList<String>();
        AlarmEngine engine=new AlarmEngine(Collections.singletonList(favor),(a,m)->messages.add(m),()->fail("Favor samples must not write settings"));
        assertTrue(favor.enabled);assertEquals(Alarm.Kind.FAVOR,favor.kind);assertEquals(60_000,favor.repeatMillis);
        engine.tick(1000,true,Float.NaN,Float.NaN,72.495f,72.5f);assertTrue(messages.isEmpty());
        engine.tick(1250,true,Float.NaN,Float.NaN,72.5f,72.5f);
        assertEquals(Arrays.asList("Favor is full. It's time to cast a spell."),messages);
        engine.tick(61_249,true,Float.NaN,Float.NaN,72.5f,72.5f);assertEquals(1,messages.size());
        engine.tick(61_250,true,Float.NaN,Float.NaN,72.5f,72.5f);assertEquals(2,messages.size());
        engine.tick(900_000,true,Float.NaN,Float.NaN,72.5f,72.5f);assertEquals(3,messages.size());
        engine.tick(900_250,true,Float.NaN,Float.NaN,72.5f,72.5f);assertEquals(3,messages.size());
        assertEquals(0,engine.dueAt(favor));
    }
    @Test public void spendingFavorOrIncreasingFaithStopsRepeatsAndRefillingRearmsImmediately() {
        List<String> messages=new ArrayList<String>();Alarm favor=AlarmEngine.defaults().get(4);
        AlarmEngine engine=new AlarmEngine(Collections.singletonList(favor),(a,m)->messages.add(m),()->{});
        engine.tick(1000,true,1,1,80,80);assertEquals(1,messages.size());
        engine.tick(1250,true,1,1,79,80);
        engine.tick(120_000,true,1,1,79,80);assertEquals(1,messages.size());
        engine.tick(120_250,true,1,1,80,80);assertEquals(2,messages.size());
        engine.tick(120_500,true,1,1,80,80.01f);
        engine.tick(240_000,true,1,1,80,80.01f);assertEquals(2,messages.size());
        engine.tick(240_250,true,1,1,80.01f,80.01f);assertEquals(3,messages.size());
        engine.tick(240_500,true,1,1,Math.nextDown(80.01f),80.01f);assertFalse(engine.favorFull());
    }
    @Test public void favorMissingInvalidAndDisconnectedSamplesNeverRingOrRetainSessionData() {
        List<String> messages=new ArrayList<String>();Alarm favor=AlarmEngine.defaults().get(4);
        AlarmEngine engine=new AlarmEngine(Collections.singletonList(favor),(a,m)->messages.add(m),()->{});
        float[][] invalid={{Float.NaN,80},{80,Float.NaN},{0,0},{-1,80},{Float.POSITIVE_INFINITY,80},{80,Float.POSITIVE_INFINITY},{80,-1}};
        for(float[] pair:invalid) {engine.tick(1000,true,1,1,pair[0],pair[1]);assertFalse(engine.favorKnown());}
        assertTrue(messages.isEmpty());
        engine.tick(2000,false,1,1,80,80);assertTrue(messages.isEmpty());assertFalse(engine.favorKnown());
        engine.tick(3000,true,1,1,80,80);assertEquals(1,messages.size());
        engine.tick(4000,true,1,1,Float.NaN,Float.NaN);
        engine.tick(5000,true,1,1,80,80);assertEquals(2,messages.size());
        engine.resetSamples();assertFalse(engine.favorKnown());assertEquals(0,engine.nextReminderAt(favor));
        engine.tick(6000,true,1,1,Float.NaN,Float.NaN);assertEquals(2,messages.size());
        engine.tick(7000,true,1,1,30,30);assertEquals(3,messages.size());
    }
    @Test public void favorDisableEnableAndEditsKeepItsMonitorBehavior() {
        List<String> messages=new ArrayList<String>();Alarm favor=AlarmEngine.defaults().get(4);
        AlarmEngine engine=new AlarmEngine(Collections.singletonList(favor),(a,m)->messages.add(m),()->{});
        engine.tick(1000,true,1,1,50,50);engine.toggle(favor,1250);
        engine.tick(61_000,true,1,1,50,50);assertEquals(1,messages.size());
        engine.toggle(favor,61_250);engine.tick(61_250,true,1,1,50,50);assertEquals(2,messages.size());
        Alarm edited=favor.copy();edited.name="Spell reminder";edited.message="Cast now.";
        engine.put(edited,62_000);engine.tick(62_000,true,1,1,50,50);assertEquals("Cast now.",messages.get(2));
        engine.remove(edited);assertEquals(1,engine.alarms().size());
        edited.repeatMillis=0;
        try {edited.validate();fail("A Favor monitor cannot be one-shot");}catch(IllegalArgumentException expected){}
    }
    @Test public void separateEnginesDoNotSharePercentageHistoryOrReminderSchedules() {
        List<Alarm> alarms=AlarmEngine.defaults();alarms.get(2).enabled=false;alarms.get(3).enabled=false;
        List<String> first=new ArrayList<String>(),second=new ArrayList<String>();
        AlarmEngine one=new AlarmEngine(alarms,(a,m)->first.add(m),()->{});
        AlarmEngine two=new AlarmEngine(alarms,(a,m)->second.add(m),()->{});
        one.tick(1000,true,.2f,1,50,50);two.tick(5000,true,.2f,1,50,50);
        assertEquals(2,first.size());assertEquals(2,second.size());
        one.tick(61_000,true,.2f,1,50,50);two.tick(61_000,true,.2f,1,50,50);
        assertEquals(3,first.size());assertEquals(2,second.size());
        two.tick(65_000,true,.2f,1,50,50);assertEquals(3,second.size());
    }
    @Test public void timerDeadlinesBelongToEachEngineAndCanBeRestoredIndependently() {
        List<Alarm> alarms=AlarmEngine.defaults();Alarm prayer=alarms.get(2);
        AlarmEngine one=new AlarmEngine(alarms,(a,m)->{},()->{}),two=new AlarmEngine(alarms,(a,m)->{},()->{});
        one.tick(1000,true,1,1);two.tick(5000,true,1,1);
        assertEquals(1_201_000,one.dueAt(prayer));assertEquals(1_205_000,two.dueAt(prayer));
        one.prayerCompleted(10_000);assertEquals(1_210_000,one.dueAt(prayer));assertEquals(1_205_000,two.dueAt(prayer));
        AlarmEngine restored=new AlarmEngine(alarms,(a,m)->{},()->{},new ReminderOptions(),one.timerDeadlines());
        assertEquals(one.dueAt(prayer),restored.dueAt(prayer));
        one.restart(prayer,20_000);assertEquals(1_210_000,restored.dueAt(prayer));
    }
    @Test public void changingOrDeletingACustomAlarmDiscardsItsPreviousRuntimeState() {
        Alarm timer=Alarm.create(Alarm.Kind.TIMER,"Custom","Reminder");
        AlarmEngine engine=new AlarmEngine(Collections.singletonList(timer),(a,m)->{},()->{});
        engine.restart(timer,1000);assertTrue(engine.timerDeadlines().containsKey(timer.id));
        Alarm monitor=timer.copy();monitor.kind=Alarm.Kind.FOOD;engine.put(monitor,2000);
        assertEquals(0,engine.dueAt(monitor));assertFalse(engine.timerDeadlines().containsKey(timer.id));
        engine.remove(monitor);assertTrue(engine.alarms().isEmpty());
        engine.put(timer,3000);assertEquals(1_203_000,engine.dueAt(timer));
    }
}
