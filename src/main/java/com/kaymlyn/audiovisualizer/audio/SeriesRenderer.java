package com.kaymlyn.audiovisualizer.audio;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface SeriesRenderer  {
    List<BufferedImage> renderFrames(int frameIndex, int fps, int framesPerCanvas);
    void renderFramesToFiles(File imageDirectory) throws IOException;

}
