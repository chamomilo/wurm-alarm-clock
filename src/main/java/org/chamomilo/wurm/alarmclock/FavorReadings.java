package org.chamomilo.wurm.alarmclock;

import com.wurmonline.client.game.SkillLogic;
import com.wurmonline.client.game.SkillLogicSet;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-supplied readings scoped to a player's skill set, never the client's global name map. */
public final class FavorReadings {
    private final Map<SkillLogicSet, Values> players = new WeakHashMap<SkillLogicSet, Values>();
    public static final Sample UNKNOWN = new Sample(Float.NaN, Float.NaN);

    public void added(SkillLogicSet owner, SkillLogic skill) {
        added(owner, skill.getId(), skill.getName(), skill.getValue());
    }
    synchronized void added(SkillLogicSet owner, long id, String name, float value) {
        boolean favor = "favor".equalsIgnoreCase(name), faith = "faith".equalsIgnoreCase(name);
        if (owner == null || (!favor && !faith)) return;
        Values values = players.get(owner);
        if (values == null) { values = new Values(); players.put(owner, values); }
        if (favor) { values.favorId = id; values.favor = value; values.hasFavor = true; }
        else { values.faithId = id; values.faith = value; values.hasFaith = true; }
    }
    public synchronized void updated(SkillLogicSet owner, long id, float value) {
        Values values = players.get(owner);
        if (values == null) return;
        if (values.hasFavor && values.favorId == id) values.favor = value;
        if (values.hasFaith && values.faithId == id) values.faith = value;
    }
    public synchronized Sample read(SkillLogicSet owner) {
        Values values = players.get(owner);
        return values == null || !values.hasFavor || !values.hasFaith ? UNKNOWN : new Sample(values.favor, values.faith);
    }
    public synchronized void cleared(SkillLogicSet owner) { players.remove(owner); }
    public synchronized void reset() { players.clear(); }
    public static final class Sample {
        public final float favor, faith;
        private Sample(float favor, float faith) { this.favor = favor; this.faith = faith; }
    }
    private static final class Values {
        long favorId, faithId;
        float favor, faith;
        boolean hasFavor, hasFaith;
    }
}
