package com.kaymlyn.audiovisualizer;


import com.kaymlyn.audiovisualizer.wave.AudioWaveformRenderer;
import com.kaymlyn.audiovisualizer.wave.AudioProcessor;
import com.kaymlyn.audiovisualizer.wave.Fading;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

public class Driver {

    public static void main(String[] args) throws Exception {

        String rootPath = "src/main/resources/";
        String fileName = "short";

        AudioProcessor waveformRenderer = new AudioProcessor(
                new Rectangle(600, 200),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(71, 4, 2))
                .withInfo(new Font("serif", Font.PLAIN, 12),
                        Color.white,
                        Color.blue,
                        "File: " + fileName + ".wav");
        AudioProcessor.Waveform waveform = waveformRenderer.forAudio(AudioSystem.getAudioInputStream(new File(rootPath + fileName + ".wav")));

        waveform.generate().renderToFile(new File(rootPath + "image/" + fileName + ".png"));

        waveform.generate()
                .withFade(new Fading.Fade(1, 1, 1))
                .renderPercentage(.5)
                .renderToFile(new File(rootPath + "image/" + fileName + "2.png"));

        waveformRenderer = new AudioProcessor(
                new Rectangle(2000, 200),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(3, 3, 3))
                .withInfo(new Font("serif", Font.PLAIN, 12),
                        Color.white,
                        Color.blue,
                        "File: " + fileName + ".wav");

        waveformRenderer.forAudio(AudioSystem.getAudioInputStream(new File(rootPath + fileName + ".wav")))
                .generate()
                .withFade(new AudioWaveformRenderer.Fade(.025, .035, .015))
                .renderToFile(new File(rootPath + "image/" + fileName + "3.gif"));

        byte[] mp3;
        AudioFormat mp3Format = new AudioFormat(44100.0F, 16, 2, true, false);
        try (FileInputStream fileInputStream = new FileInputStream(rootPath + "Jim_Yosef-Firefly.mp3")) {
            mp3 = fileInputStream.readAllBytes();
        }

        AudioInputStream.nullInputStream().read(mp3);
        waveformRenderer = new AudioProcessor(
                new Rectangle(600, 300),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(71, 4, 2))
                .withInfo(new Font("serif", Font.PLAIN, 12),
                        Color.white,
                        Color.blue,
                        "File: " + fileName + ".wav");
        waveformRenderer.forAudio(new AudioInputStream(new ByteArrayInputStream(mp3), mp3Format, (mp3.length / 2)))
                .generate()
                .renderToFile(new File(rootPath + "image/Firefly.png"));
    }
}
