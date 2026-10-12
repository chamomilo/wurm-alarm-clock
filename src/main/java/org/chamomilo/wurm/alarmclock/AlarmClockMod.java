package org.chamomilo.wurm.alarmclock;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.renderer.gui.AlarmClockBridge;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import javassist.ClassPool;
import javassist.CtClass;
import org.chamomilo.wurm.update.SharedLanguageCoordinator;
import org.chamomilo.wurm.update.SharedUpdateCoordinator;
import org.chamomilo.wurm.update.SharedUpdateHooks;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;

public final class AlarmClockMod implements WurmClientMod, PreInitable, Initable, ModListener, SharedLanguageCoordinator.Participant {
    public static final String VERSION = "0.2.8";
    public static final FavorReadings FAVOR_READINGS = new FavorReadings();
    private static final Logger LOG = Logger.getLogger(AlarmClockMod.class.getName());
    private static volatile AlarmClockMod instance;
    private final AtomicReference<CompletedActivity> pendingPrayer = new AtomicReference<CompletedActivity>();
    private final AtomicReference<CompletedActivity> pendingMeditation = new AtomicReference<CompletedActivity>();
    private final AlarmStore store = new AlarmStore(Paths.get(System.getProperty("user.home"), ".chamomilo", "alarmclock.properties"));
    private AlarmEngine engine;
    private HeadsUpDisplay activeHud;
    private AlarmClockBridge bridge;
    private static PlayerObj foodPlayer, waterPlayer;
    private long lastTick;
    private boolean wasConnected;
    private boolean saveFailed;
    private boolean protectUnreadableSettings;
    private boolean runtimeFailureLogged;

    @Override public void preInit() {
        instance = this;
        SharedUpdateHooks.install();
        install("HUD", () -> {
            ClassPool pool = HookManager.getInstance().getClassPool();
            CtClass hud = pool.getCtClass("com.wurmonline.client.renderer.gui.HeadsUpDisplay");
            hud.getMethod("init", "(II)V").insertAfter("org.chamomilo.wurm.alarmclock.AlarmClockMod.onHudReady(this);");
            hud.getMethod("gameTick", "()V").insertAfter("org.chamomilo.wurm.alarmclock.AlarmClockMod.onHudTick(this);");
        });
        install("player readings", () -> {
            CtClass player = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.client.game.PlayerObj");
            player.getMethod("setHunger", "(FB)V").insertAfter("org.chamomilo.wurm.alarmclock.AlarmClockMod.foodUpdated(this);");
            player.getMethod("setHunger", "(FBBBBB)V").insertAfter("org.chamomilo.wurm.alarmclock.AlarmClockMod.foodUpdated(this);");
            player.getMethod("setThirst", "(F)V").insertAfter("org.chamomilo.wurm.alarmclock.AlarmClockMod.waterUpdated(this);");
        });
        install("connection", () -> {
            CtClass connection = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.client.comm.SimpleServerConnectionClass");
            connection.getMethod("disconnect", "(Ljava/lang/String;)V").insertBefore("org.chamomilo.wurm.alarmclock.AlarmClockMod.connectionEnded();");
            connection.getMethod("disconnectAndConnectTo", "(Ljava/lang/String;I)V").insertBefore("org.chamomilo.wurm.alarmclock.AlarmClockMod.connectionEnded();");
        });
        install("favor readings", () -> {
            CtClass skills = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.client.game.SkillLogicSet");
            skills.getMethod("addSkill", "(Lcom/wurmonline/client/game/SkillLogic;)V")
                .insertAfter("org.chamomilo.wurm.alarmclock.AlarmClockMod.FAVOR_READINGS.added(this, $1);");
            skills.getMethod("updateSkill", "(JFB)V")
                .insertAfter("org.chamomilo.wurm.alarmclock.AlarmClockMod.FAVOR_READINGS.updated(this, $1, $2);");
            skills.getDeclaredMethod("clear").insertAfter("org.chamomilo.wurm.alarmclock.AlarmClockMod.FAVOR_READINGS.cleared(this);");
        });
        install("prayer and meditation completion", () -> {
            CtClass listener = HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.client.comm.ServerConnectionListenerClass");
            listener.getMethod("textMessage", "(Ljava/lang/String;FFFLjava/lang/String;B)V")
                .insertBefore("org.chamomilo.wurm.alarmclock.AlarmClockMod.serverMessage(this.world.getHud(), $1, $5);");
            listener.getMethod("textMessage", "(Ljava/lang/String;Ljava/util/List;B)V")
                .insertBefore("org.chamomilo.wurm.alarmclock.AlarmClockMod.serverMessage(this.world.getHud(), $1, org.chamomilo.wurm.alarmclock.PrayerMessages.text($2));");
        });
        install("console", () -> HookManager.getInstance().getClassPool().getCtClass("com.wurmonline.client.console.WurmConsole")
            .getMethod("handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z")
            .insertBefore("if (org.chamomilo.wurm.alarmclock.AlarmClockMod.command($1)) return true;"));
    }
    @Override public void init() {
        SharedUpdateHooks.registerHost("alarmclock");
        List<Alarm> definitions;
        try { definitions = store.load(); Messages.select(store.language()); }
        catch (Exception failure) {
            LOG.log(Level.WARNING, "Alarm settings could not be loaded", failure);
            try { LOG.info("Preserved unreadable settings: " + store.preserveUnreadable()); }
            catch (Exception backupFailure) {
                protectUnreadableSettings = true;
                LOG.log(Level.WARNING, "Cannot back up unreadable settings; saving is disabled to preserve the original", backupFailure);
            }
            definitions = AlarmEngine.defaults(); saveFailed = true;
        }
        engine = new AlarmEngine(definitions, this::notifyPlayer, this::save, store.options(),store.timerDeadlines());
    }
    @Override public void modInitialized(ModEntry<?> entry) { SharedUpdateCoordinator.modInitialized(entry); }
    @Override public String[] supportedLanguageCodes() { return new String[]{"en", "pt-BR", "de", "ru"}; }
    @Override public void setUserLanguage(String code) {
        if (!Messages.select(code)) return;
        if (bridge != null) bridge.relocalize();
        if (engine != null) save();
    }
    public AlarmEngine engine() { return engine; }
    public boolean saveFailed() { return saveFailed; }
    public void save() {
        if (protectUnreadableSettings) { saveFailed = true; return; }
        try { store.save(engine.alarms(), Messages.language(),engine.timerDeadlines()); saveFailed = false; }
        catch (Exception failure) {
            saveFailed = true; LOG.log(Level.WARNING, "Could not save Alarm Clock settings", failure);
        }
    }
    public void notifyPlayer(Alarm alarm, String message) {
        if (bridge != null) bridge.remind(alarm, message);
    }
    public static void foodUpdated(PlayerObj player) { foodPlayer = player; }
    public static void waterUpdated(PlayerObj player) { waterPlayer = player; }
    public static void serverMessage(HeadsUpDisplay hud, String channel, String message) {
        AlarmClockMod current = instance;
        if (current == null || hud == null) return;
        if (PrayerMessages.completed(channel, message))
            current.pendingPrayer.set(new CompletedActivity(hud, System.currentTimeMillis()));
        else if (MeditationMessages.completed(channel, message))
            current.pendingMeditation.set(new CompletedActivity(hud, System.currentTimeMillis()));
    }
    private static final class CompletedActivity {
        final HeadsUpDisplay hud;
        final long at;
        CompletedActivity(HeadsUpDisplay hud, long at) { this.hud = hud; this.at = at; }
    }
    private void applyCompletedActivities(HeadsUpDisplay hud, boolean connected) {
        CompletedActivity prayer = pendingPrayer.getAndSet(null);
        CompletedActivity meditation = pendingMeditation.getAndSet(null);
        if (!connected) return;
        if (prayer != null && prayer.hud == hud) engine.prayerCompleted(prayer.at);
        if (meditation != null && meditation.hud == hud) engine.meditationCompleted(meditation.at);
    }
    public static void connectionEnded() {
        foodPlayer = waterPlayer = null;
        FAVOR_READINGS.reset();
        if (instance != null) {
            instance.pendingPrayer.set(null);
            instance.pendingMeditation.set(null);
            if (instance.bridge != null) instance.bridge.stopEffects();
            if (instance.engine != null) { instance.engine.resetSamples(); instance.wasConnected = false; }
        }
    }
    public static void onHudReady(HeadsUpDisplay hud) {
        if (instance == null || instance.engine == null) return;
        if (instance.activeHud == hud && instance.bridge != null) { instance.bridge.showLauncher(false); return; }
        try {
            if (instance.bridge != null) instance.bridge.dispose();
            AlarmClockBridge readyBridge = new AlarmClockBridge(hud, instance);
            instance.activeHud = hud;
            instance.bridge = readyBridge;
            instance.lastTick = 0;
        } catch (Exception failure) { LOG.log(Level.SEVERE, "Cannot create Alarm Clock HUD", failure); }
    }
    public static void onHudTick(HeadsUpDisplay hud) {
        if (instance == null || instance.engine == null) return;
        try {
            if (instance.activeHud != hud || instance.bridge == null) onHudReady(hud);
            long now = System.currentTimeMillis();
            if (now - instance.lastTick < 250 && now >= instance.lastTick) return;
            instance.lastTick = now;
            boolean connected = ClientConnectionStatus.connected(hud.getWorld().getServerConnection());
            if (!connected && instance.wasConnected) connectionEnded();
            if (connected && !instance.wasConnected && instance.bridge != null) instance.bridge.showLauncher(false);
            instance.wasConnected = connected;
            // Process completion on the HUD thread before sending any overdue reminder.
            instance.applyCompletedActivities(hud, connected);
            PlayerObj player = hud.getWorld().getPlayer();
            FavorReadings.Sample favor = FAVOR_READINGS.read(player.getSkillSet());
            instance.engine.tick(now, connected,
                player == foodPlayer ? AlarmEngine.remaining(player.getHunger()) : Float.NaN,
                player == waterPlayer ? AlarmEngine.remaining(player.getThirst()) : Float.NaN,
                favor.favor, favor.faith);
            if (instance.bridge != null) {
                if (connected) instance.bridge.tick(now);
                else instance.bridge.stopEffects();
            }
        } catch (RuntimeException failure) {
            if (!instance.runtimeFailureLogged) { LOG.log(Level.SEVERE, "Alarm Clock tick failed", failure); instance.runtimeFailureLogged = true; }
        }
    }
    public static boolean command(String command) {
        boolean show = "alarmclockshow".equalsIgnoreCase(command);
        if (!show && !"alarmclock".equalsIgnoreCase(command)) return false;
        if (instance != null && instance.bridge != null) {
            instance.bridge.showLauncher(show);
            if (!show) instance.bridge.open();
        }
        return true;
    }
    private interface Hook { void run() throws Exception; }
    private static void install(String feature, Hook hook) {
        try { hook.run(); } catch (Exception failure) { LOG.log(Level.SEVERE, "Alarm Clock hook unavailable: " + feature, failure); }
    }
}
