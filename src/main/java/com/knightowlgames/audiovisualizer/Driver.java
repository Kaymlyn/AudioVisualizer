package com.knightowlgames.audiovisualizer;


import javax.sound.sampled.AudioInputStream;
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
                new Rectangle(6000, 200),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(71, 4, 2))
                .withInfo(new Font("serif", Font.PLAIN, 12),
                        Color.white,
                        Color.blue,
                        "File: " + fileName + ".wav");
        WaveformRenderer.Waveform waveform = waveformRenderer.forAudio(AudioSystem.getAudioInputStream(new File(rootPath + fileName + ".wav")));

        waveform.generate().saveToFile(new File(rootPath + "image/" + fileName + ".png"));
        waveform.generate(TimeUnit.MILLISECONDS, 200)
                .withFadeAdjustments(new Render.Fade(.2,.2,.2)).saveToFile(new File(rootPath + "image/" + fileName + "2.png"));
    }



}
