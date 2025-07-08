package com.kaymlyn.audiovisualizer.audio;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public interface Renderer {

    BufferedImage renderToImage(int frameIndex);
    void renderToFile(File imageFile) throws IOException;
}
