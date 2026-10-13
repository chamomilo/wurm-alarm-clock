package org.chamomilo.wurm.alarmclock;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Original consonant motifs, with distinct register, contour and rhythm. No sharp attacks. */
public enum Chime {
    FOOD(new int[]{72,76},new double[]{0,.34},.95),
    WATER(new int[]{79,76,72},new double[]{0,.18,.36},.80),
    // Keep the fifth/octave motif in the midrange, with separate soft strikes.
    PRAYER(new int[]{64,71,76},new double[]{0,.22,.44},1.50,.36),
    MEDITATION(new int[]{81,84,88,84},new double[]{0,.30,.60,.90},.90),
    FAVOR(new int[]{76,79,76,79},new double[]{0,.12,.65,.77},.95),
    TIMER(new int[]{74},new double[]{0},1.10);
    public static final int SAMPLE_RATE=44100;
    private final int[] notes;
    private final double[] starts;
    private final double decayLength;
    private final double peakAmplitude;
    private byte[] wave;
    Chime(int[] notes,double[] starts,double decayLength) { this(notes,starts,decayLength,.26); }
    Chime(int[] notes,double[] starts,double decayLength,double peakAmplitude) {
        this.notes=notes;this.starts=starts;this.decayLength=decayLength;this.peakAmplitude=peakAmplitude;
    }
    public static Chime forAlarm(Alarm alarm) {
        return alarm.type().appearance.chime;
    }
    public long durationMillis() { return Math.round((starts[starts.length-1]+decayLength+.12)*1000); }
    public String path() { return "/org/chamomilo/wurm/alarmclock/chimes/"+name().toLowerCase(java.util.Locale.ROOT)+".wav"; }
    public synchronized InputStream open() {
        if(wave==null)wave=synthesize();
        return new ByteArrayInputStream(wave);
    }
    private byte[] synthesize() {
        int count=(int)(durationMillis()*SAMPLE_RATE/1000);
        double[] samples=new double[count];double peak=0;
        for(int n=0;n<notes.length;n++) {
            double frequency=440*Math.pow(2,(notes[n]-69)/12.0);
            int begin=(int)(starts[n]*SAMPLE_RATE), length=(int)(decayLength*SAMPLE_RATE);
            for(int i=0;i<length&&begin+i<count;i++) {
                double t=i/(double)SAMPLE_RATE;
                double attack=Math.min(1,t/.035);attack=attack*attack*(3-2*attack);
                double tail=Math.min(1,(decayLength-t)/.16);tail=tail*tail*(3-2*tail);
                double envelope=attack*tail*Math.exp(-t/(decayLength*.34));
                double phase=2*Math.PI*frequency*t;
                samples[begin+i]+=envelope*(Math.sin(phase)+.16*Math.sin(phase*2)+.035*Math.sin(phase*3));
            }
        }
        for(double sample:samples)peak=Math.max(peak,Math.abs(sample));
        ByteBuffer out=ByteBuffer.allocate(44+count*2).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(0x46464952).putInt(36+count*2).putInt(0x45564157).putInt(0x20746d66).putInt(16);
        out.putShort((short)1).putShort((short)1).putInt(SAMPLE_RATE).putInt(SAMPLE_RATE*2).putShort((short)2).putShort((short)16);
        out.putInt(0x61746164).putInt(count*2);
        for(double sample:samples)out.putShort((short)Math.round(sample/peak*peakAmplitude*32767));
        return out.array();
    }
}
