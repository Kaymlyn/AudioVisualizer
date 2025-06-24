package com.knightowlgames.audiovisualizer;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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

/**
 * Render a WaveForm.
 */
class WaveformRenderer {
    private final AudioInputStream audio;
    private final Rectangle imageBounds;
    private final Font infoFont;
    private final Color globalBackground;
    private final Color initialColor;
    private final Color shift;
    private final Color infoInsetColor;
    private final double duration;

    public WaveformRenderer(AudioInputStream audio, Rectangle imageBounds, Color backgroundColor, Color initialWaveformColor, Color shift, Font infoFont, Color infoInsetColor) {
        this.audio = audio;
        this.imageBounds = imageBounds;
        this.infoFont = infoFont;
        this.globalBackground = backgroundColor;
        this.initialColor = initialWaveformColor;
        this.infoInsetColor = infoInsetColor;
        this.shift = shift;
        duration = (long)((this.audio.getFrameLength() * 1000) / this.audio.getFormat().getFrameRate()) / 1000.0;
    }
    public void createWaveForm(String destination) throws IOException {
        createWaveForm(destination, false, null, 0);
    }

    public void createWaveForm(String destination, boolean partial, TimeUnit unit, int duration) throws IOException {

        //Hz 48000 = 1 second
        //192000 bytes = 1 second
        AudioFormat format = audio.getFormat();

        byte[] audioBytes;
        if(partial) {
            audioBytes = new byte[(int)(unit.toSeconds(duration) * 48000 * 4)];
        } else {
            audioBytes = new byte[
                (int) (audio.getFrameLength()
                        * format.getFrameSize())];
        }

        System.out.println("AudioFormat: " + format + " " + audio.read(audioBytes) + " bytes read");

        // Write generated image to a file
        // Save as PNG
        File imageFile = new File(destination);
        if(!imageFile.getParentFile().exists()){
            if(!imageFile.mkdirs()) {
                throw new IOException("Unable to create file to store image.");
            }
        }
        ImageIO.write(
                createWaveformImage(
                        constructWaveForm(
                                translate(format, audioBytes),
                                audioBytes,
                                format,
                                TimeUnit.SECONDS,
                                1),
                        destination,
                        true),
                "png",
                new File(destination));
    }

    private Vector<Line2D.Double> constructWaveForm(int[] audioData, byte[] audioBytes, AudioFormat format, TimeUnit unit, int sampleLength) {

        int[] data =Arrays.copyOf(audioData, (int)(unit.toMillis(1) * format.getFrameSize() * format.getSampleRate()));

        int max = Arrays.stream(data)
                .map(Math::abs)
                .boxed()
                .sorted(Comparator.naturalOrder())
                .toList()
                .getLast();

        List<Integer> balanced = Arrays.stream(data)
                .map(value -> (imageBounds.height * value / max))
                .boxed()
                .toList();

        Vector<Line2D.Double> lines = new Vector<>();

        int y_last = 0;
        for (int x = 0; x < imageBounds.width; x++) {
            Integer value = balanced.get((audioData.length / format.getFrameSize() / imageBounds.width) * format.getChannels() * x);
            //scale data to the viewport height
            int y_new = imageBounds.height * (128 - value) / 256;
            //add vertical line to array offset by x pixels.
            lines.add(new Line2D.Double(x, y_last, x, y_new));
            //return the last height
            y_last = y_new;
        }
        return lines;
    }

    //translates data from byte input into an array of integers based on the
    //audio format. This is the magic. If you wrote this please reach out to where you published it, so I can give
    //proper credit
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

    private Graphics2D prepareCanvas(BufferedImage image, String destination, boolean withInfo) {
        Graphics2D g2 = image.createGraphics();
        
        g2.setBackground(globalBackground);
        g2.clearRect(0, 0, imageBounds.width, imageBounds.height);
        
        if (withInfo) {

            FontMetrics fontMetrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                    .getGraphics()
                    .getFontMetrics(infoFont);
            Rectangle2D stringBounds = fontMetrics.getStringBounds("File: " + destination + "  Length: " + duration + " seconds", g2);

            g2.setColor(infoInsetColor);
            g2.fillRect(0, imageBounds.height - (int) (stringBounds.getHeight()), (int) stringBounds.getWidth() + 4, imageBounds.height + (int) stringBounds.getHeight() + 4);
            g2.setColor(Color.BLUE);
            g2.setFont(infoFont);
            g2.drawString("File: " + destination + "  Length: " + duration + " seconds", 3, imageBounds.height - 4);
        }
        return g2;
    }

    private BufferedImage createWaveformImage(Vector<Line2D.Double> lines, String destination, boolean withInfo) {

        BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
        renderWaveform(prepareCanvas(image, destination, withInfo), lines).dispose();
        return image;
    }

    private Graphics2D renderWaveform(Graphics2D canvas, Vector<Line2D.Double> lines) {
        // .. render sampling graph ..
        Color currentColor = new Color((initialColor.getRed() + shift.getRed()) % 255, (initialColor.getGreen() + shift.getGreen()) % 255, (initialColor.getBlue() + shift.getBlue()) % 255);
        canvas.setColor(currentColor);
        for (int i = 1; i < lines.size(); i++) {
            currentColor = new Color((currentColor.getRed() + shift.getRed()) % 255,
                    (currentColor.getGreen() + shift.getGreen()) % 255,
                    (currentColor.getBlue() + shift.getBlue()) % 255);
            canvas.setColor(currentColor);
            canvas.draw(lines.get(i));
        }
        return canvas;
    }

} // End class SamplingGraph
