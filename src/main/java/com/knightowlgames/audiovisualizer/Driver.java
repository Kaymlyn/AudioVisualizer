package com.knightowlgames.audiovisualizer;

import java.awt.Color;

public class Driver {

    public static void main(String[] args) throws Exception {
        Parser awc = new Parser("src/main/resources/",
                "short");
        awc.createAudioInputStream(6000,200, new Color(20,20,20));
    }
}
