package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.renderer.gui.text.AlarmClockSystemFont;
import org.chamomilo.wurm.alarmclock.Alarm;
import java.awt.Font;

/** Native system messages, using Wurm's large font and three-second fade without a panel. */
final class AlarmClockScreenMessages extends StaticComponent {
    private OnScreenMessageViewer viewer = new OnScreenMessageViewer();
    private final TextFont font = AlarmClockSystemFont.create(new Font(Font.SANS_SERIF, Font.BOLD, 36));

    AlarmClockScreenMessages() {
        super("Alarm Clock system messages");
        setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    void show(String message, Alarm.ReminderColor color) {
        viewer.prepareRender(font, SCREEN_WIDTH, SCREEN_HEIGHT);
        viewer.addMessage(message, color.red, color.green, color.blue,
                font, SCREEN_WIDTH, SCREEN_HEIGHT, (byte) 1);
    }

    void clear() { viewer = new OnScreenMessageViewer(); }

    // Keep drawing, but exclude this full-screen layer from native mouse focus.
    @Override boolean isAvailable() { return false; }
    @Override public StaticComponent getComponentAt(int mx, int my) { return null; }

    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        setSize(SCREEN_WIDTH, SCREEN_HEIGHT);
        viewer.prepareRender(font, width, height);
        viewer.render(queue, font);
    }
}
