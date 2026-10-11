package org.chamomilo.wurm.alarmclock;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class AlarmStoreTest {
    @Test public void definitionsAndTimerDeadlinesSurviveUtf8RoundTrip() throws Exception {
        Path folder=Files.createTempDirectory("alarm-store-");Path path=folder.resolve("nested/alarms.properties");
        AlarmStore store=new AlarmStore(path);List<Alarm> alarms=store.load();
        store.options().sound=false;store.options().animation=false;
        alarms.get(0).enabled=false;alarms.get(2).autoRestartAfterPrayer=false;
        alarms.get(3).delayMillis=2400000;alarms.get(3).autoRestartAfterMeditation=false;
        Map<String,Long> deadlines=new HashMap<String,Long>();deadlines.put("prayer",123456789L);deadlines.put("meditation",234567890L);
        Alarm custom=Alarm.create(Alarm.Kind.WATER,"Água и вода","Drink {percent}%");custom.threshold=30;alarms.add(custom);
        store.save(alarms,"ru",deadlines);List<Alarm> restored=store.load();
        assertEquals(6,restored.size());assertFalse(restored.get(0).enabled);assertEquals(123456789L,(long)store.timerDeadlines().get("prayer"));
        assertEquals("Água и вода",restored.get(5).name);assertEquals(30,restored.get(5).threshold);assertEquals("ru",store.language());
        assertTrue(restored.get(0).builtIn);assertFalse(restored.get(5).builtIn);
        assertEquals(Alarm.Kind.FAVOR,restored.get(4).kind);assertTrue(restored.get(4).builtIn);
        assertFalse(restored.get(2).autoRestartAfterPrayer);
        assertTrue(restored.get(3).isMeditation());assertFalse(restored.get(3).autoRestartAfterMeditation);
        assertEquals(234567890L,(long)store.timerDeadlines().get("meditation"));assertEquals(2400000,restored.get(3).delayMillis);
        assertFalse(store.options().sound);assertFalse(store.options().animation);
    }
    @Test public void legacyPrayerSettingsEnableAutoRestartWithoutChangingDeadlines() throws Exception {
        Path path=Files.createTempFile("alarm-legacy-",".properties");AlarmStore store=new AlarmStore(path);
        List<Alarm> alarms=AlarmEngine.defaults();alarms.get(2).delayMillis=600000;
        store.save(alarms,"en",Collections.singletonMap("prayer",123456789L));
        List<String> lines=new ArrayList<String>(Files.readAllLines(path,StandardCharsets.UTF_8));
        lines.removeIf(line->line.contains("autoRestartAfterPrayer="));Files.write(path,lines,StandardCharsets.UTF_8);
        List<Alarm> restored=store.load();
        assertTrue(restored.get(2).autoRestartAfterPrayer);assertFalse(restored.get(0).autoRestartAfterPrayer);
        assertEquals(123456789L,(long)store.timerDeadlines().get("prayer"));assertEquals(600000,restored.get(2).delayMillis);
        store.save(restored,"en");assertTrue(store.load().get(2).autoRestartAfterPrayer);
    }
    @Test public void preMeditationSettingsGainOneEnabledAlarmWithoutChangingExistingAlarms() throws Exception {
        Path path=Files.createTempFile("alarm-before-meditation-",".properties");AlarmStore store=new AlarmStore(path);
        List<Alarm> alarms=AlarmEngine.defaults();alarms.remove(4);alarms.remove(3);
        alarms.get(0).enabled=false;alarms.get(2).autoRestartAfterPrayer=false;
        alarms.add(Alarm.create(Alarm.Kind.TIMER,"Custom","Retain this message"));
        store.save(alarms,"de",Collections.singletonMap("prayer",123456789L));
        List<String> lines=new ArrayList<String>(Files.readAllLines(path,StandardCharsets.UTF_8));
        lines.removeIf(line->line.contains("autoRestartAfterMeditation="));Files.write(path,lines,StandardCharsets.UTF_8);
        List<Alarm> restored=store.load();assertEquals(6,restored.size());
        assertFalse(restored.get(0).enabled);assertEquals(123456789L,(long)store.timerDeadlines().get("prayer"));
        assertFalse(restored.get(2).autoRestartAfterPrayer);assertEquals("Retain this message",restored.get(3).message);
        Alarm meditation=restored.get(4);assertTrue(meditation.isMeditation());assertTrue(meditation.enabled);
        assertTrue(meditation.autoRestartAfterMeditation);assertEquals(1800000,meditation.delayMillis);assertFalse(store.timerDeadlines().containsKey(meditation.id));
        assertTrue(store.options().sound);assertTrue(store.options().animation);
        store.save(restored,"de");assertEquals(6,store.load().size()); // never duplicate on subsequent loads
    }
    @Test public void corruptFileIsReportedWithoutChangingIt() throws Exception {
        Path path=Files.createTempFile("alarm-invalid-",".properties");byte[] bytes="count=9999\n".getBytes(StandardCharsets.UTF_8);
        Files.write(path,bytes);try {new AlarmStore(path).load();fail();}catch(java.io.IOException expected){}
        assertArrayEquals(bytes,Files.readAllBytes(path));
    }
    @Test public void preFavorSettingsGainOneEnabledMonitorWhileKeepingExistingChoices() throws Exception {
        Path path=Files.createTempFile("alarm-before-favor-",".properties");AlarmStore store=new AlarmStore(path);
        List<Alarm> alarms=AlarmEngine.defaults();alarms.remove(4);
        alarms.get(0).enabled=false;alarms.get(3).autoRestartAfterMeditation=false;
        store.options().sound=false;store.save(alarms,"ru",Collections.singletonMap("prayer",123456789L));
        List<Alarm> restored=store.load();assertEquals(5,restored.size());
        assertFalse(restored.get(0).enabled);assertEquals(123456789L,(long)store.timerDeadlines().get("prayer"));
        assertFalse(restored.get(3).autoRestartAfterMeditation);assertFalse(store.options().sound);
        Alarm favor=restored.get(4);assertEquals("favor",favor.id);assertEquals(Alarm.Kind.FAVOR,favor.kind);
        assertTrue(favor.enabled);assertTrue(favor.builtIn);assertEquals(60_000,favor.repeatMillis);
        favor.enabled=false;favor.message="My spell reminder";store.save(restored,"ru");
        restored=store.load();assertEquals(5,restored.size());assertFalse(restored.get(4).enabled);
        assertEquals("My spell reminder",restored.get(4).message);
    }
    @Test public void addingFavorToAFullLegacySettingsFileStillAllowsTheNextLoad() throws Exception {
        Path path=Files.createTempFile("alarm-full-before-favor-",".properties");AlarmStore store=new AlarmStore(path);
        List<Alarm> alarms=AlarmEngine.defaults();alarms.remove(4);
        while(alarms.size()<100) alarms.add(Alarm.create(Alarm.Kind.TIMER,"Custom "+alarms.size(),"Keep this alarm"));
        store.save(alarms,"en");List<Alarm> restored=store.load();assertEquals(101,restored.size());
        store.save(restored,"en");assertEquals(101,store.load().size());
        assertEquals(1,store.load().stream().filter(a->a.id.equals("favor")).count());
    }
    @Test public void actualVersion024SettingsRestoreDeadlinesAndSurviveAnotherSaveAndLoad() throws Exception {
        Path path=legacyFixture();AlarmStore store=new AlarmStore(path);List<Alarm> alarms=store.load();
        List<String> messages=new ArrayList<String>();
        AlarmEngine engine=new AlarmEngine(alarms,(a,m)->messages.add(m),()->{},store.options(),store.timerDeadlines());
        Alarm prayer=alarms.get(2),meditation=alarms.get(3);
        assertFalse(alarms.get(0).enabled);assertFalse(store.options().sound);assertTrue(store.options().animation);
        assertEquals("de",store.language());assertFalse(meditation.autoRestartAfterMeditation);
        assertEquals(1_210_000,engine.dueAt(prayer));assertEquals(1_810_000,engine.dueAt(meditation));
        engine.tick(1000,true,Float.NaN,Float.NaN);assertTrue(messages.isEmpty());
        engine.tick(1_210_000,true,Float.NaN,Float.NaN);assertEquals(Arrays.asList("It's time to pray."),messages);
        engine.tick(1_299_999,true,Float.NaN,Float.NaN);assertEquals(1,messages.size());
        engine.tick(1_300_000,true,Float.NaN,Float.NaN);assertEquals(2,messages.size());
        engine.prayerCompleted(1_400_000);store.save(engine.alarms(),store.language(),engine.timerDeadlines());
        AlarmStore fresh=new AlarmStore(path);List<Alarm> freshAlarms=fresh.load();
        AlarmEngine restored=new AlarmEngine(freshAlarms,(a,m)->{},()->{},fresh.options(),fresh.timerDeadlines());
        assertEquals(2_600_000,restored.dueAt(freshAlarms.get(2)));assertEquals(1_810_000,restored.dueAt(freshAlarms.get(3)));
        assertFalse(fresh.options().sound);assertEquals("de",fresh.language());
    }
    @Test public void invalidUnusedLegacyFieldsAreIgnoredButInvalidActiveFieldsAreRejected() throws Exception {
        Path path=legacyFixture();Properties settings=properties(path);
        settings.setProperty("alarm.0.delay","not used");settings.setProperty("alarm.0.repeat","-1");
        settings.setProperty("alarm.1.repeat","not used");
        settings.setProperty("alarm.2.threshold","not used");settings.setProperty("alarm.2.stopAt","-10");
        settings.setProperty("alarm.4.delay","not used");settings.setProperty("alarm.4.threshold","-100");
        settings.setProperty("alarm.4.stopAt","not used");settings.setProperty("alarm.4.due","not used");
        write(path,settings);AlarmStore store=new AlarmStore(path);List<Alarm> alarms=store.load();
        assertEquals(5,alarms.size());for(Alarm alarm:alarms)alarm.validate();
        settings.setProperty("alarm.4.repeat","0");write(path,settings);byte[] before=Files.readAllBytes(path);
        try {store.load();fail("Zero-repeat Favor was accepted");}catch(java.io.IOException expected){}
        assertArrayEquals(before,Files.readAllBytes(path));assertTrue(store.timerDeadlines().isEmpty());
        settings.setProperty("alarm.4.repeat","60000");settings.setProperty("alarm.1.threshold","invalid");write(path,settings);
        try {store.load();fail("Invalid active threshold was accepted");}catch(java.io.IOException expected){}
    }
    private static Path legacyFixture() throws Exception {
        Path path=Files.createTempDirectory("alarm-024-").resolve("alarms.properties");
        try(java.io.InputStream input=AlarmStoreTest.class.getResourceAsStream("/alarmclock-0.2.4.properties")) {
            assertNotNull(input);Files.copy(input,path);
        }
        return path;
    }
    private static Properties properties(Path path) throws Exception {
        Properties settings=new Properties();try(java.io.Reader reader=Files.newBufferedReader(path,StandardCharsets.UTF_8)) {settings.load(reader);}return settings;
    }
    private static void write(Path path,Properties settings) throws Exception {
        try(java.io.Writer writer=Files.newBufferedWriter(path,StandardCharsets.UTF_8)) {settings.store(writer,"Test legacy settings");}
    }
}
