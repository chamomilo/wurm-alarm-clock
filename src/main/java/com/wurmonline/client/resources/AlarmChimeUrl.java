package com.wurmonline.client.resources;

import org.chamomilo.wurm.alarmclock.Chime;
import java.io.InputStream;
import java.util.*;

/** Native asynchronous sound loader reads generated WAV bytes without filesystem installation. */
public final class AlarmChimeUrl extends ResourceUrl {
    private final Chime chime;
    public AlarmChimeUrl(Chime chime) { super(chime.path());this.chime=chime; }
    @Override public ResourceUrl derive(String relative) { return changeFilePath(chime.path().substring(0,chime.path().lastIndexOf('/')+1)+relative); }
    @Override public ResourceUrl changeFilePath(String path) {
        for(Chime value:Chime.values())if(value.path().equals(path))return new AlarmChimeUrl(value);
        throw new IllegalArgumentException("Unknown alarm chime");
    }
    @Override public InputStream openStream() { return chime.open(); }
    @Override public boolean exists() { return true; }
    @Override public String getFilePath() { return chime.path(); }
    @Override public Map<String,String> getOverrides() { return Collections.emptyMap(); }
    @Override long getSize() { return 44+chime.durationMillis()*Chime.SAMPLE_RATE/1000*2; }
    @Override public boolean equals(Object other) { return other instanceof AlarmChimeUrl&&chime==((AlarmChimeUrl)other).chime; }
    @Override public String toString() { return "alarmclock:"+chime.path(); }
}
