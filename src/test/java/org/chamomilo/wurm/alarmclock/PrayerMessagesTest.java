package org.chamomilo.wurm.alarmclock;

import com.wurmonline.shared.util.MulticolorLineSegment;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class PrayerMessagesTest {
    @Test public void onlyThePlayersSuccessfulServerEventCounts() {
        // Verbatim completion messages from the client's existing Event logs.
        for(String deity:new String[]{"Fo","Vynora","Libila","Magranon","Custom Deity"})
            assertTrue(PrayerMessages.completed(":Event","You finish your prayer to "+deity+"."));
        assertTrue(PrayerMessages.completed("Event"," You finish your prayer to Fo. "));
        for(String text:new String[]{"You start to pray at the altar of Fo.","You stop praying.",
            "You are now too far away to pray.","You cannot pray at altars belonging to Fo.",
            "You send your prayers to Fo.","Chamomilo finishes his prayer to Fo.",
            "Faith increased by 0.1 to 50.0","<Other> You finish your prayer to Fo.",
            "You finish your prayer to Fo.\nYou stop praying.","You finish your prayer to ",""})
            assertFalse(text,PrayerMessages.completed(":Event",text));
        for(String channel:new String[]{"GL-Freedom",":Local","Skills",null})
            assertFalse(PrayerMessages.completed(channel,"You finish your prayer to Fo."));
        assertFalse(PrayerMessages.completed(":Event",null));
    }
    @Test public void coloredServerSegmentsAreCombinedWithoutInsertingSpaces() {
        String message=PrayerMessages.text(Arrays.asList(new MulticolorLineSegment("You finish your ",(byte)0),
            new MulticolorLineSegment("prayer to Fo.",(byte)1)));
        assertTrue(PrayerMessages.completed(":Event",message));
        assertEquals("",PrayerMessages.text(null));
    }
}
