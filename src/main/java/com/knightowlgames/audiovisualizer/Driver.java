package com.knightowlgames.audiovisualizer;


public class Driver {

    public static void main(String[] args) throws Exception {
        new Parser().transformAudioToImage("src/main/resources/", "short",6000,200);
    }
}
