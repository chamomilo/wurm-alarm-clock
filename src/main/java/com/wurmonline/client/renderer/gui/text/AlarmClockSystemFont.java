package com.wurmonline.client.renderer.gui.text;

import java.awt.Font;

/** Access to Wurm's package-private plain system-font constructor. */
public final class AlarmClockSystemFont {
    private AlarmClockSystemFont() { }
    public static TextFont create(Font font) { return new SimpleTextFont(font, false); }
}
