package com.kaymlyn.audiovisualizer.audio.power;

import com.kaymlyn.audiovisualizer.audio.AudioProcessor;
import com.kaymlyn.audiovisualizer.audio.Renderer;
import com.kaymlyn.audiovisualizer.audio.wave.Fading;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PowerSeriesRenderer implements Renderer {

    private final AudioProcessor.Canvas canvas;
    private final AudioProcessor.InfoBlock info;

    public PowerSeriesRenderer(AudioProcessor.Canvas canvas, AudioProcessor.InfoBlock info) {
        this.canvas = canvas;
        this.info = info;
    }


    @Override
    public BufferedImage renderToImage(int frameIndex) {
        return null;
    }

    @Override
    public void renderToFile(File imageFile) throws IOException {

    }

    private Graphics2D render(Graphics2D graphics, int lineLimit, Fading.Fade fadeRate) {
//        //total number of lines from the left to render. Fade is not considered in this calculation
//        int totalLines;
//        if(lineLimit < 0 || lineLimit > rawWaveform.size()) {
//            totalLines = rawWaveform.size();
//        } else {
//            totalLines = lineLimit;
//        }
//
//        //preserve starting color for additional renders.
//        Color currentColor = new Color(
//                (canvas.initialColor().getRed() + canvas.shift().getRed()) % 255,
//                (canvas.initialColor().getGreen() + canvas.shift().getGreen()) % 255,
//                (canvas.initialColor().getBlue() + canvas.shift().getBlue()) % 255
//        );
//
//        graphics.setColor(currentColor);
//
//        //fade is calculated based on the overall image. need to identify a way to set an offset. Ideally, if the
//        //waveform stops at the middle of the canvas and the fade rate would require the whole canvas to complete
//        //the waveform at the edge of the canvas should only be half faded.
//        for (int i = 1; i < rawWaveform.size() && i < totalLines; i++) {
//            currentColor = new Color((currentColor.getRed() + canvas.shift().getRed()) % 255,
//                    (currentColor.getGreen() + canvas.shift().getGreen()) % 255,
//                    (currentColor.getBlue() + canvas.shift().getBlue()) % 255);
//
//            graphics.setColor(new Color(
//                    fadeComponent(currentColor.getRed(), canvas.globalBackground().getRed(), fadeRate.redFade(), totalLines - i),
//                    fadeComponent(currentColor.getGreen(), canvas.globalBackground().getGreen(), fadeRate.greenFade(), totalLines - i),
//                    fadeComponent(currentColor.getBlue(), canvas.globalBackground().getBlue(), fadeRate.blueFade(), totalLines - i)
//            ));
//            graphics.draw(rawWaveform.get(i));
//        }
//        return graphics;
        return null;
    }

    private Graphics2D prepareCanvas(BufferedImage image) {
        Graphics2D g2 = image.createGraphics();

        g2.setBackground(canvas.globalBackground());
        g2.clearRect(0, 0, canvas.imageBounds().width, canvas.imageBounds().height);

        if(info != null) {
            FontMetrics fontMetrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    .getGraphics()
                    .getFontMetrics(info.infoFont());
            Rectangle2D stringBounds = fontMetrics.getStringBounds(info.info(), g2);

            g2.setColor(info.infoInsetColor());
            g2.fillRect(0, canvas.imageBounds().height - (int) (stringBounds.getHeight()), (int) stringBounds.getWidth() + 4, canvas.imageBounds().height + (int) stringBounds.getHeight() + 4);
            g2.setColor(Color.BLUE);
            g2.setFont(info.infoFont());
            g2.drawString(info.info(), 3, canvas.imageBounds().height - 4);
        }
        return g2;
    }

    private BufferedImage prepareImage (double percentage, Fading.Fade fade) {

        int linesToRender;
        if(percentage > 1 || percentage < 0) {
            linesToRender = canvas.imageBounds().width;
        } else {
            linesToRender = (int)(canvas.imageBounds().width * percentage);
        }

        BufferedImage image = new BufferedImage(canvas.imageBounds().width, canvas.imageBounds().height, BufferedImage.TYPE_INT_RGB);
        render(
                prepareCanvas(image),
                linesToRender,
                fade
        ).dispose();
        return image;
    }
}
