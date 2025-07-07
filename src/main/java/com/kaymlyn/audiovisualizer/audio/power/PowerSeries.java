package com.kaymlyn.audiovisualizer.audio.power;

import com.kaymlyn.audiovisualizer.audio.AudioProcessor;
import com.kaymlyn.audiovisualizer.audio.Generator;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PowerSeries implements Generator<PowerSeriesRenderer> {

    private final AudioProcessor audioProcessor;
    private final AudioFormat format;

    public PowerSeries(AudioProcessor audioProcessor, AudioInputStream audioInputStream) throws IOException {
        this.audioProcessor = audioProcessor;
        format = audioInputStream.getFormat();
        byte[] audioBytes = new byte[(int) (audioInputStream.getFrameLength() * format.getFrameSize())];
        System.out.println("AudioFormat: " + format + " " + audioInputStream.read(audioBytes) + " bytes read");
        audioInputStream.close();

        for (int i = 0; i < 10; i++) {
            Complex[] series = constructSeries(audioProcessor.decodeAudio(format, audioBytes), 1, i);
        }
    }

    @Override
    public List<Double> balance(int[] audioData) {
        int max = Arrays.stream(audioData)
                .map(Math::abs)
                .boxed()
                .sorted(Comparator.naturalOrder())
                .toList()
                .getLast();

        //Scales all the integers in the audioData to the canvas height
        return Arrays.stream(audioData)
                .mapToDouble(value -> (double) (audioProcessor.canvas.imageBounds().height * value / max))
                .boxed()
                .toList();
    }

    @Override
    public PowerSeriesRenderer generate() {
        return null;
    }

    private Complex[] constructSeries(int[] audioData, double frameSizeInSeconds, int frameCount) {
        int sampleSize = (int) (format.getSampleRate() * frameSizeInSeconds);
        int frameStart = sampleSize * frameCount;
        int[] sample = new int[sampleSize];
        for (int i = 0; i < sampleSize && frameStart + i < audioData.length; i++) {
            sample[i] = audioData[frameStart + i];
        }
        List<Double> balancedList = balance(audioData);
        double[] balanced = new double[(int) Math.pow(2, Math.ceil(Math.log(balancedList.size()) / Math.log(2)))];
        for (int i = 0; i < balancedList.size(); i++) {
            balanced[i] = balancedList.get(i);
        }
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] transformed = transformer.transform(balanced, TransformType.FORWARD);
        Arrays.stream(transformed).forEach(System.out::println);
        return null;
    }
}
