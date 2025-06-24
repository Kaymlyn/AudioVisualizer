package com.knightowlgames.audiovisualizer;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Parser {


    public Parser() {
    }

    public void transformAudioToImage(String fileRoot, String fileName,int width, int height) throws UnsupportedAudioFileException, IOException {

        AudioInputStream audio =
                AudioSystem.getAudioInputStream(new File(fileRoot + fileName + ".wav"));
        new WaveformRenderer(audio,
                new Rectangle(width,height),
                new Color(20, 20, 20),
                new Color(0, 0, 255),
                new Color(71, 4, 2),
                new Font("serif", Font.PLAIN, 12),
                Color.white
        ).createWaveForm(fileRoot + fileName + ".png");
    }
}