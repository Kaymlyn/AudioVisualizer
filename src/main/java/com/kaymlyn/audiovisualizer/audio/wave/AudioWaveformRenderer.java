package com.kaymlyn.audiovisualizer.audio.wave;

import com.kaymlyn.audiovisualizer.audio.Renderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.kaymlyn.audiovisualizer.audio.AudioProcessor.InfoBlock;
import static com.kaymlyn.audiovisualizer.audio.AudioProcessor.Canvas;

public class AudioWaveformRenderer implements Renderer, Fading<AudioWaveformRenderer> {

    private final Canvas canvas;
    private final InfoBlock info;
    private final List<Line2D.Double> rawWaveform;
    private Fade globalFade;

    public AudioWaveformRenderer(List<Line2D.Double> waveformData, Canvas canvas, InfoBlock info, Fade fade) {

        this.rawWaveform = waveformData;
        this.canvas = canvas;
        this.info = info;
        this.globalFade = fade;
    }

    public AudioWaveformRenderer(List<Line2D.Double> waveformData, Canvas canvas, InfoBlock info) {
        this(waveformData,canvas,info,new Fade(0,0,0));
    }

    //this method is unintelligent, need to add a way to change the image format, this is handled currently by a magic
    //string, I need to find out if there is an enumeration that provides all the available image formats.
    //this will need to be refactored when video is generated.
    @Override
    public void renderToFile(File imageFile) throws IOException {
        renderToFile(imageFile,canvas.imageBounds().width);
    }

    public void renderToFile(File imageFile,int index) throws IOException {

        if(!imageFile.getParentFile().exists()){
            if(!imageFile.mkdirs()) {
                throw new IOException("Unable to create file to store image.");
            }
        }
        prepareImage(index);
        ImageIO.write(
                renderToImage(index),
                "png",
                imageFile
        );
    }

    @Override
    public BufferedImage renderToImage(int frameIndex) {
        return prepareImage(frameIndex);
    }

    //TODO: actual documentation
    // @Override
    public AudioWaveformRenderer withFade(Fade globalFade) {
        this.globalFade = globalFade;
        return this;
    }

    private Graphics2D render(Graphics2D graphics, int cyclicalCanvasLength, int offset, Fade fadeRate) {


        //preserve starting color for additional renders.
        Color currentColor = new Color(
                (canvas.initialColor().getRed() + canvas.shift().getRed()) % 255,
                (canvas.initialColor().getGreen() + canvas.shift().getGreen()) % 255,
                (canvas.initialColor().getBlue() + canvas.shift().getBlue()) % 255
        );

        graphics.setColor(currentColor);

        for (int i = 0 ; i < rawWaveform.size() && i < cyclicalCanvasLength; i++) {
            currentColor = new Color((currentColor.getRed() + canvas.shift().getRed()) % 255,
                    (currentColor.getGreen() + canvas.shift().getGreen()) % 255,
                    (currentColor.getBlue() + canvas.shift().getBlue()) % 255);

            int fadeScale = offset%cyclicalCanvasLength - i;
            if(fadeScale < 0) {
                fadeScale = offset%cyclicalCanvasLength + cyclicalCanvasLength - i;
            }

            graphics.setColor(new Color(
                    fadeComponent(currentColor.getRed(), canvas.globalBackground().getRed(), fadeRate.redFade(), fadeScale, canvas.imageBounds().width),
                    fadeComponent(currentColor.getGreen(), canvas.globalBackground().getGreen(), fadeRate.greenFade(), fadeScale, canvas.imageBounds().width),
                    fadeComponent(currentColor.getBlue(), canvas.globalBackground().getBlue(), fadeRate.blueFade(), fadeScale, canvas.imageBounds().width)
            ));

            Line2D.Double line = rawWaveform.get(offset - cyclicalCanvasLength + (i*60));
            line.setLine(i, line.y1, i, line.y2);
            graphics.draw(line);
        }
        return graphics;
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

    private BufferedImage prepareImage (int offset) {

        BufferedImage image = new BufferedImage(canvas.imageBounds().width,
                canvas.imageBounds().height,
                BufferedImage.TYPE_INT_RGB);
        render(
                prepareCanvas(image),
                canvas.imageBounds().width,
                offset,
                globalFade
        ).dispose();
        return image;
    }
}
