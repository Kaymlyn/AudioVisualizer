package com.knightowlgames.audiovisualizer;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import static com.knightowlgames.audiovisualizer.WaveformRenderer.InfoBlock;
import static com.knightowlgames.audiovisualizer.WaveformRenderer.Canvas;

public class Render {
    
    private final int[] audioData;
    private final AudioFormat format;
    private final Canvas canvas;
    private final InfoBlock info;
    
    Render(byte[] audioBytes, AudioFormat format, TimeUnit timeUnit, int duration, Canvas canvas, InfoBlock info) {

        audioData = translate(format, Arrays.copyOf(
                        audioBytes,
                        (int)(timeUnit.toMillis(duration) * format.getSampleRate() * format.getFrameSize())
                ));
        this.format = format;
        this.canvas = canvas;
        this.info = info;
    }

    Render(byte[] audioBytes, AudioFormat format, Canvas canvas, InfoBlock info) {

        audioData = translate(format, Arrays.copyOf(
                audioBytes, audioBytes.length)
        );
        this.format = format;
        this.canvas = canvas;
        this.info = info;
    }
    
    private Vector<Line2D.Double> constructWaveForm() {

        int max = Arrays.stream(audioData)
                .map(Math::abs)
                .boxed()
                .sorted(Comparator.naturalOrder())
                .toList()
                .getLast();

        List<Integer> balanced = Arrays.stream(audioData)
                .map(value -> (canvas.imageBounds().height * value / max))
                .boxed()
                .toList();

        Vector<Line2D.Double> lines = new Vector<>();

        int y_last = 0;
        for (int x = 0; x < canvas.imageBounds().width; x++) {
            Integer value = balanced.get((audioData.length / format.getFrameSize() / canvas.imageBounds().width) * format.getChannels() * x);
            //scale data to the viewport height
            int y_new = canvas.imageBounds().height * (128 - value) / 256;
            //add vertical line to array offset by x pixels.
            lines.add(new Line2D.Double(x, y_last, x, y_new));
            //return the last height
            y_last = y_new;
        }
        return lines;
    }

    //translates data from byte input into an array of integers based on the audio format. This is the magic.
    //If you wrote this please reach out with where you published it, so I can give proper credit.
    private int[] translate(AudioFormat format, byte[] audioBytes) {

        int[] audioData = null;
        if (format.getSampleSizeInBits() == 16) {
            int nlengthInSamples = audioBytes.length / 2;
            audioData = new int[nlengthInSamples];
            if (format.isBigEndian()) {
                for (int i = 0; i < nlengthInSamples; i++) {
                    /* First byte is MSB (high order) */
                    int MSB = audioBytes[2 * i];
                    /* Second byte is LSB (low order) */
                    int LSB = audioBytes[2 * i + 1];
                    audioData[i] = MSB << 8 | (255 & LSB);
                }
            } else {
                for (int i = 0; i < nlengthInSamples; i++) {
                    /* First byte is LSB (low order) */
                    int LSB = audioBytes[2 * i];
                    /* Second byte is MSB (high order) */
                    int MSB = audioBytes[2 * i + 1];
                    audioData[i] = MSB << 8 | (255 & LSB);
                }
            }
        } else if (format.getSampleSizeInBits() == 8) {
            int nlengthInSamples = audioBytes.length;
            audioData = new int[nlengthInSamples];
            if (format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = audioBytes[i];
                }
            } else {
                for (int i = 0; i < audioBytes.length; i++) {
                    audioData[i] = audioBytes[i] - 128;
                }
            }
        }
        return audioData;
    }

    private Graphics2D renderWaveform(Graphics2D graphics, Vector<Line2D.Double> lines) {
        return renderWaveform(graphics, lines, -1,new Fade(0,0,0));
    }

    private Graphics2D renderWaveform(Graphics2D graphics, Vector<Line2D.Double> lines, int lineLimit, Fade fadeRate) {
        int totalLines;

        if(lineLimit < 0 ) {
            totalLines = lines.size();
        } else {
            totalLines = lineLimit;
        }
        // .. render sampling graph ..
        Color currentColor = new Color(
                (canvas.initialColor().getRed() + canvas.shift().getRed()) % 255,
                (canvas.initialColor().getGreen() + canvas.shift().getGreen()) % 255,
                (canvas.initialColor().getBlue() + canvas.shift().getBlue()) % 255
        );

        graphics.setColor(currentColor);
        for (int i = 1; i < lines.size() && i < totalLines; i++) {
            currentColor = new Color((currentColor.getRed() + canvas.shift().getRed()) % 255,
                    (currentColor.getGreen() + canvas.shift().getGreen()) % 255,
                    (currentColor.getBlue() + canvas.shift().getBlue()) % 255);
            graphics.setColor(fade(currentColor, fadeRate, totalLines - i));
            graphics.draw(lines.get(i));
        }
        return graphics;
    }

    private Color fade(Color current, Fade fadeRate, int fadeScale) {
        return new Color(
                (int) Math.max(current.getRed() - (fadeRate.redFade * fadeScale), canvas.globalBackground().getRed()),
                (int) Math.max(current.getGreen() - (fadeRate.greenFade * fadeScale), canvas.globalBackground().getGreen()),
                (int) Math.max(current.getBlue() - (fadeRate.blueFade * fadeScale), canvas.globalBackground().getBlue())
        );
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
        renderWaveform(
                prepareCanvas(image),
                constructWaveForm(),
                linesToRender,
                fade
        ).dispose();
        return image;
    }
    
    public void saveToFile(File imageFile) throws IOException {

        // Write generated image to a file
        // Save as PNG
        
        if(!imageFile.getParentFile().exists()){
            if(!imageFile.mkdirs()) {
                throw new IOException("Unable to create file to store image.");
            }
        }

        ImageIO.write(
                prepareImage(
                        -1,
                        new Fade(.5,.5,.5)
                ),
                "png",
                imageFile
        );
    }

    public Render withFadeAdjustments(Fade globalFade) {
        reurn this;
    }

    public Render withPercentageAdjustments()

    public record Fade(double redFade, double greenFade, double blueFade) {}
}
