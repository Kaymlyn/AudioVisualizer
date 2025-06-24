package com.knightowlgames.audiovisualizer;


import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class Driver {

    public static void main(String[] args) throws Exception {

        String rootPath = "src/main/resources/";
        String fileName = "short";

        AudioInputStream audio =
                AudioSystem.getAudioInputStream(new File(rootPath + fileName + ".wav"));

        WaveformRenderer renderer = new WaveformRenderer(AudioSystem.getAudioInputStream(new File(rootPath + fileName + ".wav")),
                new Rectangle(6000, 200),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(71, 4, 2),
                new Font("serif", Font.PLAIN, 12),
                Color.white
        );
        renderer.createWaveForm(rootPath + "image/" + fileName + ".png",true, TimeUnit.SECONDS, 1);
        renderer = new WaveformRenderer(AudioSystem.getAudioInputStream(new File(rootPath + fileName + ".wav")),
                new Rectangle(6000, 200),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(71, 4, 2),
                new Font("serif", Font.PLAIN, 12),
                Color.white
        );
        renderer.createWaveForm(rootPath + "image/" + fileName + "2.png", true, TimeUnit.SECONDS, 2);
        renderer = new WaveformRenderer(AudioSystem.getAudioInputStream(new File(rootPath + fileName + ".wav")),
                new Rectangle(6000, 200),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(71, 4, 2),
                new Font("serif", Font.PLAIN, 12),
                Color.white
        );
        renderer.createWaveForm(rootPath + "image/" + fileName + "3.png");
    }


}
