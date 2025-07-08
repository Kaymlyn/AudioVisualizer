package com.kaymlyn.audiovisualizer.audio.wave;

import com.kaymlyn.audiovisualizer.audio.AudioProcessor;
import com.kaymlyn.audiovisualizer.audio.Generator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Waveform implements Generator<AudioWaveformRenderer> {

    private final AudioProcessor audioProcessor;
    private final AudioFormat format;
    private final List<Line2D.Double> waveform;

    public Waveform(AudioProcessor audioProcessor, AudioInputStream audioInputStream, boolean complete) throws IOException {
        this.audioProcessor = audioProcessor;
        format = audioInputStream.getFormat();
        byte[] audioBytes = new byte[(int) (audioInputStream.getFrameLength() * format.getFrameSize())];
        System.out.println("AudioFormat: " + format + " " + audioInputStream.read(audioBytes) + " bytes read");
        audioInputStream.close();

        if(complete) {
            waveform = constructWaveform(Arrays.stream(audioProcessor.decodeAudio(format, audioBytes)).boxed().toList());
        } else {
            waveform = constructWaveForm(audioProcessor.decodeAudio(format, audioBytes));
        }
    }

    public Waveform(AudioProcessor audioProcessor, AudioInputStream audioInputStream) throws IOException {
        this(audioProcessor,audioInputStream,true);
    }

    public AudioWaveformRenderer generate() {
        return new AudioWaveformRenderer(waveform, audioProcessor.canvas, audioProcessor.info);
    }

    public List<Integer> balance(List<Integer> audioData) {
        int max = audioData.stream()
                .map(Math::abs)
                .sorted(Comparator.naturalOrder())
                .toList()
                .getLast();

        //Scales all the integers in the audioData to the canvas height
        return audioData.stream()
                .map(value -> (audioProcessor.canvas.imageBounds().height * value / max))
                .toList();
    }

    @Override
    public List<Integer> balance(int[] audioData) {
        int max = Arrays.stream(audioData)
                .map(Math::abs)
                .boxed()
                .sorted(Comparator.naturalOrder())
                .toList()
                .getLast();

        //Scales all the integers in the audioData to the canvas height
        return Arrays.stream(audioData)
                .map(value -> (audioProcessor.canvas.imageBounds().height * value / max))
                .boxed()
                .toList();
    }

    //Generates the List that represents the raw waveform image from raw audio data
    private List<Line2D.Double> constructWaveForm(int[] audioData) {

        //Super complicated way of finding the maximum integer in the audio data for normalization

        List<Integer> balanced = balance(audioData);

        List<Line2D.Double> lines = new ArrayList<>();

        //Raw Waveform data generation.
        int y_last = 0;
        for (int x = 0; x < audioProcessor.canvas.imageBounds().width; x++) {
            Integer value = balanced.get((audioData.length / format.getFrameSize() / audioProcessor.canvas.imageBounds().width) * format.getChannels() * x);
            //scale data to the viewport height
            int y_new = audioProcessor.canvas.imageBounds().height * (128 - value) / 256;
            //add vertical line to array offset by x pixels.
            lines.add(new Line2D.Double(x, y_last, x, y_new));
            //return the last height
            y_last = y_new;
        }
        return lines;
    }

    private List<Line2D.Double> constructWaveform(List<Integer> rawData) {
        return constructWaveform(rawData, rawData.size());
    }
    private List<Line2D.Double> constructWaveform(List<Integer> rawData, Integer finalLength) {
        List<Line2D.Double> lines = new ArrayList<>();
        double horizontalScaleFactor = (double)finalLength/(double)rawData.size();
        List<Integer> balanced = balance(rawData);

        //Raw Waveform data generation.
        int y_last = 0;
        for (int x = 0; x < finalLength; x++) {

            Integer value = balanced.get((int)Math.floor(x*horizontalScaleFactor));
            //scale data to the viewport height
            int y_new = audioProcessor.canvas.imageBounds().height * (128 - value) / 256;
            //add vertical line to array offset by x pixels.
            lines.add(new Line2D.Double(x, y_last, x, y_new));
            //return the last height
            y_last = y_new;
        }
        return lines;
    }
}
