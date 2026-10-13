package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import org.chamomilo.wurm.ui.v1.*;
import java.util.function.BooleanSupplier;

/** Native completed-click checkbox; press/drag and hover match the shared kit. */
final class ChoiceCheckBox extends WButton implements ButtonListener {
    private final BooleanSupplier checked;
    private final Runnable toggle;
    private final ChamomiloUiV1Canvas canvas=new ChamomiloUiV1Canvas(this);
    private final UiButtonMotion motion=new UiButtonMotion();
    private int baseline;
    private boolean armed;
    ChoiceCheckBox(String caption,String hint,int baseline,BooleanSupplier checked,Runnable toggle) {
        super(caption);this.checked=checked;this.toggle=toggle;setButtonListener(this);
        text=ChamomiloUiV1Fonts.caption(16,false,UiDensity.HIGH);
        textBold=ChamomiloUiV1Fonts.caption(16,true,UiDensity.HIGH);
        relabel(caption,hint,baseline);sizeFlags=0;setSize(300,28);sizeFlags=FIXED_HEIGHT;
    }
    void relabel(String caption,String hint,int baseline) { label=caption;this.baseline=baseline;setHoverString(caption+"\n"+hint); }
    @Override public void buttonPressed(WButton button) { armed=isEnabled();if(armed)motion.pointerPressed(System.nanoTime()); }
    @Override public void buttonClicked(WButton button) { boolean fire=armed&&isEnabled();armed=false;if(fire)toggle.run(); }
    @Override protected void mouseDragged(int mx,int my) { armed=false;super.mouseDragged(mx,my); }
    @Override protected void renderComponent(Queue queue,float ignoredAlpha) {
        motion.update(isEnabled(),hovered,isCloseHovered,System.nanoTime());
        canvas.begin(queue);int top=y+(height-18)/2;
        UiPainter.button(canvas,motion.brightness(),motion.depth(),motion.hover(),UiScale.BASE,1f,x,top,18,18);
        int offset=Math.round(motion.depth());UiColor color=UiPainter.buttonCaptionColor(isEnabled(),motion.hover());
        if(checked.getAsBoolean())UiIcon.CHECK.paint(canvas,color,1f,x+3+offset,top+3+offset,12);
        AlarmClockWindow.paintText(queue,hovered?textBold:text,label,x+26,y+baseline,width-26,color);
    }
}
