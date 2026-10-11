package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.settings.WindowPosition;
import org.chamomilo.wurm.alarmclock.Messages;
import org.chamomilo.wurm.ui.v1.*;

/** A small movable alarm clock, following Keybinder's launcher click/drag lifecycle. */
public final class AlarmClockLauncher extends StaticComponent implements WindowSerializer {
    private final Runnable open;
    private final DragController dragger = new DragController(this);
    private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
    private final UiScale scale;
    private int pressX, pressY;
    private boolean pressed, dragged;
    public AlarmClockLauncher(Runnable open, UiScale scale) {
        super("Alarm Clock launcher"); this.open = open; this.scale = scale;
        setSize(scale.pixels(42), scale.pixels(42));
    }
    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        UiPainter.background(canvas.begin(queue), UiBackground.WALNUT, 1f, x, y, width, height);
        UiPainter.frame(canvas, scale.pixels(3), 1f, x, y, width, height);
        AlarmClockGlyph.paint(canvas,UiColor.TEXT,x+scale.pixels(20),y+scale.pixels(20),scale.pixels(24),0);
    }
    @Override protected void leftPressed(int mx, int my, int count) {
        pressed = true; dragged = false; pressX = mx; pressY = my; dragger.leftPressed(mx, my, count);
    }
    @Override protected void mouseDragged(int mx, int my) {
        if (Math.abs(mx-pressX) > 3 || Math.abs(my-pressY) > 3) dragged = true;
        dragger.mouseDragged(mx, my);
    }
    @Override protected void leftReleased(int mx, int my) {
        dragger.leftReleased(mx, my);
        boolean click = pressed && !dragged && contains(mx, my) && Math.abs(mx-pressX) <= 3 && Math.abs(my-pressY) <= 3;
        pressed = false; if (click) open.run();
    }
    @Override public void pick(PickData data, int mx, int my) { if (contains(mx, my)) data.addText(Messages.t("launcherHint")); }
    void moveToDefault() { setPosition(scale.pixels(12), scale.pixels(370)); ensureVisible(); }
    @Override public WindowPosition createPositionHints() { return new WindowPosition(x, y, dragger.isDisabled()?1:0); }
    @Override public void restorePositionHints(WindowPosition position) {
        // Migrate the original default, which could sit underneath the inventory on the right.
        if (position.x == Math.max(10, SCREEN_WIDTH - 230) && position.y == 100) moveToDefault();
        else { setPosition(position.x, position.y); ensureVisible(); }
        dragger.setDisabled((position.flags & 1) != 0);
    }
}
