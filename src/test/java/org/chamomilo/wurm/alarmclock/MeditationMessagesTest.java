package org.chamomilo.wurm.alarmclock;

import com.wurmonline.shared.util.MulticolorLineSegment;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class MeditationMessagesTest {
    @Test public void onlySuccessfulCompletionInThePlayersEventChannelCounts() {
        assertTrue(MeditationMessages.completed(":Event","You finish your meditation."));
        assertTrue(MeditationMessages.completed("Event"," You finish your meditation. "));
        for(String text:new String[]{"You start meditating.","You fall into a trance.","You stop meditating.",
            "You fail to meditate.","You have hard time to focus and no question comes to you.",
            "Skyeday finishes her meditation.","Barefoot finishes his meditation.",
            "Meditating increased by 0.1","You finish your prayer to Fo.",
            "<Other> You finish your meditation.","You finish your meditation.\nYou stop meditating.",""})
            assertFalse(text,MeditationMessages.completed(":Event",text));
        for(String channel:new String[]{":Local","GL-Freedom","Skills",null})
            assertFalse(MeditationMessages.completed(channel,"You finish your meditation."));
        assertFalse(MeditationMessages.completed(":Event",null));
    }
    @Test public void coloredCompletionSegmentsFollowTheSameDetectionRule() {
        String message=PrayerMessages.text(Arrays.asList(new MulticolorLineSegment("You finish your ",(byte)0),
            new MulticolorLineSegment("meditation.",(byte)1)));
        assertTrue(MeditationMessages.completed(":Event",message));
    }
}
