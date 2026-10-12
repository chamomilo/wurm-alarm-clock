package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.renderer.gui.text.AlarmClockSystemFont;
import org.chamomilo.wurm.alarmclock.Alarm;
import java.awt.Font;
import java.lang.reflect.Field;
import java.util.*;

/** Native system messages, using Wurm's large font and three-second fade without a panel. */
final class AlarmClockScreenMessages extends StaticComponent {
    private final OnScreenMessageViewer viewer = new OnScreenMessageViewer();
    private final List<OnScreenMessage> pending=nativeMessages(viewer);
    private final Map<OnScreenMessage,String> owners=new IdentityHashMap<OnScreenMessage,String>();
    private final TextFont font = AlarmClockSystemFont.create(new Font(Font.SANS_SERIF, Font.BOLD, 36));

    AlarmClockScreenMessages() {
        super("Alarm Clock system messages");
        setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    void show(Alarm alarm,String message) {
        viewer.prepareRender(font, SCREEN_WIDTH, SCREEN_HEIGHT);
        owners.keySet().retainAll(pending);
        Alarm.ReminderColor color=alarm.reminderColor();
        viewer.addMessage(message, color.red, color.green, color.blue,
                font, SCREEN_WIDTH, SCREEN_HEIGHT, (byte) 1);
        owners.put(pending.get(pending.size()-1),alarm.id);
    }

    void clear() { pending.clear();owners.clear(); }
    void cancel(String alarmId) {
        pending.removeIf(message->alarmId.equals(owners.get(message)));
        owners.keySet().retainAll(pending);
    }
    @SuppressWarnings("unchecked")
    private static List<OnScreenMessage> nativeMessages(OnScreenMessageViewer viewer) {
        // Wurm has no public removal API. Retain its native layout/fade and remove only owned entries.
        try {
            Field messages=OnScreenMessageViewer.class.getDeclaredField("messageList");
            messages.setAccessible(true);
            return (List<OnScreenMessage>)messages.get(viewer);
        } catch(ReflectiveOperationException failure) { throw new IllegalStateException("Cannot access native reminder messages",failure); }
    }

    // Keep drawing, but exclude this full-screen layer from native mouse focus.
    @Override boolean isAvailable() { return false; }
    @Override public StaticComponent getComponentAt(int mx, int my) { return null; }

    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        viewer.prepareRender(font, width, height);
        owners.keySet().retainAll(pending);
        viewer.render(queue, font);
    }
}
