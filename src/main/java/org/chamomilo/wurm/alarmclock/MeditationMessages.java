package org.chamomilo.wurm.alarmclock;

/** The inbound Event completion message confirms success even when no skill is gained. */
public final class MeditationMessages {
    private MeditationMessages() { }
    public static boolean completed(String channel, String message) {
        return (":Event".equals(channel) || "Event".equals(channel)) && message != null
            && "You finish your meditation.".equals(message.trim());
    }
}
