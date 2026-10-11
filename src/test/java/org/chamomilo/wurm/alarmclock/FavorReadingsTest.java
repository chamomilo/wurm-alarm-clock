package org.chamomilo.wurm.alarmclock;

import com.wurmonline.client.game.SkillLogic;
import com.wurmonline.client.game.SkillLogicSet;
import org.junit.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

public class FavorReadingsTest {
    @Test public void waitsForBothServerReadingsAndUpdatesFavorAndChangingFaithBySkillId() throws Exception {
        FavorReadings readings=new FavorReadings();SkillLogicSet player=blank(SkillLogicSet.class);
        readings.updated(player,11,60);assertSame(FavorReadings.UNKNOWN,readings.read(player));
        readings.added(player,skill(11,"Favor",50));assertSame(FavorReadings.UNKNOWN,readings.read(player));
        readings.added(player,skill(12,"Faith",60));assertEquals(50,readings.read(player).favor,0);
        readings.updated(player,11,60);assertEquals(60,readings.read(player).favor,0);
        readings.updated(player,12,60.1f);assertEquals(60.1f,readings.read(player).faith,0);
        readings.updated(player,99,100);assertEquals(60,readings.read(player).favor,0);
    }
    @Test public void characterSkillSetsStaySeparateAndUnrelatedSkillsCannotReplaceFavor() throws Exception {
        FavorReadings readings=new FavorReadings();SkillLogicSet first=blank(SkillLogicSet.class),second=blank(SkillLogicSet.class);
        readings.added(first,skill(11,"Favor",40));readings.added(first,skill(12,"Faith",50));
        readings.added(second,skill(11,"favor",80));readings.added(second,skill(12,"faith",80));
        readings.added(first,skill(13,"Body strength",99));
        readings.updated(second,11,79);
        assertEquals(40,readings.read(first).favor,0);assertEquals(50,readings.read(first).faith,0);
        assertEquals(79,readings.read(second).favor,0);assertEquals(80,readings.read(second).faith,0);
        readings.cleared(first);assertSame(FavorReadings.UNKNOWN,readings.read(first));
        assertEquals(79,readings.read(second).favor,0);
    }
    @Test public void disconnectAndClearDiscardOldIdsUntilFreshServerSamplesArrive() throws Exception {
        FavorReadings readings=new FavorReadings();SkillLogicSet player=blank(SkillLogicSet.class);
        readings.added(player,skill(11,"Favor",80));readings.added(player,skill(12,"Faith",80));
        readings.reset();readings.updated(player,11,80);assertSame(FavorReadings.UNKNOWN,readings.read(player));
        readings.added(player,skill(21,"Favor",30));readings.added(player,skill(22,"Faith",40));
        readings.updated(player,11,80);readings.updated(player,12,80);
        assertEquals(30,readings.read(player).favor,0);assertEquals(40,readings.read(player).faith,0);
        readings.cleared(player);readings.updated(player,21,40);assertSame(FavorReadings.UNKNOWN,readings.read(player));
    }
    // Client constructors access a live World; blank fixtures exercise getters without creating game files.
    private static <T> T blank(Class<T> type) throws Exception {
        Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return type.cast(((Unsafe)field.get(null)).allocateInstance(type));
    }
    private static SkillLogic skill(long id,String name,float value) throws Exception {
        SkillLogic skill=blank(SkillLogic.class);
        set(skill,"id",id);set(skill,"name",name);set(skill,"value",value);return skill;
    }
    private static void set(Object target,String name,Object value) throws Exception {
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
}
