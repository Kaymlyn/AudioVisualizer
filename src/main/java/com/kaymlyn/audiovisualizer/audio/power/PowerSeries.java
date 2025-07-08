package com.kaymlyn.audiovisualizer.audio.power;

import com.kaymlyn.audiovisualizer.audio.AudioProcessor;
import com.kaymlyn.audiovisualizer.audio.Generator;
import org.apache.commons.math3.analysis.function.Log;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DctNormalization;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastCosineTransformer;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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


        double[] pretransformed = new double[getNextPower2plus1ArraySize(audioBytes.length)];
        for(int i = 0; i < audioBytes.length; i++) {
            pretransformed[i] = audioBytes[i];
        }
//        Arrays.stream(pretransformed).forEach(System.out::println);
        double[] transformed = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I)
                .transform(pretransformed,TransformType.FORWARD);

    }

    private int getNextPower2plus1ArraySize(int length) {
        return (int)Math.pow(2,(int)Math.ceil(Math.log(length)/Math.log(2)))+1;

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
        double[] sample = new double[sampleSize];
        for (int i = 0; i < sampleSize && frameStart + i < audioData.length; i++) {
            sample[i] = audioData[frameStart + i];
        }


//
//        List<Double> balancedList = balance(audioData);
//        double[] balanced = new double[(int) Math.pow(2, Math.ceil(Math.log(balancedList.size()) / Math.log(2)))];
//        for (int i = 0; i < balancedList.size(); i++) {
//            balanced[i] = balancedList.get(i);
//        }
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] transformed = transformer.transform(sample, TransformType.FORWARD);

        return null;
    }

}
