package com.kaymlyn.audiovisualizer.wave;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public interface Renderer {

    BufferedImage renderToImage();
    void renderToFile(File imageFile) throws IOException;
}
