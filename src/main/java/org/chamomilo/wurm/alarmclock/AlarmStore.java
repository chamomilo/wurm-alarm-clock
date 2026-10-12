package org.chamomilo.wurm.alarmclock;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Atomic settings outside the client installation. UTF-8 names/messages are preserved. */
public final class AlarmStore {
    private final Path path;
    private String language = "en";
    private final ReminderOptions options = new ReminderOptions();
    private final Map<String,Long> timerDeadlines=new HashMap<String,Long>();
    public AlarmStore(Path path) { this.path = path; }
    public String language() { return language; }
    public ReminderOptions options() { return options; }
    public Map<String,Long> timerDeadlines() { return Collections.unmodifiableMap(new HashMap<String,Long>(timerDeadlines)); }
    public Path preserveUnreadable() throws IOException {
        if (!Files.isRegularFile(path)) return null;
        Path backup = path.resolveSibling(path.getFileName() + ".unreadable-" + System.currentTimeMillis());
        Files.copy(path, backup);
        return backup;
    }
    public List<Alarm> load() throws IOException {
        timerDeadlines.clear();
        if (!Files.exists(path)) return AlarmEngine.defaults();
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(reader); }
        language = p.getProperty("language", "en");
        options.sound=Boolean.parseBoolean(p.getProperty("sound", "true"));
        options.animation=Boolean.parseBoolean(p.getProperty("animation", "true"));
        boolean legacyWater=!"below-threshold".equals(p.getProperty("waterReminderMode"));
        List<Alarm> alarms = new ArrayList<Alarm>();
        Map<String,Long> loadedDeadlines=new HashMap<String,Long>();
        try {
            int count = Integer.parseInt(p.getProperty("count", "0"));
            // Adding new built-ins must not make a previously full settings file unreadable.
            if (count < 0 || count > 100 + AlarmEngine.defaults().size()) throw new IllegalArgumentException("Invalid alarm count");
            Set<String> ids = new HashSet<String>();
            for (int i = 0; i < count; i++) {
                String key = "alarm." + i + ".";
                Alarm alarm = new Alarm(p.getProperty(key + "id"), Alarm.Kind.valueOf(p.getProperty(key + "kind")),
                        p.getProperty(key + "name"), p.getProperty(key + "message"));
                alarm.enabled = Boolean.parseBoolean(p.getProperty(key + "enabled", "true"));
                alarm.builtIn = AlarmType.builtIn(alarm.id)!=null;
                AlarmType type=alarm.type();
                alarm.autoRestartAfterPrayer = Boolean.parseBoolean(p.getProperty(key + "autoRestartAfterPrayer", Boolean.toString(alarm.isPrayer())));
                alarm.autoRestartAfterMeditation = Boolean.parseBoolean(p.getProperty(key + "autoRestartAfterMeditation", Boolean.toString(alarm.isMeditation())));
                alarm.threshold = integer(p,key+"threshold",alarm.threshold,type.has(AlarmType.Parameter.THRESHOLD));
                alarm.stopAt = integer(p,key+"stopAt",alarm.stopAt,type.has(AlarmType.Parameter.STOP));
                alarm.delayMillis = number(p,key+"delay",alarm.delayMillis,type.has(AlarmType.Parameter.DELAY));
                alarm.repeatMillis = number(p,key+"repeat",alarm.repeatMillis,type.has(AlarmType.Parameter.REPEAT)&&!(legacyWater&&alarm.kind==Alarm.Kind.WATER));
                if(legacyWater&&alarm.kind==Alarm.Kind.WATER) {
                    alarm.repeatMillis=type.defaultRepeatMillis;
                    if(alarm.builtIn)alarm.threshold=type.defaultThreshold;
                }
                if(type.has(AlarmType.Parameter.DELAY)) {
                    long due=number(p,key+"due",0,true);
                    if(due<0)throw new IllegalArgumentException("Invalid timer deadline.");
                    loadedDeadlines.put(alarm.id,due);
                }
                alarm.validate();
                if (!ids.add(alarm.id)) throw new IllegalArgumentException("Duplicate alarm ID");
                alarms.add(alarm);
            }
            for (Alarm standard : AlarmEngine.defaults()) if (!ids.contains(standard.id)) alarms.add(standard);
        } catch (RuntimeException invalid) { throw new IOException("Invalid alarm settings: " + path, invalid); }
        timerDeadlines.putAll(loadedDeadlines);
        return alarms;
    }
    public void save(List<Alarm> alarms, String language) throws IOException {
        save(alarms,language,timerDeadlines());
    }
    public void save(List<Alarm> alarms, String language, Map<String,Long> deadlines) throws IOException {
        Map<String,Long> savedDeadlines=new HashMap<String,Long>();
        Properties p = new Properties(); p.setProperty("schema", "1"); p.setProperty("language", language);
        p.setProperty("waterReminderMode","below-threshold");
        p.setProperty("sound", Boolean.toString(options.sound));p.setProperty("animation", Boolean.toString(options.animation));
        p.setProperty("count", Integer.toString(alarms.size()));
        for (int i = 0; i < alarms.size(); i++) {
            Alarm a = alarms.get(i); String key = "alarm." + i + ".";
            p.setProperty(key + "id", a.id); p.setProperty(key + "kind", a.kind.name());
            p.setProperty(key + "name", a.name); p.setProperty(key + "message", a.message);
            p.setProperty(key + "enabled", Boolean.toString(a.enabled));
            p.setProperty(key + "autoRestartAfterPrayer", Boolean.toString(a.autoRestartAfterPrayer));
            p.setProperty(key + "autoRestartAfterMeditation", Boolean.toString(a.autoRestartAfterMeditation));
            p.setProperty(key + "threshold", Integer.toString(a.threshold)); p.setProperty(key + "stopAt", Integer.toString(a.stopAt));
            p.setProperty(key + "delay", Long.toString(a.delayMillis)); p.setProperty(key + "repeat", Long.toString(a.repeatMillis));
            long due=a.type().has(AlarmType.Parameter.DELAY)?deadlines.getOrDefault(a.id,0L):0;
            if(due<0)throw new IllegalArgumentException("Invalid timer deadline.");
            p.setProperty(key + "due", Long.toString(due));
            if(a.type().has(AlarmType.Parameter.DELAY))savedDeadlines.put(a.id,due);
        }
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "alarms-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { p.store(writer, "Chamomilo Alarm Clock"); }
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unavailable) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
        this.language = language;
        timerDeadlines.clear();timerDeadlines.putAll(savedDeadlines);
    }
    private static long number(Properties properties,String key,long fallback,boolean active) {
        try {return Long.parseLong(properties.getProperty(key,Long.toString(fallback)));}
        catch(NumberFormatException invalid) {if(active)throw invalid;return fallback;}
    }
    private static int integer(Properties properties,String key,int fallback,boolean active) {
        try {return Integer.parseInt(properties.getProperty(key,Integer.toString(fallback)));}
        catch(NumberFormatException invalid) {if(active)throw invalid;return fallback;}
    }
}
