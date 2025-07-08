package com.kaymlyn.audiovisualizer.video;

import com.kaymlyn.audiovisualizer.audio.SeriesRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class VideoRenderer implements SeriesRenderer {
//    public void thing() throws IOException {
//        File output = new File("test.mp4");
//        SequenceEncoder enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), new Rational(1, 1));
//        String[] files = {"frame0.png", "frame1.png", "frame2.png"};
//        for (String file : files) {
////            enc.encodeNativeFrame(AWTUtil.fromBufferedImage(new File(file), ColorSpace.RGB));
//        }
//        enc.finish();
//    }

    @Override
    public List<BufferedImage> renderFrames(int frameIndex, int fps, int framesPerCanvas) {

        return null;
    }

    @Override
    public void renderFramesToFiles(File imageDirectory) throws IOException {

    }
}
