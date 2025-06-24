package com.knightowlgames.audiovisualizer;

import java.io.File;

public class Driver {

    public static void main(String[] args) throws Exception {
        Parser awc = new Parser("src/main/resources/",
                "short");
        awc.createAudioInputStream();
    }
}
