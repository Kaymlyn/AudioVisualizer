package com.kaymlyn.audiovisualizer.video;

import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;

import java.io.File;
import java.io.IOException;

public class VideoRenderer {
    public void thing() throws IOException {
        File output = new File("test.mp4");
        SequenceEncoder enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), new Rational(1, 1));
        String[] files = {"frame0.png", "frame1.png", "frame2.png"};
        for (String file : files) {
//            enc.encodeNativeFrame(AWTUtil.fromBufferedImage(new File(file), ColorSpace.RGB));
        }
        enc.finish();
    }
}
