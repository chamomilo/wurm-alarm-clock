package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.settings.SavePosManager;
import com.wurmonline.client.settings.WindowPosition;
import org.chamomilo.wurm.alarmclock.*;
import org.chamomilo.wurm.ui.v1.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Production native layout/input, substituting only GPU/font startup for repeatable previews. */
public final class AlarmClockLayoutProbe {
    private static final BufferedImage canvas=new BufferedImage(2100,1400,BufferedImage.TYPE_INT_ARGB);
    private static final Graphics2D graphics=canvas.createGraphics();
    private static final Map<UiAsset,BufferedImage> assets=new EnumMap<UiAsset,BufferedImage>(UiAsset.class);
    private static final Map<String,BufferedImage> tintedAssets=new HashMap<String,BufferedImage>();
    private static final Deque<Shape> clips=new LinkedList<Shape>();
    private static final Map<String,Point> labels=new HashMap<String,Point>();
    private static final Map<String,Integer> labelSizes=new HashMap<String,Integer>();
    private static final Map<String,Color> labelColors=new HashMap<String,Color>();
    private static Path output;
    private static int previews;
    private static long previewTime;
    private static final java.util.List<Chime> previewChimes=new ArrayList<Chime>();
    private static final java.util.List<String> eventMessages=new ArrayList<String>();
    private static final java.util.List<Color> eventColors=new ArrayList<Color>();
    public static void event(String channel,float r,float g,float b,String message) {
        check(channel.equals(":Event"),"Reminders use Wurm's shared Event channel");
        eventMessages.add(message);eventColors.add(new Color(r/255f,g/255f,b/255f));
    }
    public static long previewTime() { return previewTime; }
    public static void main(String[] args) throws Exception {
        output=Paths.get(args[0]);Files.createDirectories(output);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        fixtureHud();
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->{},()->{});
        engine.tick(System.currentTimeMillis(),true,.19f,.34f,87.5f,87.5f);
        AlarmClockWindow window=new AlarmClockWindow(engine,()->false,a->previewChimes.add(Chime.forAlarm(a)));
        window.setLocation(20,20,780,710);
        preview("alarms-en",window);
        verifyToolbar(window);verifyActions(window,engine);verifyScroll(window,engine);
        verifyFavorHudToggle();
        verifyLauncher();
        verifyHudLifecycle();
        verifyActivityCompletionCallbacks();verifyNotificationColors();
        verifyRingingOverlay();
        verifyFavorEditor(engine);
        verifyNeedsEditors();verifyEditorTypeSwitching();
        Path chimes=output.resolve("chimes");Files.createDirectories(chimes);
        for(Chime chime:Chime.values())try(java.io.InputStream wave=chime.open()) {
            Files.copy(wave,chimes.resolve(chime.name().toLowerCase(Locale.ROOT)+".wav"),StandardCopyOption.REPLACE_EXISTING);
        }
        int[] closed={0};
        AlarmClockEditor editor=new AlarmClockEditor(null,engine,()->closed[0]++);
        editor.setLocation(20,20,770,390);
        Object body=field(AlarmClockEditor.class,"body").get(editor);
        ChamomiloUiV1TextField name=(ChamomiloUiV1TextField)field(body.getClass(),"name").get(body);
        ChamomiloUiV1TextField message=(ChamomiloUiV1TextField)field(body.getClass(),"message").get(body);
        ChamomiloUiV1TextField delay=(ChamomiloUiV1TextField)field(body.getClass(),"delay").get(body);
        ChamomiloUiV1DropDown kind=(ChamomiloUiV1DropDown)field(body.getClass(),"kind").get(body);
        name.setText("Custom prayer reminder");message.setText("It's time to pray.");delay.setText("20");
        preview("editor-en",editor);verifyEditorBounds(editor);
        verifyPopup(kind);
        for(String language:new String[]{"pt-BR","de","ru"}) {
            Messages.select(language);window.relocalize();editor.relocalize();
            check(name.value().equals("Custom prayer reminder")&&message.value().equals("It's time to pray.")&&delay.value().equals("20"),"Language switch preserves unsaved fields");
            preview("alarms-"+language,window);preview("editor-"+language,editor);verifyToolbar(window);verifyAutoRestartChoiceBounds(window);verifyEditorBounds(editor);
            window.setLocation(20,20,720,370);window.gameTick();verifyAutoRestartChoiceBounds(window);
            window.setLocation(20,20,780,710);window.gameTick();
        }
        Messages.select("en");editor.relocalize();
        delay.setText("bad");click((WButton)field(body.getClass(),"save").get(body));check(closed[0]==0,"Invalid duration rejected");
        preview("editor-validation",editor);
        delay.setText("1");click((WButton)field(body.getClass(),"save").get(body));check(closed[0]==1,"Native completed Save creates alarm");
        check(engine.alarms().get(engine.alarms().size()-1).name.equals("Custom prayer reminder"),"Created alarm values retained");
        check(AlarmClockEditor.duration("0",true)==0&&AlarmClockEditor.duration("1,5",false)==90000,"Localized numeric entry");
        verifyHookContracts();
        graphics.dispose();System.out.println("ALARM_UI_OK: "+previews+" production previews; native menu/launcher opening, close/maximize, HUD visibility/recovery, saved positions, input, scrolling, grouped fonts, fixed alpha and localization");
    }
    private static void verifyToolbar(AlarmClockWindow window) throws Exception {
        snapshot(window,1f);
        check(labelSizes.get(Messages.t("title"))==20,"Title uses the updater's 20 px branded heading font");
        check(UiTypography.width(Messages.t("title"),20,true,UiDensity.HIGH)<=window.width-120,"Localized window heading clears its chrome controls");
        check(!labels.containsKey(Messages.t("intro"))&&!labels.containsKey("Alarm Clock "+AlarmClockMod.VERSION),"Crossed-out instruction and version lines are removed");
        Object body=field(AlarmClockWindow.class,"body").get(window);
        ChamomiloSkinnedButton create=(ChamomiloSkinnedButton)field(body.getClass(),"create").get(body);
        for(ChamomiloSkinnedButton b:new ChamomiloSkinnedButton[]{create}) for(boolean bold:new boolean[]{false,true}) {
            Rectangle ink=UiTypography.ink(b.getLabel(),b.captionFontPixels(),bold,UiDensity.HIGH);
            check(UiTypography.width(b.getLabel(),b.captionFontPixels(),bold,UiDensity.HIGH)<=b.width-8,"Toolbar caption clears sides");
            check(b.captionBaseline()+ink.y>=3&&b.captionBaseline()+ink.y+ink.height<=b.height-3,"Toolbar glyph ink clears top/bottom");
        }
        int baseline=-1;
        for(String key:new String[]{"sound","animation"}) {
            WButton choice=(WButton)field(body.getClass(),key).get(body);
            int current=field(choice.getClass(),"baseline").getInt(choice);
            if(baseline>=0)check(current==baseline,"Presentation checkbox group shares baseline");baseline=current;
            for(boolean bold:new boolean[]{false,true}) {
                Rectangle ink=UiTypography.ink(choice.getLabel(),16,bold,UiDensity.HIGH);
                check(UiTypography.width(choice.getLabel(),16,bold,UiDensity.HIGH)<=choice.width-26,"Sound/animation captions fit "+Messages.language());
                check(current+ink.y>=2&&current+ink.y+ink.height<=choice.height-2,"Sound/animation ink fits");
            }
            check(choice.x+choice.width<=window.x+window.width,"Presentation choices fit inside window");
        }
    }
    private static void verifyActions(AlarmClockWindow window,AlarmEngine engine) throws Exception {
        Object body=field(AlarmClockWindow.class,"body").get(window);
        Object list=field(body.getClass(),"list").get(body);
        java.util.List<?> cards=(java.util.List<?>)field(list.getClass(),"cards").get(list);
        Object food=cards.get(0);
        java.util.List<?> buttons=(java.util.List<?>)field(food.getClass(),"buttons").get(food);
        WButton disable=(WButton)buttons.get(1);click(disable);check(!engine.alarms().get(0).enabled,"Native row action disables only selected alarm");
        preview("alarms-disabled",window);
        window.refresh();
        cards=(java.util.List<?>)field(list.getClass(),"cards").get(list);buttons=(java.util.List<?>)field(cards.get(0).getClass(),"buttons").get(cards.get(0));
        click((WButton)buttons.get(1));check(engine.alarms().get(0).enabled,"Row enable re-arms monitor");
        WButton choice=activityChoice(window,"prayer");Alarm prayerAlarm=engine.alarms().get(2);
        buttons=(java.util.List<?>)field(choice.parent.getClass(),"buttons").get(choice.parent);
        long old=engine.dueAt(prayerAlarm);click((WButton)buttons.get(2));check(engine.dueAt(prayerAlarm)>=old,"Prayer card retains its native manual Restart action");
        check(prayerAlarm.autoRestartAfterPrayer,"Prayer auto restart is enabled by default");
        long due=engine.dueAt(prayerAlarm);click(choice);
        check(!prayerAlarm.autoRestartAfterPrayer&&engine.dueAt(prayerAlarm)==due,"Prayer checkbox persists choice without restarting timer");
        choice.leftPressed(choice.x+10,choice.y+10,1);choice.mouseDragged(choice.x+20,choice.y+10);choice.leftReleased(choice.x+20,choice.y+10);
        check(!prayerAlarm.autoRestartAfterPrayer,"Dragging checkbox cancels toggle");
        click(choice);check(prayerAlarm.autoRestartAfterPrayer,"Native checkbox re-enables automatic restart");
        choice=activityChoice(window,"meditation");Alarm meditation=engine.alarms().get(3);
        check(meditation.autoRestartAfterMeditation,"Meditation auto restart is enabled by default");
        due=engine.dueAt(meditation);long prayerDue=engine.dueAt(prayerAlarm);click(choice);
        check(!meditation.autoRestartAfterMeditation&&engine.dueAt(meditation)==due&&engine.dueAt(prayerAlarm)==prayerDue,"Meditation checkbox changes only its own preference");
        click(choice);check(meditation.autoRestartAfterMeditation,"Meditation checkbox re-enables automatic restart");
        Object meditationCard=choice.parent;
        buttons=(java.util.List<?>)field(meditationCard.getClass(),"buttons").get(meditationCard);
        click((WButton)buttons.get(2));check(engine.dueAt(meditation)>=due&&engine.dueAt(prayerAlarm)==prayerDue,"Meditation has its own manual restart button");
        click((WButton)buttons.get(3));check(previewChimes.equals(Arrays.asList(Chime.MEDITATION)),"Test chime plays selected event's motif");
        Object favorCard=cards.get(4);
        Alarm favor=engine.alarms().get(4);
        buttons=(java.util.List<?>)field(favorCard.getClass(),"buttons").get(favorCard);
        check(buttons.size()==3&&field(favorCard.getClass(),"autoRestart").get(favorCard)==null,"Favor has Edit, Disable and Test chime without timer actions");
        click((WButton)buttons.get(2));check(previewChimes.equals(Arrays.asList(Chime.MEDITATION,Chime.FAVOR)),"Favor preview has its own chime");
        click((WButton)buttons.get(1));check(!favor.enabled,"Favor can be disabled from its own card");
        window.refresh();cards=(java.util.List<?>)field(list.getClass(),"cards").get(list);
        buttons=(java.util.List<?>)field(cards.get(4).getClass(),"buttons").get(cards.get(4));
        click((WButton)buttons.get(1));check(favor.enabled,"Favor can be re-enabled from its own card");
        WButton sound=(WButton)field(body.getClass(),"sound").get(body),animation=(WButton)field(body.getClass(),"animation").get(body);
        click(sound);check(!engine.options().sound&&engine.options().animation,"Sound option toggles independently");click(sound);
        click(animation);check(!engine.options().animation&&engine.options().sound,"Animation option toggles independently");click(animation);
        verifyAutoRestartChoiceBounds(window);
    }
    private static void verifyScroll(AlarmClockWindow window,AlarmEngine engine) throws Exception {
        for(int i=0;i<8;i++)engine.put(Alarm.create(Alarm.Kind.TIMER,"Custom timer "+(i+1),"Reminder "+(i+1)),System.currentTimeMillis());
        window.refresh();window.setLocation(20,20,720,370);
        Object body=field(AlarmClockWindow.class,"body").get(window);
        ChamomiloUiV1ScrollPanel scroll=(ChamomiloUiV1ScrollPanel)field(body.getClass(),"scroll").get(body);
        scroll.gameTick();check(scroll.content.height>((FlexComponent)scroll.offs).height,"Overflow list is clipped");
        scroll.mouseWheeled(scroll.x+20,scroll.y+20,1);check(scroll.yo>0,"Native wheel changes scroll offset");
        preview("alarms-narrow-scrolled",window);
        verifyAutoRestartChoiceBounds(window);
        scroll.scrollTo(0,550);window.gameTick();preview("alarms-custom-plates",window);
        for(Alarm alarm:new ArrayList<Alarm>(engine.alarms()))if(!alarm.builtIn)engine.remove(alarm);
        window.refresh();window.setLocation(20,20,780,710);scroll.scrollTo(0,0);window.gameTick();
    }
    private static void verifyLauncher() throws Exception {
        int[] opens={0};AlarmClockLauncher launcher=new AlarmClockLauncher(()->opens[0]++,UiScale.BASE);
        launcher.setPosition(20,20);launcher.leftPressed(25,25,1);launcher.leftReleased(25,25);check(opens[0]==1,"Launcher completed click opens window");
        launcher.leftPressed(25,25,1);launcher.mouseDragged(40,40);launcher.leftReleased(40,40);check(opens[0]==1,"Launcher dragging cancels open");
        preview("launcher",launcher);
    }
    private static void verifyFavorHudToggle() throws Exception {
        HeadsUpDisplay previous=WurmComponent.hud;
        try {
            HeadsUpDisplay hud=fixtureHud();field(HeadsUpDisplay.class,"visible").setBoolean(hud,true);
            java.util.List<String> reminders=new ArrayList<String>();
            AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->reminders.add(a.id),()->{});
            Alarm favor=engine.alarms().get(4);
            AlarmClockBridge bridge=new AlarmClockBridge(hud,engine,()->false);
            AlarmClockWindow window=(AlarmClockWindow)field(AlarmClockBridge.class,"window").get(bridge);
            AlarmClockScreenMessages messages=(AlarmClockScreenMessages)field(AlarmClockBridge.class,"messages").get(bridge);
            AlarmClockAlert alert=(AlarmClockAlert)field(AlarmClockBridge.class,"alert").get(bridge);
            AlarmSounds sounds=(AlarmSounds)field(AlarmClockBridge.class,"sounds").get(bridge);
            java.util.List<Chime> heard=new ArrayList<Chime>();
            field(AlarmSounds.class,"player").set(sounds,(java.util.function.Consumer<Chime>)heard::add);
            bridge.open();
            for(int height:new int[]{710,370}) {
                window.setLocation(20,20,780,height);window.layout();window.gameTick();
                Object body=field(AlarmClockWindow.class,"body").get(window);
                ChamomiloUiV1ScrollPanel scroll=(ChamomiloUiV1ScrollPanel)field(body.getClass(),"scroll").get(body);
                scroll.scrollTo(0,440);window.gameTick();
                Object list=field(body.getClass(),"list").get(body);
                java.util.List<?> cards=(java.util.List<?>)field(list.getClass(),"cards").get(list);
                java.util.List<?> buttons=(java.util.List<?>)field(cards.get(4).getClass(),"buttons").get(cards.get(4));
                WButton disable=(WButton)buttons.get(1);
                int px=disable.x+disable.width/2,py=disable.y+disable.height/2;
                bridge.remind(favor,favor.message);alert.tick(1000);
                Alarm food=engine.alarms().get(0);bridge.remind(food,"Keep the food reminder.");
                bridge.ring(favor); // Both an active Favor animation and a queued repeat must be canceled.
                check(hud.getComponentAt(px,py)==disable,"HUD picks the visible Favor Disable button at height "+height);
                check(hud.mousePressed(px,py,0,1),"HUD accepts Favor Disable press");
                check(field(HeadsUpDisplay.class,"mouseFocusComponent").get(hud)==disable,"Favor Disable receives native mouse focus");
                hud.mouseReleased(px,py,0);
                check(!favor.enabled,"Full HUD click disables Favor at height "+height);
                snapshot(messages,1f);
                check(!labels.containsKey(favor.message)&&labels.containsKey("Keep the food reminder."),"Disabling Favor immediately clears only its visible text");
                Alarm active=(Alarm)field(AlarmClockAlert.class,"active").get(alert);
                check(active==null||!active.id.equals(favor.id),"Disabling Favor cancels its active animation");
                for(Object queued:(java.util.Deque<?>)field(AlarmClockAlert.class,"pending").get(alert))
                    check(!((Alarm)queued).id.equals(favor.id),"Disabling Favor removes queued animations");
                heard.clear();sounds.tick(1_000_000);sounds.tick(2_000_000);
                check(heard.equals(Arrays.asList(Chime.FOOD)),"Disabling Favor drops only its queued chime");
                engine.tick(1_000_000,true,1,1,80,80);
                check(!reminders.contains("favor"),"Disabled full Favor never sends reminders");
                window.refresh();window.layout();window.gameTick();
                cards=(java.util.List<?>)field(list.getClass(),"cards").get(list);
                buttons=(java.util.List<?>)field(cards.get(4).getClass(),"buttons").get(cards.get(4));
                WButton enable=(WButton)buttons.get(1);
                px=enable.x+enable.width/2;py=enable.y+enable.height/2;
                check(hud.getComponentAt(px,py)==enable,"HUD picks Favor Enable after rebuilding the list");
                hud.mousePressed(px,py,0,1);hud.mouseReleased(px,py,0);
                check(favor.enabled,"Full HUD click re-enables Favor");
                bridge.stopEffects();
            }
            AlarmClockEditor editor=new AlarmClockEditor(favor,engine,()->{});
            editor.setLocation(20,20,770,390);editor.show(hud);editor.layout();
            bridge.remind(favor,favor.message);bridge.tick(System.currentTimeMillis());
            Alarm disabled=favor.copy();disabled.enabled=false;
            engine.updateSettings(disabled,"enabled",System.currentTimeMillis());
            snapshot(messages,1f);check(!labels.containsKey(favor.message),"Settings disable clears Favor text immediately");
            check(field(AlarmClockAlert.class,"active").get(alert)==null,"Settings disable removes active Favor animation");
            Object editorBody=field(AlarmClockEditor.class,"body").get(editor);
            WButton save=(WButton)field(editorBody.getClass(),"save").get(editorBody);
            int px=save.x+save.width/2,py=save.y+save.height/2;
            check(hud.getComponentAt(px,py)==save,"HUD picks Save in the older Favor editor");
            hud.mousePressed(px,py,0,1);hud.mouseReleased(px,py,0);
            check(!engine.alarms().get(4).enabled,"Native editor Save keeps Favor disabled by Settings");
            editor.hide();hud.removeComponent(editor);
            bridge.dispose();
        } finally {WurmComponent.hud=previous;}
    }
    private static void verifyHudLifecycle() throws Exception {
        HeadsUpDisplay hud=WurmComponent.hud;
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->{},()->{});
        AlarmClockBridge bridge=new AlarmClockBridge(hud,engine,()->false);
        AlarmClockWindow window=(AlarmClockWindow)field(AlarmClockBridge.class,"window").get(bridge);
        AlarmClockLauncher launcher=(AlarmClockLauncher)field(AlarmClockBridge.class,"launcher").get(bridge);
        check(hud.isComponentEnabled(launcher)&&!hud.isComponentEnabled(window),"Icon visible by default; list initially closed");
        check(launcher.x==12&&launcher.y==370,"Default icon is on the left, away from inventory");
        check(hud.getComponents().get(hud.getComponents().size()-1)==launcher,"Icon brought to front");
        check(hud.mainMenu.getMenuSet().size()==1&&hud.hudSettings.getMenuSet().size()==1,"Window and icon have separate native menu registrations");
        AlarmClockAlert alert=(AlarmClockAlert)field(AlarmClockBridge.class,"alert").get(bridge);
        long ringing=System.currentTimeMillis();bridge.ring(engine.alarms().get(3));bridge.tick(ringing);
        check(hud.getComponents().contains(alert)&&!hud.isComponentEnabled(window),"Ringing overlay works with the list closed");
        check(hud.mainMenu.getMenuSet().size()==1&&hud.hudSettings.getMenuSet().size()==1,"Overlay creates no menu or window registration");
        engine.options().animation=false;bridge.tick(ringing+1);
        check(!hud.getComponents().contains(alert),"Animation option removes active overlay");engine.options().animation=true;
        bridge.ring(engine.alarms().get(2));bridge.tick(ringing+2);bridge.stopEffects();
        check(!hud.getComponents().contains(alert),"Stopping effects removes free HUD overlay");
        WButton menu=hud.mainMenu.getMenuSet().iterator().next().getKey();
        hud.mainMenu.buttonClicked(menu);
        check(hud.isComponentEnabled(window),"Real Main menu opens the window");
        window.layout();
        FlexComponent header=(FlexComponent)field(ChamomiloUiV1Window.class,"header").get(window);
        header.componentResized();
        WButton close=(WButton)field(header.getClass(),"close").get(header);
        WButton maximize=(WButton)field(header.getClass(),"maximize").get(header);
        int normalWidth=window.width;
        click(maximize);check(window.width==hud.getWidth()-20,"Maximize works after menu opening");
        header.componentResized();click(maximize);check(window.width==normalWidth,"Restore works after menu opening");
        header.componentResized();click(close);
        check(!hud.isComponentEnabled(window)&&hud.isComponentEnabled(launcher),"Native close glyph closes menu-opened window; icon stays visible");
        launcher.leftPressed(launcher.x+10,launcher.y+10,1);launcher.leftReleased(launcher.x+10,launcher.y+10);
        check(hud.isComponentEnabled(window),"Registered HUD icon opens window");
        header.componentResized();click(close);check(!hud.isComponentEnabled(window),"Close also works after icon opening");

        WButton toggle=hud.hudSettings.getMenuSet().iterator().next().getKey();
        hud.hudSettings.buttonClicked(toggle);check(!hud.isComponentEnabled(launcher),"HUD Settings can hide the icon");
        hud.hudSettings.buttonClicked(toggle);check(hud.isComponentEnabled(launcher),"HUD Settings can show the icon");
        for(String language:new String[]{"pt-BR","de","ru","en"}) {
            Messages.select(language);bridge.relocalize();
            check(menu.getLabel().equals(Messages.t("title"))&&toggle.getLabel().equals(Messages.t("launcher")),"Both native menu captions follow language changes");
        }

        AlarmClockMod mod=new AlarmClockMod();
        Field instance=field(AlarmClockMod.class,"instance");Object previous=instance.get(null);
        field(AlarmClockMod.class,"engine").set(mod,engine);
        field(AlarmClockMod.class,"activeHud").set(mod,hud);
        field(AlarmClockMod.class,"bridge").set(mod,bridge);
        instance.set(null,mod);
        try {
            hud.hideComponent(launcher);launcher.setPosition(900,500);
            check(AlarmClockMod.command("alarmclockshow"),"Console recovery command recognized");
            check(hud.isComponentEnabled(launcher)&&launcher.x==12&&launcher.y==370&&!hud.isComponentEnabled(window),"Console recovery shows and resets icon without opening list");
            check(AlarmClockMod.command("alarmclock")&&hud.isComponentEnabled(window),"Existing console command opens list");
            header.componentResized();click(close);
            hud.hideComponent(launcher);AlarmClockMod.onHudReady(hud);AlarmClockMod.onHudReady(hud);
            check(hud.isComponentEnabled(launcher)&&Collections.frequency(hud.getComponents(),launcher)==1,"Repeated HUD readiness restores one icon");
            check(hud.mainMenu.getMenuSet().size()==1&&hud.hudSettings.getMenuSet().size()==1,"Repeated readiness does not duplicate menus");
            check(!AlarmClockMod.command("unrelated"),"Unrelated console input untouched");
        } finally { instance.set(null,previous);bridge.dispose(); }

        launcher.restorePositionHints(new WindowPosition(900,500,1));
        check(launcher.x==900&&launcher.y==500&&launcher.createPositionHints().flags==1,"Custom saved position and drag lock preserved");
        launcher.restorePositionHints(new WindowPosition(1690,100,0));
        check(launcher.x==12&&launcher.y==370,"Original right-side default migrates to visible left-side position");
        launcher.restorePositionHints(new WindowPosition(5000,-90,0));
        check(launcher.x>=0&&launcher.y>=0&&launcher.x+launcher.width<=1920&&launcher.y+launcher.height<=1080,"Off-screen saved icon clamped to screen");
        WurmComponent.SCREEN_WIDTH=320;WurmComponent.SCREEN_HEIGHT=240;launcher.moveToDefault();
        check(launcher.x+launcher.width<=320&&launcher.y+launcher.height<=240,"Default also stays inside a small viewport");
        WurmComponent.SCREEN_WIDTH=1920;WurmComponent.SCREEN_HEIGHT=1080;

        SavePosManager positions=(SavePosManager)field(HeadsUpDisplay.class,"savePosManager").get(hud);
        Properties saved=(Properties)field(SavePosManager.class,"rawPositions").get(positions);
        saved.setProperty("alarmclock.launcher",new WindowPosition(1690,100,0).toString());
        AlarmClockBridge restored=new AlarmClockBridge(hud,engine,()->false);
        AlarmClockLauncher restoredIcon=(AlarmClockLauncher)field(AlarmClockBridge.class,"launcher").get(restored);
        check(hud.isComponentEnabled(restoredIcon)&&restoredIcon.x==12&&restoredIcon.y==370,"Native SavePosManager restores/migrates icon before default display");
        restored.dispose();
    }
    private static WButton activityChoice(AlarmClockWindow window,String id) throws Exception {
        Object body=field(AlarmClockWindow.class,"body").get(window);
        Object list=field(body.getClass(),"list").get(body);
        for(Object card:(java.util.List<?>)field(list.getClass(),"cards").get(list)) {
            WButton choice=(WButton)field(card.getClass(),"autoRestart").get(card);
            Alarm alarm=(Alarm)field(card.getClass(),"alarm").get(card);
            if(choice!=null&&alarm.id.equals(id)) return choice;
        }
        throw new AssertionError(id+" checkbox missing");
    }
    private static void verifyAutoRestartChoiceBounds(AlarmClockWindow window) throws Exception {
        WButton prayer=activityChoice(window,"prayer"), meditation=activityChoice(window,"meditation");
        check(field(prayer.getClass(),"baseline").getInt(prayer)==field(meditation.getClass(),"baseline").getInt(meditation),"Activity checkbox group shares baseline");
        for(WButton choice:new WButton[]{prayer,meditation}) {
        String caption=choice.getLabel();
        int baseline=field(choice.getClass(),"baseline").getInt(choice);
        for(boolean bold:new boolean[]{false,true}) {
            check(UiTypography.width(caption,16,bold,UiDensity.HIGH)<=choice.width-26,"Activity checkbox caption fits both weights: "+Messages.language()+" "+caption);
            Rectangle ink=UiTypography.ink(caption,16,bold,UiDensity.HIGH);
            check(baseline+ink.y>=2&&baseline+ink.y+ink.height<=choice.height-2,"Activity checkbox glyph ink clears top/bottom: "+Messages.language()+" baseline="+baseline+" height="+choice.height+" ink="+ink+" bold="+bold+" caption="+caption);
        }
        Object card=choice.parent;
        java.util.List<?> buttons=(java.util.List<?>)field(card.getClass(),"buttons").get(card);
        check(choice.y+choice.height<=((WButton)buttons.get(0)).y,"Activity checkbox clears row actions");
        check(choice.x>=window.x&&choice.x+choice.width<=window.x+window.width,"Activity checkbox stays inside window");
        }
    }
    private static void verifyActivityCompletionCallbacks() throws Exception {
        AlarmClockMod mod=new AlarmClockMod();
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->{},()->{});
        Alarm prayer=engine.alarms().get(2);engine.restart(prayer,1000);
        Alarm meditation=engine.alarms().get(3);engine.restart(meditation,1000);
        field(AlarmClockMod.class,"engine").set(mod,engine);
        Field instance=field(AlarmClockMod.class,"instance");Object previous=instance.get(null);instance.set(null,mod);
        Method apply=AlarmClockMod.class.getDeclaredMethod("applyCompletedActivities",HeadsUpDisplay.class,boolean.class);apply.setAccessible(true);
        HeadsUpDisplay hud=WurmComponent.hud;
        try {
            long old=engine.dueAt(prayer);
            AlarmClockMod.serverMessage(hud,":Event","You start to pray at the altar of Fo.");apply.invoke(mod,hud,true);
            check(engine.dueAt(prayer)==old,"Start event does not restart prayer");
            long before=System.currentTimeMillis();AlarmClockMod.serverMessage(hud,":Event","You finish your prayer to Fo.");
            check(engine.dueAt(prayer)==old,"Inbound callback queues completion until the HUD thread handles it");
            apply.invoke(mod,hud,true);
            check(engine.dueAt(prayer)>=before+prayer.delayMillis&&engine.dueAt(prayer)<=System.currentTimeMillis()+prayer.delayMillis,"Successful completion starts next interval on HUD thread");
            old=engine.dueAt(prayer);
            AlarmClockMod.serverMessage(hud,":Event","You finish your prayer to Fo.");AlarmClockMod.connectionEnded();apply.invoke(mod,hud,true);
            check(engine.dueAt(prayer)==old,"Disconnect clears pending prayer from previous session");
            AlarmClockMod.serverMessage(hud,":Event","You finish your prayer to Fo.");apply.invoke(mod,allocate(HeadsUpDisplay.class),true);
            check(engine.dueAt(prayer)==old,"Completion from another HUD is ignored");
            AlarmClockMod.serverMessage(hud,":Event","You finish your prayer to Fo.");apply.invoke(mod,hud,false);
            check(engine.dueAt(prayer)==old,"Disconnected client does not apply completion");
            old=engine.dueAt(meditation);long prayerDue=engine.dueAt(prayer);
            for(String message:new String[]{"You start meditating.","You fall into a trance.","You stop meditating.","Barefoot finishes his meditation."}) {
                AlarmClockMod.serverMessage(hud,":Event",message);apply.invoke(mod,hud,true);
                check(engine.dueAt(meditation)==old,"Only successful player meditation resets the alarm");
            }
            before=System.currentTimeMillis();AlarmClockMod.serverMessage(hud,":Event","You finish your meditation.");
            check(engine.dueAt(meditation)==old,"Meditation completion is queued for the HUD thread");apply.invoke(mod,hud,true);
            check(engine.dueAt(meditation)>=before+1_800_000&&engine.dueAt(prayer)==prayerDue,"Meditation starts a new 30-minute interval without restarting prayer");
            old=engine.dueAt(meditation);
            AlarmClockMod.serverMessage(hud,":Event","You finish your meditation.");AlarmClockMod.connectionEnded();apply.invoke(mod,hud,true);
            check(engine.dueAt(meditation)==old,"Disconnect clears pending meditation");
            AlarmClockMod.serverMessage(hud,":Event","You finish your meditation.");apply.invoke(mod,allocate(HeadsUpDisplay.class),true);
            check(engine.dueAt(meditation)==old,"Meditation from another HUD is ignored");
            AlarmClockMod.serverMessage(hud,":Event","You finish your meditation.");apply.invoke(mod,hud,false);
            check(engine.dueAt(meditation)==old,"Disconnected client does not apply meditation");
            engine.restart(prayer,1000);engine.restart(meditation,1000);before=System.currentTimeMillis();
            AlarmClockMod.serverMessage(hud,":Event","You finish your prayer to Fo.");
            AlarmClockMod.serverMessage(hud,":Event","You finish your meditation.");apply.invoke(mod,hud,true);
            check(engine.dueAt(prayer)>=before+1_200_000&&engine.dueAt(meditation)>=before+1_800_000,"Different completions in one HUD tick are both retained");
        } finally { instance.set(null,previous); }
    }
    private static void verifyRingingOverlay() throws Exception {
        AlarmClockAlert alert=new AlarmClockAlert();Alarm meditation=AlarmEngine.defaults().get(3);
        alert.ring(meditation);check(alert.tick(System.currentTimeMillis()-500),"Ring starts overlay");
        verifyOverlayInput(alert);
        preview("ringing-meditation",alert);
        check(labels.isEmpty(),"Ringing animation has no small caption plate");
        long start=field(AlarmClockAlert.class,"started").getLong(alert);
        previewTime=start+600;BufferedImage first=snapshot(alert,1f);
        previewTime=start+700;check(!Arrays.equals(pixels(first),pixels(snapshot(alert,1f))),"Clock visibly moves between animation frames");
        check(!alert.tick(start+3000),"Overlay automatically disappears after three seconds");
        alert.ring(meditation);alert.clear();check(!alert.tick(start+4000),"Disconnect clears active and pending animation");
        alert.ring(AlarmEngine.defaults().get(4));check(alert.tick(System.currentTimeMillis()),"Favor starts its ringing overlay");
        preview("ringing-favor",alert);alert.clear();
    }
    private static void verifyNotificationColors() throws Exception {
        AlarmClockMod mod=new AlarmClockMod();
        Map<String,Long> deadlines=new HashMap<String,Long>();deadlines.put("prayer",5000L);deadlines.put("meditation",6000L);
        ReminderOptions options=new ReminderOptions();options.sound=false;options.animation=false;
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),mod::notifyPlayer,()->{},options,deadlines);
        AlarmClockBridge bridge=new AlarmClockBridge(WurmComponent.hud,engine,()->false);
        field(AlarmClockMod.class,"bridge").set(mod,bridge);
        AlarmClockScreenMessages messages=(AlarmClockScreenMessages)field(AlarmClockBridge.class,"messages").get(bridge);
        try {
            verifyOverlayInput(messages);
            int eventStart=eventMessages.size();
            engine.tick(1000,true,.20f,Float.NaN);
            verifySystemMessage(messages,"Eat, you have 20% food left.",new Color(1f,.94f,.60f),"food");
            messages.clear();engine.tick(2000,true,Float.NaN,.20f);
            verifySystemMessage(messages,"Drink, you have 20% water left.",new Color(1f,.94f,.60f),"water");
            messages.clear();engine.tick(5000,true,Float.NaN,Float.NaN);
            verifySystemMessage(messages,"It's time to pray.",new Color(1f,.94f,.60f),"prayer");
            messages.clear();engine.tick(6000,true,Float.NaN,Float.NaN);
            verifySystemMessage(messages,"It's time to meditate.",new Color(.55f,.85f,1f),"meditation");
            messages.clear();engine.tick(6250,true,Float.NaN,Float.NaN,65,65);
            verifySystemMessage(messages,engine.alarms().get(4).message,new Color(.60f,.93f,.72f),"favor");
            check(eventMessages.size()==eventStart+5,"Each fired reminder is duplicated in Event exactly once even with sound/animation off");
            for(Alarm alarm:engine.alarms()) {
                alarm.name=alarm.message="Same editable message";messages.clear();mod.notifyPlayer(alarm,alarm.message);
                snapshot(messages,1f);Alarm.ReminderColor color=alarm.reminderColor();
                check(labelColors.get(alarm.message).equals(new Color(color.red,color.green,color.blue)),"Renaming retains reminder color: "+alarm.id);
                check(eventColors.get(eventColors.size()-1).equals(new Color(color.red,color.green,color.blue)),"Event copy retains the reminder color: "+alarm.id);
            }
            Alarm timer=Alarm.create(Alarm.Kind.TIMER,"Custom","A custom timer reminder.");
            mod.notifyPlayer(timer,timer.message);
            check(eventMessages.get(eventMessages.size()-1).equals(timer.message),"Custom timers are also copied to the shared Event");
            int eventCount=eventMessages.size();bridge.ring(timer);
            check(eventMessages.size()==eventCount,"Sound previews/animation do not duplicate Event text");
            verifyOverlayInput(messages);
            check(WurmComponent.hud.getComponents().contains(messages),"Text remains attached with sound and animation disabled");
            bridge.stopEffects();snapshot(messages,1f);check(labels.isEmpty(),"Disconnect clears all native system messages");
        } finally { bridge.dispose(); }
        check(!WurmComponent.hud.getComponents().contains(messages),"Disposing the HUD removes the system-message renderer");
    }
    private static void verifySystemMessage(AlarmClockScreenMessages messages,String text,Color color,String name) throws Exception {
        preview("reminder-"+name,messages);
        check(labels.containsKey(text)&&labelSizes.get(text)>=32,"Full reminder uses the large native system font: "+name);
        check(labelColors.get(text).equals(color),"System text has the alarm color: "+name);
        check(eventMessages.get(eventMessages.size()-1).equals(text)&&eventColors.get(eventColors.size()-1).equals(color),"Event receives the same complete text and color: "+name);
        TextFont font=(TextFont)field(AlarmClockScreenMessages.class,"font").get(messages);
        Point position=labels.get(text);
        check(position.x==(WurmComponent.SCREEN_WIDTH-font.getWidth(text))/2&&position.y==WurmComponent.SCREEN_HEIGHT/4,"Native reminder is centered at the system-message position");
        OnScreenMessageViewer viewer=(OnScreenMessageViewer)field(AlarmClockScreenMessages.class,"viewer").get(messages);
        @SuppressWarnings("unchecked") java.util.List<OnScreenMessage> pending=(java.util.List<OnScreenMessage>)field(OnScreenMessageViewer.class,"messageList").get(viewer);
        check(pending.size()==1&&pending.get(0).getMessage().equals(text),"Real Wurm message viewer receives the complete reminder");
        pending.get(0).prepareRender(4f,position.x,position.y);
        viewer.prepareRender(font,WurmComponent.SCREEN_WIDTH,WurmComponent.SCREEN_HEIGHT);
        snapshot(messages,1f);check(labels.isEmpty(),"Native reminder fades out after three seconds");
    }
    private static void verifyOverlayInput(StaticComponent overlay) throws Exception {
        HeadsUpDisplay previous = WurmComponent.hud;
        try {
            HeadsUpDisplay inputHud = fixtureHud();
            field(HeadsUpDisplay.class,"visible").setBoolean(inputHud,true);
            int px = overlay.x + overlay.width / 2, py = overlay.y + overlay.height / 2;
            inputHud.getComponents().add(overlay);
            check(inputHud.getComponentAt(px,py)==null,"Decorative overlay leaves world hover available");
            check(!inputHud.mousePressed(px,py,0,1),"Left click reaches the world without a null focus crash");
            check(!inputHud.mousePressed(px,py,1,1),"Right click reaches the world without a null focus crash");
            OverlayInputTarget target = new OverlayInputTarget(px-20,py-20);
            inputHud.getComponents().add(0,target);
            check(inputHud.getComponentAt(px,py)==target,"Hover reaches the control behind the overlay");
            check(inputHud.mousePressed(px,py,0,1)&&target.leftPresses==1,"Native left press reaches the underlying control");
            check(field(HeadsUpDisplay.class,"mouseFocusComponent").get(inputHud)==target,"Underlying control receives native mouse focus");
            inputHud.getComponents().remove(overlay);inputHud.getComponents().add(overlay);
            check(inputHud.mousePressed(px,py,1,1)&&target.rightPresses==1,"Native right press reaches the underlying control");
        } finally { WurmComponent.hud = previous; }
    }
    private static final class OverlayInputTarget extends StaticComponent {
        int leftPresses, rightPresses;
        OverlayInputTarget(int x,int y) { super("Overlay input target",x,y,40,40); }
        @Override protected void leftPressed(int x,int y,int clicks) { leftPresses++; }
        @Override protected void rightPressed(int x,int y,int clicks) { rightPresses++; }
        @Override protected void renderComponent(Queue queue,float alpha) {}
    }
    private static void verifyFavorEditor(AlarmEngine engine) throws Exception {
        AlarmClockEditor editor=new AlarmClockEditor(engine.alarms().get(4),engine,()->{});
        editor.setLocation(20,20,770,390);
        Object body=field(AlarmClockEditor.class,"body").get(editor);
        ChamomiloUiV1DropDown kind=(ChamomiloUiV1DropDown)field(body.getClass(),"kind").get(body);
        check(kind.selectedIndex()==3,"Favor retains its type in the editor");
        for(String key:new String[]{"delay","threshold","stop","repeat"}) {
            Object wrapper=field(body.getClass(),key).get(body);
            WurmInputField input=(WurmInputField)field(ChamomiloUiV1TextField.class,"input").get(wrapper);
            check(input.enabled==key.equals("repeat"),"Favor edits repeat interval, with automatic full-Favor threshold");
        }
        for(String language:new String[]{"en","pt-BR","de","ru"}) {
            Messages.select(language);editor.relocalize();verifyEditorBounds(editor);
            preview("favor-editor-"+language,editor);
            for(boolean bold:new boolean[]{false,true}) {
                check(UiTypography.width(Messages.t("favorRepeatField"),16,bold,UiDensity.LOW)<=(editor.width-46)/2,"Favor repeat label fits "+language);
                check(UiTypography.width(Messages.t("favorHint"),16,bold,UiDensity.LOW)<=editor.width-26,"Favor explanation fits "+language);
            }
        }
        Messages.select("en");editor.relocalize();
        ChamomiloUiV1TextField message=(ChamomiloUiV1TextField)field(body.getClass(),"message").get(body);
        message.setText("Time for a spell.");click((WButton)field(body.getClass(),"save").get(body));
        Alarm favor=engine.alarms().get(4);
        check(favor.kind==Alarm.Kind.FAVOR&&favor.message.equals("Time for a spell.")&&favor.repeatMillis==60_000,"Saving Favor keeps the monitor and its one-minute repeat");
        favor.message="Favor is full. It's time to cast a spell.";
    }
    private static void verifyPopup(ChamomiloUiV1DropDown kind) throws Exception {
        kind.leftPressed(kind.x+12,kind.y+12,1);
        java.util.List<WurmDropdownPopup> popups=(java.util.List<WurmDropdownPopup>)field(HeadsUpDisplay.class,"dropdownPopups").get(WurmComponent.hud);
        check(popups.size()==1,"Type choice opens native detached popup");
        WurmDropdownPopup popup=popups.get(0);preview("type-popup",popup);WurmComponent.hud.clearAllPopups();
    }
    private static void verifyNeedsEditors() throws Exception {
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->{},()->{});
        for(int alarmIndex:new int[]{0,1}) {
            int[] closed={0};Alarm alarm=engine.alarms().get(alarmIndex);
            AlarmClockEditor editor=new AlarmClockEditor(alarm,engine,()->closed[0]++);
            editor.setLocation(20,20,770,390);
            Object body=field(AlarmClockEditor.class,"body").get(editor);
            // Hidden values must never be parsed as settings for these monitors.
            ((ChamomiloUiV1TextField)field(body.getClass(),"delay").get(body)).setText("unused");
            ((ChamomiloUiV1TextField)field(body.getClass(),alarmIndex==0?"repeat":"stop").get(body)).setText("unused");
            for(String language:new String[]{"en","pt-BR","de","ru"}) {
                Messages.select(language);editor.relocalize();
                editor.setLocation(20,20,740,385);verifyEditorBounds(editor);
                preview(alarm.id+"-editor-"+language,editor);
            }
            click((WButton)field(body.getClass(),"save").get(body));
            check(closed[0]==1&&engine.alarms().get(alarmIndex).threshold==(alarmIndex==0?20:50),"Monitor Save parses only its active parameters");
            if(alarmIndex==1)check(engine.alarms().get(1).repeatMillis==60_000,"Water editor retains minute repeats without a lower cutoff");
        }
        Messages.select("en");
    }
    private static void verifyEditorTypeSwitching() throws Exception {
        AlarmEngine engine=new AlarmEngine(AlarmEngine.defaults(),(a,m)->{},()->{});
        AlarmClockEditor editor=new AlarmClockEditor(null,engine,()->{});editor.setLocation(20,20,740,385);
        Object body=field(AlarmClockEditor.class,"body").get(editor);
        ChamomiloUiV1DropDown kind=(ChamomiloUiV1DropDown)field(body.getClass(),"kind").get(body);
        kind.selectIndex(2);
        check(((ChamomiloUiV1TextField)field(body.getClass(),"threshold").get(body)).value().equals("50"),"New Water alarms start at the 50% threshold");
        String[] keys={"name","delay","repeat","threshold","stop","message"};
        String[] values={"Unsaved alarm","23","2","25","7","Keep my message"};
        for(int i=0;i<keys.length;i++)((ChamomiloUiV1TextField)field(body.getClass(),keys[i]).get(body)).setText(values[i]);
        for(String language:new String[]{"en","pt-BR","de","ru"}) for(int type:new int[]{0,1,2,3,0}) {
            kind.selectIndex(type);Messages.select(language);editor.relocalize();verifyEditorBounds(editor);
            for(int i=0;i<keys.length;i++) check(((ChamomiloUiV1TextField)field(body.getClass(),keys[i]).get(body)).value().equals(values[i]),"Switching type/language retains unsaved visible and hidden input values");
        }
        Messages.select("en");
    }
    private static void verifyEditorBounds(AlarmClockEditor editor) throws Exception {
        Object body=field(AlarmClockEditor.class,"body").get(editor);
        java.util.List<?> controls=(java.util.List<?>)field(body.getClass(),"controls").get(body);
        for(Object control:controls) {
            WurmComponent c=(WurmComponent)control;
            check(c.x>=editor.x&&c.y>=editor.y&&c.x+c.width<=editor.x+editor.width&&c.y+c.height<=editor.y+editor.height,"Editor input/action stays inside window");
        }
        ChamomiloSkinnedButton save=(ChamomiloSkinnedButton)field(body.getClass(),"save").get(body);
        ChamomiloSkinnedButton cancel=(ChamomiloSkinnedButton)field(body.getClass(),"cancel").get(body);
        check(save.captionFontPixels()==cancel.captionFontPixels()&&save.captionBaseline()==cancel.captionBaseline(),"Editor footer group shares typography");
        int type=((ChamomiloUiV1DropDown)field(body.getClass(),"kind").get(body)).selectedIndex();
        for(String key:new String[]{"delay","repeat","threshold","stop"}) {
            WurmComponent wrapper=(WurmComponent)field(body.getClass(),key).get(body);
            boolean shown=key.equals("delay")?type==0:key.equals("repeat")?type==0||type==2||type==3:key.equals("threshold")?type==1||type==2:type==1;
            WurmInputField input=(WurmInputField)field(ChamomiloUiV1TextField.class,"input").get(wrapper);
            check(controls.contains(wrapper)==shown&&input.enabled==shown,"Unused parameters are absent from drawing/picking and disabled for keyboard entry: "+key);
            if(!shown) {
                WurmComponent picked=((FlexComponent)body).getComponentAt(wrapper.x+5,wrapper.y+5);
                for(WurmComponent node=picked;node!=null;node=node.parent)check(node!=wrapper,"Hidden input cannot be picked");
            }
        }
        snapshot(editor,1f);
        check(labels.containsKey(Messages.t("delay"))==(type==0),"Wait label only appears for timers");
        check(labels.containsKey(Messages.t("repeatField"))==(type==0),"Timer repeat label only appears for timers");
        check(labels.containsKey(Messages.t("favorRepeatField"))==(type==2||type==3),"Monitor repeat label appears for Water/Favor");
        check(labels.containsKey(Messages.t("thresholdField"))==(type==1),"Food uses its percentage threshold");
        check(labels.containsKey(Messages.t("waterThresholdField"))==(type==2),"Water uses a strictly-below threshold");
        check(labels.containsKey(Messages.t("stopField"))==(type==1),"Only Food has a lower cutoff");
        int column=(((FlexComponent)body).width-20)/2;
        java.util.List<String> captions=new ArrayList<String>(Arrays.asList("name","kind"));
        if(type==0)captions.addAll(Arrays.asList("delay","repeatField"));
        else if(type==3)captions.add("favorRepeatField");else if(type==2)captions.addAll(Arrays.asList("waterThresholdField","favorRepeatField"));else captions.addAll(Arrays.asList("thresholdField","stopField"));
        String hint=type==0?"timerHint":type==3?"favorHint":type==2?"waterHint":"needsHint";
        for(boolean bold:new boolean[]{false,true}) {
            for(String key:captions)check(UiTypography.width(Messages.t(key),16,bold,UiDensity.LOW)<=column,"Active parameter caption fits narrow layout: "+Messages.language()+" "+key);
            check(UiTypography.width(Messages.t(hint),16,bold,UiDensity.LOW)<=((FlexComponent)body).width,"Type-specific explanation fits narrow layout: "+Messages.language());
        }
    }
    private static void verifyHookContracts() throws Exception {
        // Check actual pinned client descriptors and production hook code without a live server.
        javassist.ClassPool pool=new javassist.ClassPool(true);
        pool.get("com.wurmonline.client.game.PlayerObj").getMethod("setHunger","(FB)V");
        pool.get("com.wurmonline.client.game.PlayerObj").getMethod("setHunger","(FBBBBB)V");
        pool.get("com.wurmonline.client.game.PlayerObj").getMethod("setThirst","(F)V");
        pool.get("com.wurmonline.client.renderer.gui.HeadsUpDisplay").getMethod("addOnscreenMessage","(Ljava/lang/String;FFFB)V");
    }
    private static void click(WButton button) {button.leftPressed(button.x+10,button.y+10,1);button.leftReleased(button.x+10,button.y+10);}
    private static void preview(String name,WurmComponent component) throws Exception {
        previewTime=System.currentTimeMillis();
        for(WurmComponent node:tree(component)) {
            if(node instanceof ChamomiloUiV1Button)((ChamomiloUiV1Button)node).setAnimationsEnabled(false);
            if(node instanceof ChamomiloSkinnedButton)((ChamomiloSkinnedButton)node).setAnimationsEnabled(false);
            if(node.getClass().getSimpleName().equals("ChoiceCheckBox"))((UiButtonMotion)field(node.getClass(),"motion").get(node)).setAnimationsEnabled(false);
            if(node instanceof ContainerComponent)((ContainerComponent)node).layout();
        }
        BufferedImage expected=snapshot(component,1f);
        for(float alpha:new float[]{0f,.2f,.7f})check(Arrays.equals(pixels(expected),pixels(snapshot(component,alpha))),name+" is stable for HUD alpha "+alpha);
        ImageIO.write(expected,"png",output.resolve(name+".png").toFile());previews++;
    }
    private static java.util.List<WurmComponent> tree(WurmComponent component) throws Exception {
        java.util.List<WurmComponent> result=new ArrayList<WurmComponent>();
        visit(component,Collections.newSetFromMap(new IdentityHashMap<WurmComponent,Boolean>()),result);return result;
    }
    private static void visit(WurmComponent component,Set<WurmComponent> seen,java.util.List<WurmComponent> result) throws Exception {
        if(component==null||!seen.add(component))return;result.add(component);
        for(Class<?> type=component.getClass();type!=null&&type!=WurmComponent.class;type=type.getSuperclass())for(Field f:type.getDeclaredFields()) {
            if(Modifier.isStatic(f.getModifiers())||f.getName().equals("parent")||f.getName().startsWith("this$"))continue;
            f.setAccessible(true);Object value=f.get(component);
            if(value instanceof WurmComponent)visit((WurmComponent)value,seen,result);
            if(value instanceof Iterable)for(Object child:(Iterable<?>)value)if(child instanceof WurmComponent)visit((WurmComponent)child,seen,result);
            if(value instanceof WurmComponent[])for(WurmComponent child:(WurmComponent[])value)visit(child,seen,result);
        }
    }
    private static BufferedImage snapshot(WurmComponent component,float alpha) {
        graphics.setClip(null);rect(.03f,.028f,.022f,1f,0,0,canvas.getWidth(),canvas.getHeight());labels.clear();labelSizes.clear();labelColors.clear();component.render(null,alpha);
        BufferedImage result=new BufferedImage(component.width,component.height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D copy=result.createGraphics();copy.drawImage(canvas,0,0,component.width,component.height,component.x,component.y,component.x+component.width,component.y+component.height,null);copy.dispose();return result;
    }
    private static int[] pixels(BufferedImage image){return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());}
    private static HeadsUpDisplay fixtureHud() throws Exception {
        HeadsUpDisplay hud=(HeadsUpDisplay)allocate(HeadsUpDisplay.class);
        field(HeadsUpDisplay.class,"width").setInt(hud,1920);field(HeadsUpDisplay.class,"height").setInt(hud,1080);
        field(HeadsUpDisplay.class,"components").set(hud,new ArrayList<WurmComponent>());
        field(HeadsUpDisplay.class,"popups").set(hud,new ArrayList<WurmPopup>());
        field(HeadsUpDisplay.class,"dropdownPopups").set(hud,new ArrayList<WurmDropdownPopup>());
        field(HeadsUpDisplay.class,"itemPlateComponent").set(hud,allocate(WurmItemPlate.class));
        field(HeadsUpDisplay.class,"chatManager").set(hud,allocate(ChatManagerManager.class));
        WurmComponent.SCREEN_WIDTH=1920;WurmComponent.SCREEN_HEIGHT=1080;WurmComponent.hud=hud;
        field(HeadsUpDisplay.class,"mainMenu").set(hud,new MainMenu());
        field(HeadsUpDisplay.class,"hudSettings").set(hud,new HudSettings());
        field(HeadsUpDisplay.class,"savePosManager").set(hud,new SavePosManager());return hud;
    }
    public static Object allocate(Class<?> type) throws Exception {Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);return ((sun.misc.Unsafe)f.get(null)).allocateInstance(type);}
    public static com.wurmonline.client.options.MultiOption guiSkin() throws Exception {
        com.wurmonline.client.options.MultiOption skin=(com.wurmonline.client.options.MultiOption)allocate(com.wurmonline.client.options.MultiOption.class);
        field(com.wurmonline.client.options.MultiOption.class,"options").set(skin,new String[]{"Default"});return skin;
    }
    private static Field field(Class<?> type,String name) throws Exception {Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static boolean clip(int x,int y,int w,int h){clips.push(graphics.getClip()==null?new Rectangle(0,0,canvas.getWidth(),canvas.getHeight()):graphics.getClip());graphics.clipRect(x,y,w,h);return w>0&&h>0;}
    public static void unclip(){graphics.setClip(clips.pop());}
    public static void rect(float r,float g,float b,float a,int x,int y,int w,int h){graphics.setColor(new Color(r,g,b,a));graphics.fillRect(x,y,w,h);}
    public static void button(WButton button){}
    public static boolean texture(UiAsset asset,float tint,float alpha,int x,int y,int w,int h,float u0,float v0,float u1,float v1) throws Exception {
        check(alpha==1f,"Texture opacity must ignore incoming HUD alpha");BufferedImage source=assets.get(asset);
        if(source==null){try(java.io.InputStream input=UiResources.open(asset)){source=ImageIO.read(input);}assets.put(asset,source);}
        String key=asset.name()+":"+Float.floatToIntBits(tint);BufferedImage tinted=tintedAssets.get(key);
        if(tinted==null){tinted=new java.awt.image.RescaleOp(new float[]{tint,tint,tint,1f},new float[4],null).filter(source,null);tintedAssets.put(key,tinted);}
        graphics.drawImage(tinted,x,y,x+w,y+h,Math.round(u0*source.getWidth()),Math.round(v0*source.getHeight()),Math.round(u1*source.getWidth()),Math.round(v1*source.getHeight()),null);return true;
    }
    public static TextFont captionFont(int pixels,boolean bold,UiDensity density){return new ProbeFont(UiTypography.font(pixels,bold,density),true);}
    public static final class ProbeFont extends TextFont {
        private final Font font;private final boolean characterAdvances;private int x,y;
        public ProbeFont(String name){this(new Font("Verdana",name.equals("bold")?Font.BOLD:Font.PLAIN,12),false);}
        public ProbeFont(Font font,boolean characterAdvances){this.font=font;this.characterAdvances=characterAdvances;}
        public void moveTo(int x,int y){this.x=x;this.y=y;}
        public int paint(Queue queue,String value,float r,float g,float b,float a){
            if(characterAdvances||font.getSize()<32)check(a==1f,"Text opacity must ignore incoming HUD alpha");
            labels.put(value,new Point(x,y));labelSizes.put(value,font.getSize());labelColors.put(value,new Color(r,g,b,a));graphics.setFont(font);graphics.setColor(new Color(r,g,b,a));
            if(characterAdvances){int position=x;for(char c:value.toCharArray()){graphics.drawString(String.valueOf(c),position,y);position+=metrics().charWidth(c);}}
            else graphics.drawString(value,x,y);return getWidth(value);
        }
        private FontMetrics metrics(){return graphics.getFontMetrics(font);}
        public int getWidth(String value){if(!characterAdvances)return metrics().stringWidth(value);int result=0;for(char c:value.toCharArray())result+=metrics().charWidth(c);return result;}
        public int getWidth(char[] value,int start,int length){return getWidth(new String(value,start,length));}
        public int getHeight(){return metrics().getHeight();}public int getAscent(){return metrics().getAscent();}public int getDescent(){return metrics().getDescent();}public int getLeading(){return metrics().getLeading();}
    }
}
