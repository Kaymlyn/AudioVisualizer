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
    private double globalRenderPercentage;

    public AudioWaveformRenderer(List<Line2D.Double> waveformData, Canvas canvas, InfoBlock info, Fade fade, double renderPercentage) {

        this.rawWaveform = waveformData;
        this.canvas = canvas;
        this.info = info;
        this.globalFade = fade;
        this.globalRenderPercentage = renderPercentage;
    }

    public AudioWaveformRenderer(List<Line2D.Double> waveformData, Canvas canvas, InfoBlock info) {
        this(waveformData,canvas,info,new Fade(0,0,0), -1);
    }

    //this method is unintelligent, need to add a way to change the image format, this is handled currently by a magic
    //string, I need to find out if there is an enumeration that provides all the available image formats.
    //this will need to be refactored when video is generated.
    @Override
    public void renderToFile(File imageFile) throws IOException {

        if(!imageFile.getParentFile().exists()){
            if(!imageFile.mkdirs()) {
                throw new IOException("Unable to create file to store image.");
            }
        }

        ImageIO.write(
                renderToImage(),
                "png",
                imageFile
        );
    }

    @Override
    public BufferedImage renderToImage() {
        return prepareImage(
                globalRenderPercentage,
                globalFade
        );
    }

    //TODO: actual documentation
    // @Override
    public AudioWaveformRenderer withFade(Fade globalFade) {
        this.globalFade = globalFade;
        return this;
    }

    public AudioWaveformRenderer renderPercentage(double percent ) {
        this.globalRenderPercentage = percent;
        return this;
    }


    private Graphics2D render(Graphics2D graphics, int lineLimit, Fade fadeRate) {
        //total number of lines from the left to render. Fade is not considered in this calculation
        int totalLines;
        if(lineLimit < 0 || lineLimit > rawWaveform.size()) {
            totalLines = rawWaveform.size();
        } else {
            totalLines = lineLimit;
        }

        //preserve starting color for additional renders.
        Color currentColor = new Color(
                (canvas.initialColor().getRed() + canvas.shift().getRed()) % 255,
                (canvas.initialColor().getGreen() + canvas.shift().getGreen()) % 255,
                (canvas.initialColor().getBlue() + canvas.shift().getBlue()) % 255
        );

        graphics.setColor(currentColor);

        //fade is calculated based on the overall image. need to identify a way to set an offset. Ideally, if the
        //waveform stops at the middle of the canvas and the fade rate would require the whole canvas to complete
        //the waveform at the edge of the canvas should only be half faded.
        for (int i = 1; i < rawWaveform.size() && i < totalLines; i++) {
            currentColor = new Color((currentColor.getRed() + canvas.shift().getRed()) % 255,
                    (currentColor.getGreen() + canvas.shift().getGreen()) % 255,
                    (currentColor.getBlue() + canvas.shift().getBlue()) % 255);

            graphics.setColor(new Color(
                    fadeComponent(currentColor.getRed(), canvas.globalBackground().getRed(), fadeRate.redFade(), totalLines - i),
                    fadeComponent(currentColor.getGreen(), canvas.globalBackground().getGreen(), fadeRate.greenFade(), totalLines - i),
                    fadeComponent(currentColor.getBlue(), canvas.globalBackground().getBlue(), fadeRate.blueFade(), totalLines - i)
            ));
            graphics.draw(rawWaveform.get(i));
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

    private BufferedImage prepareImage (double percentage, Fade fade) {

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
