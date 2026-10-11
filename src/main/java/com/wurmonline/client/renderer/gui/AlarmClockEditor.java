package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.alarmclock.*;
import org.chamomilo.wurm.ui.v1.*;
import java.util.*;

/** Editing keeps the existing native inputs alive during shared-language changes. */
public final class AlarmClockEditor extends ChamomiloUiV1Window {
    private final Alarm original;
    private final AlarmEngine engine;
    private final Runnable closed;
    private final EditorBody body;
    public AlarmClockEditor(Alarm alarm, AlarmEngine engine, Runnable closed) {
        super("alarmclock.editor",Messages.t(alarm==null?"new":"edit"),Messages.language(),UiBackground.WALNUT,5);
        this.original=alarm;this.engine=engine;this.closed=closed;
        setHeaderHeight(40);setContentPadding(10);
        body=new EditorBody();setContent(body);setInitialSize(770,390,false,false,.5f,.5f);
        onClose(closed);
    }
    @Override void setSize(int width,int height) { super.setSize(Math.max(740,width),Math.max(385,height)); }
    public void relocalize() {
        setCaption(Messages.t(original==null?"new":"edit"));setLanguage(Messages.language());body.relocalize();
    }
    private final class EditorBody extends FlexComponent {
        final ChamomiloUiV1TextField name,delay,repeat,threshold,stop,message;
        final ChamomiloUiV1DropDown kind;
        private ChamomiloSkinnedButton save,cancel;
        private final List<FlexComponent> controls=new ArrayList<FlexComponent>();
        private final Map<AlarmType.Parameter,ChamomiloUiV1TextField> parameterFields=new EnumMap<AlarmType.Parameter,ChamomiloUiV1TextField>(AlarmType.Parameter.class);
        private boolean invalid;
        EditorBody() {
            super("alarmclock.editor.body");
            name=field("name",80); delay=field("delay",16); repeat=field("repeat",16);
            threshold=field("threshold",3); stop=field("stop",3); message=field("message",180);
            parameterFields.put(AlarmType.Parameter.DELAY,delay);parameterFields.put(AlarmType.Parameter.REPEAT,repeat);
            parameterFields.put(AlarmType.Parameter.THRESHOLD,threshold);parameterFields.put(AlarmType.Parameter.STOP,stop);
            kind=new ChamomiloUiV1DropDown("alarmclock.kind",330,typeOptions(),i->updateFields());
            kind.parent=this;
            Alarm value=original==null?Alarm.create(Alarm.Kind.TIMER,"",AlarmType.TIMER.defaultMessage):original;
            name.setText(value.name);message.setText(value.message);
            for(AlarmType.Parameter parameter:AlarmType.Parameter.values())parameterFields.get(parameter).setText(parameter.value(value));
            kind.selectIndex(index(value.kind));kind.setAvailable(original==null||!original.builtIn);
            relocalize();updateFields();
        }
        private ChamomiloUiV1TextField field(String id,int limit) {
            ChamomiloUiV1TextField field=new ChamomiloUiV1TextField("alarmclock."+id,330,limit,s->{},s->{});
            field.parent=this;return field;
        }
        void updateFields() {
            if(delay==null) return;
            AlarmType type=kind==null?AlarmType.TIMER:selectedType();
            for(Map.Entry<AlarmType.Parameter,ChamomiloUiV1TextField> field:parameterFields.entrySet())field.getValue().setAvailable(type.has(field.getKey()));
            // Keep native inputs and their values, but only expose applicable controls to painting and picking.
            controls.clear();controls.add(name);if(kind!=null)controls.add(kind);
            for(AlarmType.Parameter parameter:type.parameters())controls.add(parameterFields.get(parameter));
            controls.add(message);
            if(save!=null) {controls.add(save);controls.add(cancel);}
            componentResized();
        }
        void relocalize() {
            int selected=kind.selectedIndex();kind.setOptions(typeOptions());kind.selectIndex(selected);
            ChamomiloSkinnedButton.CaptionGroup group=new ChamomiloSkinnedButton.CaptionGroup(
                new String[]{Messages.t("save"),Messages.t("cancel")},150,32,UiScale.BASE);
            if(save!=null) {controls.remove(save);controls.remove(cancel);}
            save=AlarmClockWindow.action(Messages.t("save"),150,group,this::commit);
            cancel=AlarmClockWindow.action(Messages.t("cancel"),150,group,closed);
            save.parent=cancel.parent=this;updateFields();
        }
        private void commit() {
            try {
                Alarm result=original==null?Alarm.create(selectedKind(),name.value().trim(),message.value().trim()):original.copy();
                result.kind=selectedKind();result.name=name.value().trim();result.message=message.value().trim();
                for(AlarmType.Parameter parameter:selectedType().parameters())selectedType().apply(result,parameter,parameterFields.get(parameter).value());
                result.validate();engine.put(result,System.currentTimeMillis());closed.run();
            } catch(IllegalArgumentException failure) {
                invalid=true;
                name.setInvalid(name.value().trim().isEmpty());message.setInvalid(message.value().trim().isEmpty());
            }
        }
        private AlarmType selectedType() { return AlarmType.editable().get(kind.selectedIndex()); }
        private Alarm.Kind selectedKind() { return selectedType().kind; }
        @Override void componentResized() {
            if(save==null) return;
            int column=(width-20)/2;
            name.setLocation(x,y+24,column,32);kind.setLocation(x+column+20,y+24,column,32);
            int position=0;
            for(AlarmType.Parameter parameter:selectedType().parameters())parameterFields.get(parameter).setLocation(x+position++*(column+20),y+98,column,32);
            message.setLocation(x,y+172,width,32);
            save.setLocation(x,y+height-40,150,32);cancel.setLocation(x+158,y+height-40,150,32);
        }
        @Override public FlexComponent getComponentAt(int mx,int my) {
            if(!contains(mx,my)) return null;
            for(FlexComponent control:controls) if(control.contains(mx,my)) return control.getComponentAt(mx,my);
            return this;
        }
        @Override protected void renderComponent(Queue queue,float ignoredAlpha) {
            int column=(width-20)/2;
            label(queue,"name",x,y+16,column);label(queue,"kind",x+column+20,y+16,column);
            int position=0;
            for(AlarmType.Parameter parameter:selectedType().parameters())label(queue,selectedType().captionKey(parameter),x+position++*(column+20),y+90,column);
            label(queue,"message",x,y+164,width);
            AlarmClockWindow.paintText(queue,bodyFont(),Messages.t(selectedType().hintKey()),x,y+226,width,UiColor.MUTED);
            if(invalid) AlarmClockWindow.paintText(queue,bodyFont(),Messages.t("invalid"),x,y+248,width,UiColor.DANGER);
            for(FlexComponent control:controls) control.render(queue,1f);
        }
        private TextFont bodyFont() { return ChamomiloUiV1Fonts.caption(16,false,UiDensity.LOW); }
        void label(Queue queue,String key,int x,int baseline,int available) { AlarmClockWindow.paintText(queue,bodyFont(),Messages.t(key),x,baseline,available,UiColor.TEXT); }
    }
    static String[] typeOptions() {
        String[] options=new String[AlarmType.editable().size()];
        for(int i=0;i<options.length;i++)options[i]=Messages.t(AlarmType.editable().get(i).nameKey);
        return options;
    }
    static int index(Alarm.Kind kind) {return AlarmType.editable().indexOf(AlarmType.forKind(kind));}
    static long duration(String value,boolean zeroAllowed) {return AlarmType.duration(value,zeroAllowed);}
}
