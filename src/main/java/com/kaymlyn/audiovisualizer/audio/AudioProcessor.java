package com.kaymlyn.audiovisualizer.audio;

import com.kaymlyn.audiovisualizer.audio.power.PowerSeries;
import com.kaymlyn.audiovisualizer.audio.wave.Waveform;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.IOException;

/**
 * Translate AudioData into a waveform.
 */
public class AudioProcessor {
    public final Canvas canvas;
    public InfoBlock info;

    public record InfoBlock(Font infoFont, Color infoInsetColor, Color infoTextColor, String info) { }
    public record Canvas(Rectangle imageBounds, Color globalBackground, Color initialColor, Color shift) { }

    public AudioProcessor(Rectangle imageBounds, Color backgroundColor, Color initialColor, Color colorShift) {
        this(new Canvas(imageBounds,backgroundColor,initialColor,colorShift));
    }

    public AudioProcessor(Canvas canvas) {
        this.canvas = canvas;
    }

    public AudioProcessor withInfo(Font style, Color infoBackgroundColor, Color textColor, String info) {
        return withInfo(new InfoBlock(style,infoBackgroundColor,textColor,info));
    }

    public AudioProcessor withInfo(InfoBlock info) {
        this.info = info;
        return this;
    }

    public Waveform waveformForAudio(AudioInputStream audioInputStream) throws IOException {

        return new Waveform(this, audioInputStream);
    }

    public PowerSeries powerSeriesForAudio(AudioInputStream audioInputStream) throws IOException {
        return new PowerSeries(this, audioInputStream);
    }

    //translates data from byte input into an array of integers based on the audio format. This is the magic.
    //If you wrote this please reach out with where you published it, so I can give proper credit.
    public int[] decodeAudio(AudioFormat format, byte[] audioBytes) {

        int[] audioData = null;
        if (format.getSampleSizeInBits() == 16) {
            int nlengthInSamples = audioBytes.length / 2;
            audioData = new int[nlengthInSamples];
            if (format.isBigEndian()) {
                for (int i = 0; i < nlengthInSamples; i++) {
                    /* First byte is MSB (high order) */
                    int MSB = audioBytes[2 * i];
                    /* Second byte is LSB (low order) */
                    int LSB = audioBytes[2 * i + 1];
                    audioData[i] = MSB << 8 | (255 & LSB);
                }
            } else {
                for (int i = 0; i < nlengthInSamples; i++) {
                    /* First byte is LSB (low order) */
                    int LSB = audioBytes[2 * i];
                    /* Second byte is MSB (high order) */
                    int MSB = audioBytes[2 * i + 1];
                    audioData[i] = MSB << 8 | (255 & LSB);
                }
            }
        } else if (format.getSampleSizeInBits() == 8) {
            int nlengthInSamples = audioBytes.length;
            audioData = new int[nlengthInSamples];
            if (format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = audioBytes[i];
                }
            } else {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = audioBytes[i] - 128;
                }
            }
        }
        return audioData;
    }

}
