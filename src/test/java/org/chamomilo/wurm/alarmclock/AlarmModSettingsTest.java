package org.chamomilo.wurm.alarmclock;

import org.junit.Test;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class AlarmModSettingsTest {
    @Test public void waterSettingsExposeThresholdAndRepeatsWithoutALowerCutoff() throws Exception {
        Fixture fixture=new Fixture();Set<String> keys=new HashSet<String>();
        fixture.settings.settings().forEach(spec->keys.add(spec.key));
        assertTrue(keys.contains("water/threshold"));assertTrue(keys.contains("water/repeat"));assertFalse(keys.contains("water/stop"));
        assertEquals("50",fixture.settings.value("water/threshold"));assertEquals("1",fixture.settings.value("water/repeat"));
        try {fixture.settings.change("water/repeat","0");fail("Water cannot be one-shot");}catch(IllegalArgumentException expected){}
    }
    @Test public void disabledFavorStaysOffAfterSharedSettingsEditsAndReload() throws Exception {
        Fixture fixture=new Fixture();
        fixture.settings.change("favor/enabled","false");
        fixture.settings.change("favor/repeat","1.5");
        fixture.settings.change("favor/message","My spell reminder.");
        fixture.engine.tick(1000,true,1,1,80,80);
        fixture.engine.tick(91_000,true,1,1,80,80);
        assertFalse(fixture.reminders.contains("favor"));
        assertEquals(Collections.singletonList("favor"),fixture.cancelled);
        AlarmStore reloaded=new AlarmStore(fixture.path);
        Alarm favor=reloaded.load().stream().filter(a->a.id.equals("favor")).findFirst().get();
        assertFalse(favor.enabled);assertEquals(90_000,favor.repeatMillis);
        assertEquals("My spell reminder.",favor.message);
        fixture.settings.change("favor/enabled","true");
        fixture.engine.tick(92_000,true,1,1,80,80);
        assertTrue(fixture.reminders.contains("favor"));
    }

    @Test public void savingAnEditorOpenedBeforeDisablingFavorDoesNotEnableItAgain() throws Exception {
        Fixture fixture=new Fixture();
        Alarm draft=fixture.engine.alarms().get(4).copy();
        fixture.settings.change("favor/enabled","false");
        draft.name="Edited spell reminder";
        fixture.engine.put(draft,1000);
        assertFalse("An editor draft must retain the current disabled choice",fixture.engine.alarms().get(4).enabled);
        fixture.engine.tick(2000,true,1,1,80,80);
        assertFalse(fixture.reminders.contains("favor"));
        assertFalse(new AlarmStore(fixture.path).load().get(4).enabled);
    }

    private static final class Fixture {
        final Path path=Files.createTempDirectory("favor-settings-").resolve("alarmclock.properties");
        final List<String> reminders=new ArrayList<String>(),cancelled=new ArrayList<String>();
        final AlarmEngine engine;
        final AlarmModSettings settings;
        Fixture() throws Exception {
            AlarmClockMod mod=new AlarmClockMod();AlarmStore store=new AlarmStore(path);
            engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->reminders.add(a.id),mod::save,store.options());
            set(mod,"store",store);set(mod,"engine",engine);
            engine.onReminderCancelled(a->cancelled.add(a.id));
            settings=new AlarmModSettings(mod);
        }
        private static void set(AlarmClockMod mod,String name,Object value) throws Exception {
            Field field=AlarmClockMod.class.getDeclaredField(name);field.setAccessible(true);field.set(mod,value);
        }
    }
}
