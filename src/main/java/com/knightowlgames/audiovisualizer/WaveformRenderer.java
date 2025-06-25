package com.knightowlgames.audiovisualizer;

import lombok.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Render a WaveForm.
 */
class WaveformRenderer {
    private final Canvas canvas;
    private InfoBlock info;

    public record InfoBlock(Font infoFont, Color infoInsetColor, Color infoTextColor, String info) { }
    public record Canvas(Rectangle imageBounds, Color globalBackground, Color initialColor, Color shift) { }

    public WaveformRenderer(Rectangle imageBounds, Color backgroundColor, Color initialColor, Color colorShift) {
        this.canvas = new Canvas(imageBounds,backgroundColor,initialColor,colorShift);
    }

    public WaveformRenderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public class Waveform {

        private byte[] audioBytes;
        private AudioFormat format;

        Waveform(AudioInputStream audioInputStream) throws IOException {
            format = audioInputStream.getFormat();
            audioBytes = new byte[(int) (audioInputStream.getFrameLength() * format.getFrameSize())];
            System.out.println("AudioFormat: " + format + " " + audioInputStream.read(audioBytes) + " bytes read");
        }

        public Render generate(TimeUnit timeUnit, int audioLength) {
            return new Render(audioBytes, format, timeUnit, audioLength, canvas, info);
        }

        public Render generate() {
            return new Render(audioBytes, format, canvas, info);
        }
    }

    public WaveformRenderer withInfo(Font style, Color infoBackgroundColor, Color textColor, String info) {
        this.info = new InfoBlock(style,infoBackgroundColor,textColor,info);
        return this;
    }

    public WaveformRenderer withInfo(InfoBlock info) {
        this.info = info;
        return this;
    }

    public Waveform forAudio(AudioInputStream audioInputStream) throws IOException {

        return new Waveform(audioInputStream);
    }
} // End class SamplingGraph
