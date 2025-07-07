package com.kaymlyn.audiovisualizer.audio.power;

import com.kaymlyn.audiovisualizer.audio.Renderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PowerSeriesRenderer implements Renderer {

    @Override
    public BufferedImage renderToImage() {
        return null;
    }

    @Override
    public void renderToFile(File imageFile) throws IOException {

    }

    public void fastFourierTransform(byte[] sampledFrame) {

    }
}
