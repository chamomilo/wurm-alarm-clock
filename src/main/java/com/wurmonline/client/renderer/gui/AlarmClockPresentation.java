package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import org.chamomilo.wurm.alarmclock.*;
import org.chamomilo.wurm.ui.v1.*;
import java.util.function.Consumer;

/** The same four independent delivery choices and color palette on cards and drafts. */
final class AlarmClockPresentation extends FlexComponent {
    private static final String[] COLOR_KEYS={"gold","lightBlue","mint","pink","orange","lavender","white","red","customColor"};
    private static final int[] COLORS={0xFFF099,0x8CD9FF,0x99EDB8,0xFFABCC,0xFFBD80,0xC5ADFF,0xFFFFFF,0xFF8080};
    final ChoiceCheckBox[] choices=new ChoiceCheckBox[4];
    final ChamomiloUiV1DropDown palette;
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
        relocalize();
    }
    void applyTo(Alarm result) {
        for(Alarm.Delivery delivery:Alarm.Delivery.values())result.setDelivery(delivery,alarm.delivers(delivery));
        result.colorRgb=alarm.colorRgb;
    }
    private void selectColor(int index) {
        if(updating||palette==null||index<0||index>=COLORS.length)return;
        String value=String.format(java.util.Locale.ROOT,"#%06X",COLORS[index]);
        colorChanged.accept(value);updatePalette();
    }
    private int selectedColor() {
        int selected=COLORS.length,rgb=alarm.reminderColor().rgb();
        for(int i=0;i<COLORS.length;i++)if(COLORS[i]==rgb)selected=i;
        return selected;
    }
    private String[] colorOptions() {
        String[] result=new String[COLORS.length+(selectedColor()==COLORS.length?1:0)];
        for(int i=0;i<result.length;i++)result[i]=Messages.t(COLOR_KEYS[i]);return result;
    }
    private void updatePalette() {
        updating=true;palette.setOptions(colorOptions());palette.selectIndex(selectedColor());updating=false;
    }
    void relocalize() {
        for(Alarm.Delivery delivery:Alarm.Delivery.values())choices[delivery.ordinal()].relabel(Messages.t(delivery.key),Messages.t(delivery.key+"Hint"),AlarmClockWindow.checkboxBaseline());
        updatePalette();
    }
    @Override void componentResized() {
        if(palette==null)return;int column=(width-16)/2;
        for(int i=0;i<choices.length;i++)choices[i].setLocation(x+(i%2)*(column+16),y+(i/2)*28,column,28);
        palette.setLocation(x+76,y+66,column-76,32);
    }
    @Override public FlexComponent getComponentAt(int mx,int my) {
        if(!contains(mx,my))return null;
        for(ChoiceCheckBox choice:choices)if(choice.contains(mx,my))return choice;
        if(palette.contains(mx,my))return palette.getComponentAt(mx,my);
        return this;
    }
    @Override protected void renderComponent(Queue queue,float ignoredAlpha) {
        for(ChoiceCheckBox choice:choices)choice.render(queue,1f);
        AlarmClockWindow.paintText(queue,ChamomiloUiV1Fonts.caption(16,false,UiDensity.LOW),Messages.t("color"),x,y+87,72,UiColor.TEXT);
        palette.render(queue,1f);
        Alarm.ReminderColor tint=alarm.reminderColor();canvas.begin(queue);
        int swatchX=palette.x+palette.width+14;
        canvas.fill(new UiColor(tint.red,tint.green,tint.blue),1f,swatchX,y+70,24,24);
        UiPainter.frame(canvas,3,1f,swatchX,y+70,24,24);
    }
}
