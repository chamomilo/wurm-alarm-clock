package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.alarmclock.*;
import org.chamomilo.wurm.ui.v1.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Keybinder-style window with a persistent scrollable alarm list. */
public final class AlarmClockWindow extends ChamomiloUiV1Window {
    private final AlarmEngine engine;
    private final BooleanSupplier saveFailed;
    private final Consumer<Alarm> previewSound;
    private final UiScale scale = UiScale.BASE;
    private final Body body;
    private FlexComponent titleHeader;
    private AlarmClockEditor editor;
    private ChamomiloUiV1Window confirmation;
    public AlarmClockWindow(AlarmEngine engine, BooleanSupplier saveFailed) {
        this(engine,saveFailed,alarm->{});
    }
    public AlarmClockWindow(AlarmEngine engine, BooleanSupplier saveFailed, Consumer<Alarm> previewSound) {
        super("Alarm Clock", Messages.t("title"), Messages.language(), UiBackground.WALNUT, 5);
        this.engine = engine; this.saveFailed = saveFailed;this.previewSound=previewSound;
        titleHeader.text=ChamomiloUiV1Fonts.caption(20,true,UiDensity.HIGH);
        setHeaderHeight(Math.max(40,titleHeader.text.getHeight()+23)); setContentPadding(8);
        body = new Body(); setContent(body);
        setInitialSize(780, 710, false, false, .5f, .5f);
        refresh();
    }
    @Override void setSize(int width, int height) { super.setSize(Math.max(720, width), Math.max(330, height)); }
    @Override void setComponent(FlexComponent component,int region) {
        super.setComponent(component,region);
        if(region==NORTH)titleHeader=component;
    }
    public void refresh() { body.refresh(); }
    public void relocalize() {
        setCaption(Messages.t("title")); setLanguage(Messages.language()); refresh();
        if (editor != null) editor.relocalize();
        if (confirmation != null) { confirmation.hide(); hud.removeComponent(confirmation); confirmation = null; }
    }
    private void edit(Alarm alarm) {
        disposeEditor();
        editor = new AlarmClockEditor(alarm, engine, () -> { disposeEditor(); refresh(); });
        editor.show(hud);
    }
    public void disposeEditor() {
        if (editor != null) { editor.hide(); hud.removeComponent(editor); editor = null; }
        if (confirmation != null) { confirmation.hide(); hud.removeComponent(confirmation); confirmation = null; }
    }
    private void delete(Alarm alarm) {
        if (confirmation != null) { confirmation.hide(); hud.removeComponent(confirmation); }
        confirmation = new ChamomiloUiV1Window("alarmclock.confirm", Messages.t("deleteTitle"), Messages.language(), UiBackground.WALNUT, 5);
        WurmArrayPanel<FlexComponent> content = new WurmArrayPanel<FlexComponent>("alarmclock.confirm.body", WurmArrayPanel.DIR_VERTICAL);
        content.addComponent(new Label(Messages.name(alarm), 350, 36));
        WurmArrayPanel<FlexComponent> actions = new WurmArrayPanel<FlexComponent>("alarmclock.confirm.actions", WurmArrayPanel.DIR_HORIZONTAL);
        ChamomiloSkinnedButton.CaptionGroup group = new ChamomiloSkinnedButton.CaptionGroup(
                new String[]{Messages.t("delete"), Messages.t("cancel")}, 150, 32, scale);
        actions.componentWidthOffset = 8;
        actions.addComponent(action(Messages.t("delete"), 150, group, () -> {
            engine.remove(alarm); confirmation.hide(); hud.removeComponent(confirmation); confirmation = null; refresh();
        }));
        actions.addComponent(action(Messages.t("cancel"), 150, group, () -> {
            confirmation.hide(); hud.removeComponent(confirmation); confirmation = null;
        }));
        content.addComponent(actions); confirmation.setContent(content); confirmation.setInitialSize(390, 170, false);
        confirmation.onClose(() -> { confirmation.hide(); hud.removeComponent(confirmation); confirmation = null; });
        confirmation.show(hud);
    }
    static ChamomiloSkinnedButton action(String caption, int width, ChamomiloSkinnedButton.CaptionGroup group, Runnable run) {
        ChamomiloSkinnedButton button = new ChamomiloSkinnedButton(caption, new ButtonListener() {
            public void buttonPressed(WButton button) { }
            public void buttonClicked(WButton button) { run.run(); }
        }, width);
        button.configure(UiScale.BASE, UiDensity.HIGH, width, 32, group);
        return button;
    }
    static String clock(long millis) {
        long seconds = Math.max(0, (millis + 999) / 1000);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
    static String minutes(long millis) { return AlarmType.minutes(millis); }
    static int checkboxBaseline() {
        int top=Integer.MAX_VALUE,bottom=Integer.MIN_VALUE;
        for(String key:new String[]{"sound","animation","autoRestartPrayer","autoRestartMeditation"})
            for(boolean bold:new boolean[]{false,true}) {
                java.awt.Rectangle ink=UiTypography.ink(Messages.t(key),16,bold,UiDensity.HIGH);
                top=Math.min(top,ink.y);bottom=Math.max(bottom,ink.y+ink.height);
            }
        return (28-(bottom-top))/2-top;
    }
    static void paintText(Queue queue, TextFont font, String value, int x, int baseline, int available, UiColor color) {
        int end = value.length();
        while (end > 0 && font.getWidth(value.substring(0, end) + (end < value.length() ? "..." : "")) > available)
            end = value.offsetByCodePoints(end, -1);
        String visible = end == 0 ? "" : value.substring(0, end) + (end < value.length() ? "..." : "");
        font.moveTo(x, baseline); font.paint(queue, visible, color.red, color.green, color.blue, 1f);
    }
    static final class Label extends FlexComponent {
        String caption;
        Label(String caption, int width, int height) { super("alarmclock.label", 0, 0, width, height); this.caption = caption; sizeFlags = FIXED_HEIGHT; }
        @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
            paintText(queue, text, caption, x + 4, y + (height-text.getHeight())/2 + text.getAscent(), width-8, UiColor.TEXT);
        }
        @Override public void pick(PickData data, int mx, int my) { if (contains(mx,my)) data.addText(caption); }
    }
    private final class Body extends FlexComponent {
        private final ListContent list = new ListContent();
        private final ChamomiloUiV1ScrollPanel scroll = new ChamomiloUiV1ScrollPanel("alarmclock.list.scroll", list, false, true);
        private ChamomiloSkinnedButton create;
        private ChoiceCheckBox sound, animation;
        private boolean showingSaveFailure;
        Body() { super("alarmclock.body"); scroll.parent = this; scroll.setScale(scale); }
        void refresh() {
            // Named standard group: alarmclock.toolbar.
            ChamomiloSkinnedButton.CaptionGroup group = new ChamomiloSkinnedButton.CaptionGroup(
                    new String[]{Messages.t("new")}, new int[]{180}, 32, scale);
            create = action(Messages.t("new"), 180, group, () -> edit(null));
            create.parent = this;
            int baseline=checkboxBaseline();
            sound=new ChoiceCheckBox(Messages.t("sound"),Messages.t("soundHint"),baseline,()->engine.options().sound,engine::toggleSound);
            animation=new ChoiceCheckBox(Messages.t("animation"),Messages.t("animationHint"),baseline,()->engine.options().animation,engine::toggleAnimation);
            sound.parent=animation.parent=this;
            list.rebuild(); componentResized();
        }
        @Override void componentResized() {
            if (scroll == null || create == null) return;
            create.setLocation(x, y, 180, 32);
            sound.setLocation(x+196,y+2,92,28);animation.setLocation(x+296,y+2,140,28);
            int listWidth = Math.max(630, width-18);
            list.resize(listWidth);
            showingSaveFailure=saveFailed.getAsBoolean();
            scroll.setLocation(x, y+42, width, Math.max(40, height-42-(showingSaveFailure?24:0))); scroll.layout();
        }
        @Override public FlexComponent getComponentAt(int mx,int my) {
            if (!contains(mx,my)) return null;
            if (create.contains(mx,my)) return create;
            if (sound.contains(mx,my)) return sound;
            if (animation.contains(mx,my)) return animation;
            if (scroll.contains(mx,my)) return scroll.getComponentAt(mx,my);
            return this;
        }
        @Override public void gameTick() {
            if(showingSaveFailure!=saveFailed.getAsBoolean())componentResized();
            scroll.gameTick();
        }
        @Override protected void renderComponent(Queue queue,float ignoredAlpha) {
            create.render(queue,1f);
            sound.render(queue,1f);animation.render(queue,1f);
            scroll.render(queue,1f);
            if(saveFailed.getAsBoolean())paintText(queue,text,Messages.t("saveFailed"),x,y+height-8,width,UiColor.DANGER);
        }
    }
    private final class ListContent extends FlexComponent {
        private final List<Card> cards = new ArrayList<Card>();
        ListContent() { super("alarmclock.list",0,0,650,1); sizeFlags = FIXED_WIDTH | FIXED_HEIGHT; }
        void rebuild() {
            cards.clear();
            String[] variants = {Messages.t("edit"),Messages.t("disable"),Messages.t("enable"),Messages.t("restart"),Messages.t("delete"),Messages.t("testSound")};
            int[] widths = {120,120,120,120,120,120};
            // Named compact group: alarmclock.row-actions; includes all rows and enable/disable variants.
            UiButtonLayout[] group = UiButtonLayout.compactGroup(variants,widths,scale,true);
            // Named group: alarmclock.checkboxes; all four controls share one ink envelope.
            int baseline=checkboxBaseline();
            for (Alarm alarm : engine.alarms()) { Card card = new Card(alarm,group,baseline); card.parent = this; cards.add(card); }
            resize(width);
        }
        void resize(int width) { sizeFlags = 0; setSize(width,Math.max(1,cards.size()*110)); sizeFlags = FIXED_WIDTH | FIXED_HEIGHT; componentResized(); }
        @Override void componentResized() { if (cards!=null) for (int i=0;i<cards.size();i++) cards.get(i).setLocation(x,y+i*110,width,110); }
        @Override public FlexComponent getComponentAt(int mx,int my) {
            if (!contains(mx,my)) return null;
            for(Card card:cards) if(card.contains(mx,my)) return card.getComponentAt(mx,my);
            return this;
        }
        @Override protected void renderComponent(Queue queue,float ignoredAlpha) { for(Card card:cards) card.render(queue,1f); }
    }
    private final class Card extends FlexComponent {
        private final Alarm alarm;
        private final AlarmType type;
        private final List<ChamomiloUiV1Button> buttons = new ArrayList<ChamomiloUiV1Button>();
        private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
        private final ChoiceCheckBox autoRestart;
        private final UiColor accent;
        private final UiColor plate;
        Card(Alarm alarm, UiButtonLayout[] group, int baseline) {
            super("alarmclock.row."+alarm.id); this.alarm=alarm;type=alarm.type();
            Alarm.ReminderColor color=alarm.reminderColor();
            accent=type.appearance.coloredCaption?new UiColor(color.red,color.green,color.blue):UiColor.TEXT;
            plate=new UiColor(type.appearance.plateRed,type.appearance.plateGreen,type.appearance.plateBlue);
            add("edit",group[0],()->edit(alarm));
            int state=alarm.enabled?1:2;
            add(alarm.enabled?"disable":"enable",group[state],()->{engine.toggle(alarm,System.currentTimeMillis());refresh();});
            if(type.has(AlarmType.Parameter.DELAY)) add("restart",group[3],()->{engine.restart(alarm,System.currentTimeMillis());refresh();});
            if(!alarm.builtIn) add("delete",group[4],()->delete(alarm));
            add("testSound",group[5],()->previewSound.accept(alarm));
            String key=alarm.isPrayer()?"autoRestartPrayer":"autoRestartMeditation";
            autoRestart=alarm.isPrayer()||alarm.isMeditation()?new ChoiceCheckBox(Messages.t(key),Messages.t(key+"Hint"),baseline,alarm::autoRestart,()->engine.toggleAutoRestart(alarm)):null;
            if(autoRestart!=null) autoRestart.parent=this;
        }
        void add(String key, UiButtonLayout layout, Runnable action) {
            ChamomiloUiV1Button button=ChamomiloUiV1Button.compact(Messages.t(key),120,scale,action);
            button.setHoverString(Messages.t(key));
            button.applyCompactLayout(layout,scale); button.parent=this; buttons.add(button);
        }
        @Override void componentResized() {
            if(buttons!=null) for(int i=0;i<buttons.size();i++) buttons.get(i).setLocation(x+12+i*128,y+76,120,24);
            if(autoRestart!=null) autoRestart.setLocation(x+width/2,y+45,width/2-12,28);
        }
        @Override public FlexComponent getComponentAt(int mx,int my) {
            if(!contains(mx,my)) return null;
            for(ChamomiloUiV1Button button:buttons) if(button.contains(mx,my)) return button;
            if(autoRestart!=null&&autoRestart.contains(mx,my)) return autoRestart;
            return this;
        }
        @Override public void pick(PickData data,int mx,int my) {
            if(contains(mx,my)) { data.addText(Messages.name(alarm)); data.addText(alarm.message); }
        }
        @Override protected void renderComponent(Queue queue,float ignoredAlpha) {
            canvas.begin(queue); UiPainter.background(canvas,UiBackground.LEATHER,1f,x,y,width,height-6);
            // Same fixed material translucency as Keybinder's constructor zones; independent of HUD alpha/hover.
            canvas.fill(plate,.78f,x,y,width,height-6);
            boolean favor=type.rule==AlarmType.Rule.FULL_FAVOR;
            TextFont heading=ChamomiloUiV1Fonts.caption(20,true,type.appearance.sharedCardFonts?UiDensity.HIGH:UiDensity.LOW);
            TextFont bodyFont=type.appearance.sharedCardFonts?ChamomiloUiV1Fonts.caption(16,false,UiDensity.LOW):text;
            TextFont statusFont=type.appearance.sharedCardFonts?ChamomiloUiV1Fonts.caption(16,true,UiDensity.LOW):textBold;
            paintText(queue,heading,Messages.name(alarm),x+12,y+23,width/2-20,accent);
            String status;
            long now=System.currentTimeMillis();
            long dueAt=engine.dueAt(alarm);
            float value=alarm.kind==Alarm.Kind.FOOD?engine.food():engine.water();
            if(!alarm.enabled) status=Messages.t("off");
            else if(type.has(AlarmType.Parameter.DELAY)) status=dueAt==0?Messages.t("start"):
                now>=dueAt?Messages.t("due"):clock(dueAt-now)+" "+Messages.t("remaining");
            else if(favor) status=engine.favorKnown()?String.format(Locale.ROOT,"%.2f / %.2f",engine.favor(),engine.faith())+
                (engine.favorFull()?"  "+Messages.t("full"):""):Messages.t("waiting");
            else status=Float.isFinite(value)?AlarmEngine.percentage(value)+"% "+Messages.t("remaining"):Messages.t("waiting");
            paintText(queue,statusFont,status,x+width/2,y+22,width/2-12,alarm.enabled?accent:UiColor.MUTED);
            String rule=favor?Messages.t("favorAtMax")+"  |  "+Messages.t("repeat")+" "+minutes(alarm.repeatMillis)+" "+Messages.t("minutes"):
                type.rule==AlarmType.Rule.BELOW_PERCENTAGE?Messages.t("alertBelow")+" "+alarm.threshold+"%  |  "+Messages.t("repeat")+" "+minutes(alarm.repeatMillis)+" "+Messages.t("minutes"):
                type.has(AlarmType.Parameter.DELAY)?minutes(alarm.delayMillis)+" "+Messages.t("minutes")+"  |  "+
                (alarm.repeatMillis==0?Messages.t("once"):Messages.t("repeat")+" "+minutes(alarm.repeatMillis)+" "+Messages.t("minutes")):
                Messages.t("threshold")+" "+alarm.threshold+"%  |  "+Messages.t("everyPercent")+"  |  "+Messages.t("stop")+" "+alarm.stopAt+"%";
            paintText(queue,bodyFont,rule,x+12,y+43,width-24,UiColor.TEXT);
            paintText(queue,bodyFont,alarm.message,x+12,y+63,autoRestart==null?width-24:width/2-24,UiColor.MUTED);
            for(ChamomiloUiV1Button button:buttons) button.render(queue,1f);
            if(autoRestart!=null) autoRestart.render(queue,1f);
        }
    }
    /** Native completed-click checkbox using the same kit painter and glyph as Keybinder. */
    private final class ChoiceCheckBox extends WButton implements ButtonListener {
        private final BooleanSupplier checked;
        private final Runnable toggle;
        private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
        private final UiButtonMotion motion = new UiButtonMotion();
        private final int baseline;
        private boolean armed;
        ChoiceCheckBox(String caption, String hint, int baseline, BooleanSupplier checked, Runnable toggle) {
            super(caption);this.checked=checked;this.toggle=toggle;setButtonListener(this);
            text=ChamomiloUiV1Fonts.caption(16,false,UiDensity.HIGH);
            textBold=ChamomiloUiV1Fonts.caption(16,true,UiDensity.HIGH);
            this.baseline=baseline;
            setHoverString(label+"\n"+hint);
            sizeFlags=0; setSize(300,28); sizeFlags=FIXED_HEIGHT;
        }
        @Override public void buttonPressed(WButton button) { armed=isEnabled(); if(armed) motion.pointerPressed(System.nanoTime()); }
        @Override public void buttonClicked(WButton button) {
            boolean fire=armed&&isEnabled(); armed=false;
            if(fire) toggle.run();
        }
        @Override protected void mouseDragged(int mx,int my) { armed=false; super.mouseDragged(mx,my); }
        @Override protected void renderComponent(Queue queue,float ignoredAlpha) {
            motion.update(isEnabled(),hovered,isCloseHovered,System.nanoTime());
            canvas.begin(queue); int top=y+(height-18)/2;
            UiPainter.button(canvas,motion.brightness(),motion.depth(),motion.hover(),scale,1f,x,top,18,18);
            int offset=Math.round(motion.depth()*scale.factor);
            UiColor color=UiPainter.buttonCaptionColor(isEnabled(),motion.hover());
            if(checked.getAsBoolean()) UiIcon.CHECK.paint(canvas,color,1f,x+3+offset,top+3+offset,12);
            paintText(queue,hovered?textBold:text,label,x+26,y+baseline,width-26,color);
        }
    }
}
