package com.kaymlyn.audiovisualizer.wave;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Translate AudioData into a waveform.
 */
public class AudioProcessor {
    private final Canvas canvas;
    private InfoBlock info;

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

    public Waveform forAudio(AudioInputStream audioInputStream) throws IOException {

        return new Waveform(audioInputStream);
    }

    //translates data from byte input into an array of integers based on the audio format. This is the magic.
    //If you wrote this please reach out with where you published it, so I can give proper credit.
    private int[] decodeAudio(AudioFormat format, byte[] audioBytes) {

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

    public class PowerSeries implements Generator {

        @Override
        public Renderer generate() {
            return null;
        }
    }

    public class Waveform implements Generator<AudioWaveformRenderer> {

        private final AudioFormat format;
        private final List<Line2D.Double> waveform;

        Waveform(AudioInputStream audioInputStream) throws IOException {
            format = audioInputStream.getFormat();
            byte[] audioBytes = new byte[(int) (audioInputStream.getFrameLength() * format.getFrameSize())];
            System.out.println("AudioFormat: " + format + " " + audioInputStream.read(audioBytes) + " bytes read");
            audioInputStream.close();

            waveform = constructWaveForm(decodeAudio(format, audioBytes));
        }

        public AudioWaveformRenderer generate() {
            return new AudioWaveformRenderer(waveform, canvas, info);
        }

        //Generates the List that represents the raw waveform image from raw audio data
        private List<Line2D.Double> constructWaveForm(int[] audioData) {

            //Super complicated way of finding the maximum integer in the audio data for normalization
            int max = Arrays.stream(audioData)
                    .map(Math::abs)
                    .boxed()
                    .sorted(Comparator.naturalOrder())
                    .toList()
                    .getLast();

            //Scales all the integers in the audioData to the canvas height
            List<Integer> balanced = Arrays.stream(audioData)
                    .map(value -> (canvas.imageBounds().height * value / max))
                    .boxed()
                    .toList();

            List<Line2D.Double> lines = new ArrayList<>();

            //Raw Waveform data generation.
            int y_last = 0;
            for (int x = 0; x < canvas.imageBounds().width; x++) {
                Integer value = balanced.get((audioData.length / format.getFrameSize() / canvas.imageBounds().width) * format.getChannels() * x);
                //scale data to the viewport height
                int y_new = canvas.imageBounds().height * (128 - value) / 256;
                //add vertical line to array offset by x pixels.
                lines.add(new Line2D.Double(x, y_last, x, y_new));
                //return the last height
                y_last = y_new;
            }
            return lines;
        }
    }
}
