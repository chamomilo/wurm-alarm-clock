package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import org.chamomilo.wurm.alarmclock.*;
import org.chamomilo.wurm.ui.v1.*;
import java.util.function.Consumer;

/** The same four independent delivery choices and RGB editor on cards and drafts. */
final class AlarmClockPresentation extends FlexComponent {
    private static final String[] COLOR_KEYS={"gold","lightBlue","mint","pink","orange","lavender","white","red","customColor"};
    private static final int[] COLORS={0xFFF099,0x8CD9FF,0x99EDB8,0xFFABCC,0xFFBD80,0xC5ADFF,0xFFFFFF,0xFF8080};
    final ChoiceCheckBox[] choices=new ChoiceCheckBox[4];
    final ChamomiloUiV1DropDown palette;
    final ChamomiloUiV1TextField color;
    private final Alarm alarm;
    private final Consumer<String> colorChanged;
    private final ChamomiloUiV1Canvas canvas=new ChamomiloUiV1Canvas(this);
    private boolean updating;
    AlarmClockPresentation(Alarm alarm,Consumer<Alarm.Delivery> toggle,Consumer<String> colorChanged) {
        super("alarmclock.presentation."+alarm.id);this.alarm=alarm;this.colorChanged=colorChanged;
        for(Alarm.Delivery delivery:Alarm.Delivery.values()) {
            ChoiceCheckBox choice=new ChoiceCheckBox(Messages.t(delivery.key),Messages.t(delivery.key+"Hint"),AlarmClockWindow.checkboxBaseline(),
                ()->alarm.delivers(delivery),()->toggle.accept(delivery));
            choice.parent=this;choices[delivery.ordinal()]=choice;
        }
        palette=new ChamomiloUiV1DropDown("alarmclock.color.palette",230,colorOptions(),this::selectColor);palette.parent=this;
        palette.setCaptionTypography(16,UiDensity.LOW,22,UiScale.BASE);
        color=new ChamomiloUiV1TextField("alarmclock.color.rgb",110,7,this::editColor,this::editColor);color.parent=this;
        updating=true;color.setText(alarm.colorHex());updating=false;relocalize();
    }
    void applyTo(Alarm result) {
        for(Alarm.Delivery delivery:Alarm.Delivery.values())result.setDelivery(delivery,alarm.delivers(delivery));
        result.setColor(color.value());
    }
    private void editColor(String value) {
        if(updating)return;
        try { Alarm check=alarm.copy();check.setColor(value);color.setInvalid(false);colorChanged.accept(check.colorHex());updatePalette(); }
        catch(IllegalArgumentException invalid) { color.setInvalid(true); }
    }
    private void selectColor(int index) {
        if(updating||color==null||index<0||index>=COLORS.length)return;
        String value=String.format(java.util.Locale.ROOT,"#%06X",COLORS[index]);
        updating=true;color.setText(value);color.setInvalid(false);updating=false;colorChanged.accept(value);
    }
    private String[] colorOptions() { String[] result=new String[COLOR_KEYS.length];for(int i=0;i<result.length;i++)result[i]=Messages.t(COLOR_KEYS[i]);return result; }
    private void updatePalette() {
        int selected=COLORS.length,rgb=alarm.reminderColor().rgb();
        for(int i=0;i<COLORS.length;i++)if(COLORS[i]==rgb)selected=i;
        updating=true;palette.selectIndex(selected);updating=false;
    }
    void relocalize() {
        for(Alarm.Delivery delivery:Alarm.Delivery.values())choices[delivery.ordinal()].relabel(Messages.t(delivery.key),Messages.t(delivery.key+"Hint"),AlarmClockWindow.checkboxBaseline());
        updating=true;palette.setOptions(colorOptions());updating=false;updatePalette();
    }
    @Override void componentResized() {
        if(color==null)return;int column=(width-16)/2;
        for(int i=0;i<choices.length;i++)choices[i].setLocation(x+(i%2)*(column+16),y+(i/2)*28,column,28);
        palette.setLocation(x+76,y+66,column-76,32);color.setLocation(x+column+16,y+66,110,32);
    }
    @Override public FlexComponent getComponentAt(int mx,int my) {
        if(!contains(mx,my))return null;
        for(ChoiceCheckBox choice:choices)if(choice.contains(mx,my))return choice;
        if(palette.contains(mx,my))return palette.getComponentAt(mx,my);
        if(color.contains(mx,my))return color.getComponentAt(mx,my);return this;
    }
    @Override protected void renderComponent(Queue queue,float ignoredAlpha) {
        for(ChoiceCheckBox choice:choices)choice.render(queue,1f);
        AlarmClockWindow.paintText(queue,ChamomiloUiV1Fonts.caption(16,false,UiDensity.LOW),Messages.t("color"),x,y+87,72,UiColor.TEXT);
        palette.render(queue,1f);color.render(queue,1f);
        Alarm.ReminderColor tint=alarm.reminderColor();canvas.begin(queue);
        canvas.fill(new UiColor(tint.red,tint.green,tint.blue),1f,color.x+124,y+70,24,24);
        UiPainter.frame(canvas,3,1f,color.x+124,y+70,24,24);
    }
}
