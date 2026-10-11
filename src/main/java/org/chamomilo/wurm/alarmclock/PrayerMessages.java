package org.chamomilo.wurm.alarmclock;

import com.wurmonline.shared.util.MulticolorLineSegment;
import java.util.List;
import java.util.regex.Pattern;

/** Match only the player's successful completion in the inbound server Event channel. */
public final class PrayerMessages {
    private static final Pattern COMPLETED = Pattern.compile("You finish your prayer to [^\\r\\n]+\\.");
    private PrayerMessages() { }
    public static boolean completed(String channel, String message) {
        return (":Event".equals(channel) || "Event".equals(channel)) && message != null
            && COMPLETED.matcher(message.trim()).matches();
    }
    public static String text(List<MulticolorLineSegment> segments) {
        if (segments == null) return "";
        StringBuilder result = new StringBuilder();
        for (MulticolorLineSegment segment : segments)
            if (segment != null && segment.getText() != null) result.append(segment.getText());
        return result.toString();
    }
}
