package com.knightowlgames.audiovisualizer;


import com.knightowlgames.audiovisualizer.wave.Render;
import com.knightowlgames.audiovisualizer.wave.WaveformRenderer;

import javax.sound.sampled.AudioSystem;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class Driver {

    public static void main(String[] args) throws Exception {

        String rootPath = "src/main/resources/";
        String fileName = "short";

        WaveformRenderer waveformRenderer = new WaveformRenderer(
                new Rectangle(600, 200),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(71, 4, 2))
                .withInfo(new Font("serif", Font.PLAIN, 12),
                        Color.white,
                        Color.blue,
                        "File: " + fileName + ".wav");
        WaveformRenderer.Waveform waveform = waveformRenderer.forAudio(AudioSystem.getAudioInputStream(new File(rootPath + fileName + ".wav")));

        waveform.generate().saveToFile(new File(rootPath + "image/" + fileName + ".png"));

        waveform.generate(TimeUnit.SECONDS, 1.3)
                .withFade(new Render.Fade(1,1,1))
                .renderPercentage(.5)
                .saveToFile(new File(rootPath + "image/" + fileName + "2.png"));

        waveformRenderer = new WaveformRenderer(
                new Rectangle(2000, 200),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(3, 3, 3))
                .withInfo(new Font("serif", Font.PLAIN, 12),
                        Color.white,
                        Color.blue,
                        "File: " + fileName + ".wav");

        waveformRenderer.forAudio(AudioSystem.getAudioInputStream(new File(rootPath + fileName + ".wav")))
                .generate(TimeUnit.SECONDS, 1.3)
                .withFade(new Render.Fade(.025,.035,.015))
                .saveToFile(new File(rootPath + "image/" + fileName + "3.gif"));
    }



}
