package org.chamomilo.wurm.alarmclock;

import com.wurmonline.client.resources.AlarmChimeUrl;
import org.junit.Test;
import javax.sound.sampled.*;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;

public class ChimeTest {
    @Test public void motifsAreDistinctValidQuietPcmWithSoftEdges() throws Exception {
        Set<Integer> signatures=new HashSet<Integer>();
        for(Chime chime:Chime.values())try(AudioInputStream audio=AudioSystem.getAudioInputStream(new AlarmChimeUrl(chime).openStream())) {
            AudioFormat f=audio.getFormat();assertEquals(44100,f.getSampleRate(),0);assertEquals(1,f.getChannels());assertEquals(16,f.getSampleSizeInBits());
            assertFalse(f.isBigEndian());assertTrue(chime.durationMillis()>=1000&&chime.durationMillis()<2500);
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
            while((count=audio.read(buffer))!=-1)out.write(buffer,0,count);
            byte[] pcm=out.toByteArray();assertTrue(signatures.add(Arrays.hashCode(pcm)));
            int peak=0;double squared=0;
            for(int i=0;i<pcm.length;i+=2) { int sample=(short)((pcm[i]&255)|(pcm[i+1]<<8));peak=Math.max(peak,Math.abs(sample));squared+=(double)sample*sample; }
            assertTrue("Soft amplitude",peak>8000&&peak<9000);
            assertTrue("Audible motif",Math.sqrt(squared/(pcm.length/2))/32768>.025);
            for(int i=0;i<80;i++)assertEquals("No ending click",0,pcm[pcm.length-1-i]);
            assertEquals(0,pcm[0]);assertEquals(0,pcm[1]);
        }
    }
    @Test public void typeIdentityDeterminesSoundEvenWhenNamesAndTextAreEdited() {
        List<Alarm> alarms=AlarmEngine.defaults();Chime[] expected={Chime.FOOD,Chime.WATER,Chime.PRAYER,Chime.MEDITATION,Chime.FAVOR};
        for(int i=0;i<alarms.size();i++){Alarm a=alarms.get(i);a.name=a.message="Same words";assertSame(expected[i],Chime.forAlarm(a));}
        assertSame(Chime.TIMER,Chime.forAlarm(Alarm.create(Alarm.Kind.TIMER,"Prayer","It's time to meditate.")));
    }
    @Test public void nativeWurmWaveDecoderAcceptsEveryChime() {
        for(Chime chime:Chime.values()) {
            com.wurmonline.client.sound.formats.WavData data=com.wurmonline.client.sound.formats.WavData.create(chime.open());
            assertNotNull(data);assertEquals(44100,data.getRate());assertEquals(1,data.getChannels());
            assertTrue(data.getData().remaining()>80000);data.clear();
        }
    }
    @Test public void simultaneousChimesPlayInOrderWithoutOverlapAndDisconnectDropsQueuedSounds() {
        List<Chime> heard=new ArrayList<Chime>();AlarmSounds sounds=new AlarmSounds(heard::add);
        sounds.enqueue(Chime.FOOD);sounds.enqueue(Chime.WATER);sounds.enqueue(Chime.FOOD);
        sounds.tick(1000);assertEquals(Arrays.asList(Chime.FOOD),heard);
        sounds.tick(1000+Chime.FOOD.durationMillis()+249);assertEquals(1,heard.size());
        sounds.tick(1000+Chime.FOOD.durationMillis()+250);assertEquals(Arrays.asList(Chime.FOOD,Chime.WATER),heard);
        sounds.enqueue(Chime.PRAYER);sounds.clear();sounds.tick(100000);assertEquals(2,heard.size());
        sounds.enqueue(Chime.MEDITATION);sounds.tick(100001);assertEquals(Chime.MEDITATION,heard.get(2));
    }
    @Test public void unavailableAudioDoesNotBreakTheScheduler() {
        AlarmSounds sounds=new AlarmSounds(c->{throw new IllegalStateException("No sound device");});
        sounds.enqueue(Chime.PRAYER);sounds.tick(1000);sounds.clear();
    }
    @Test public void cancellationKeepsOtherAlarmsWithTheSameChimeAndExplicitPreviews() {
        List<Chime> heard=new ArrayList<Chime>();AlarmSounds sounds=new AlarmSounds(heard::add);
        Alarm first=Alarm.create(Alarm.Kind.FAVOR,"First spell reminder","Cast now.");
        Alarm second=Alarm.create(Alarm.Kind.FAVOR,"Second spell reminder","Cast later.");
        sounds.enqueue(first);sounds.enqueue(first);sounds.enqueue(second);sounds.enqueue(Chime.FAVOR);
        sounds.cancel(first.id);
        sounds.tick(1000);sounds.tick(100_000);sounds.tick(200_000);
        assertEquals(Arrays.asList(Chime.FAVOR,Chime.FAVOR),heard);
    }
}
