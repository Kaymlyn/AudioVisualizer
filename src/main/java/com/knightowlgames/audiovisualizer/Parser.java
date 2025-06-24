package com.knightowlgames.audiovisualizer;

import lombok.NoArgsConstructor;

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

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Parser {
    AudioInputStream audioInputStream;
    double duration;

    File file;
    String fileName;
    String waveformFilename;
    Color imageBackgroundColor;

    public Parser(String fileRoot, String fileName,Color globalBackground) {
        this.fileName = fileRoot + fileName + ".wav";
        this.waveformFilename = fileRoot + fileName + ".png";
        file = new File(this.fileName);
        imageBackgroundColor = globalBackground;
    }

    public void createAudioInputStream(int width, int height) throws UnsupportedAudioFileException, IOException {
        audioInputStream = AudioSystem.getAudioInputStream(file);
        duration = (long)((audioInputStream.getFrameLength() * 1000) / audioInputStream.getFormat().getFrameRate()) / 1000.0;
        new SamplingGraph().createWaveForm(audioInputStream, new Rectangle(width,height), waveformFilename);
    }
    /**
     * Render a WaveForm.
     */
    @NoArgsConstructor
    class SamplingGraph {
        private final Font font12 = new Font("serif", Font.PLAIN, 12);
        Color jfcBlue = new Color(0, 0, 255);
        Color shift = new Color(71, 4, 2);

        public void createWaveForm(AudioInputStream audioInputStream, Rectangle imageBounds, String destination) throws IOException {

            AudioFormat format = audioInputStream.getFormat();
            byte[] audioBytes = new byte[
                    (int) (audioInputStream.getFrameLength()
                            * format.getFrameSize())];

            System.out.println(audioInputStream.read(audioBytes) + " bytes read");

            int[] audioData = translate(format, audioBytes);

            //pre render, refactor to return the Vector array from a method to componentize the action since it's a discrete concern.
            int max = Arrays.stream(audioData)
                    .map(Math::abs)
                    .boxed()
                    .sorted(Comparator.naturalOrder())
                    .toList()
                    .getLast();

            List<Integer> balanced = Arrays.stream(audioData)
                    .map(value -> (imageBounds.height * value/ max))
                    .boxed()
                    .toList();

            Vector<Line2D.Double> lines = new Vector<>();

            int y_last = 0;
            for (int x = 0; x < imageBounds.width; x++) {
                Integer value = balanced.get((audioBytes.length / format.getFrameSize()/imageBounds.width) * format.getChannels() * x);
                //scale data to the viewport height
                int y_new = imageBounds.height * (128 - value) / 256;
                //add vertical line to array offset by x pixels.
                lines.add(new Line2D.Double(x, y_last, x, y_new));
                //return the last height
                y_last = y_new;
            }

            // Write generated image to a file
            // Save as PNG
            ImageIO.write(
                    renderWaveform(lines, imageBounds, true),
                    "png",
                    new File(destination));
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
                        int MSB = audioBytes[2*i];
                        /* Second byte is LSB (low order) */
                        int LSB = audioBytes[2*i+1];
                        audioData[i] = MSB << 8 | (255 & LSB);
                    }
                } else {
                    for (int i = 0; i < nlengthInSamples; i++) {
                        /* First byte is LSB (low order) */
                        int LSB = audioBytes[2*i];
                        /* Second byte is MSB (high order) */
                        int MSB = audioBytes[2*i+1];
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

        private void renderInfoBox(Graphics2D g2, Rectangle imageBounds, Font font, String info, Color backgroundColor, Color textColor) {

            FontMetrics fontMetrics =  new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB)
                    .getGraphics()
                    .getFontMetrics(font);
            Rectangle2D stringBounds = fontMetrics.getStringBounds(info,g2);

            g2.setColor(backgroundColor);
            g2.fillRect(0, imageBounds.height-(int)(stringBounds.getHeight()), (int)stringBounds.getWidth() + 4, imageBounds.height + (int)stringBounds.getHeight() + 4);
            g2.setColor(textColor);
            g2.setFont(font);
            g2.drawString(info, 3, imageBounds.height-4);
        }

        private BufferedImage renderWaveform(Vector<Line2D.Double> lines, Rectangle imageBounds, boolean withInfo) {
            BufferedImage bufferedImage = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);

            Graphics2D g2 = bufferedImage.createGraphics();

            g2.setBackground(imageBackgroundColor);
            g2.clearRect(0, 0, imageBounds.width, imageBounds.height);

            if(withInfo) {
                renderInfoBox(g2,
                        imageBounds,
                        font12,
                        "File: " + fileName + "  Length: " + duration + " seconds",
                        Color.WHITE,
                        Color.BLUE);
            }

            // .. render sampling graph ..
            jfcBlue = new Color((jfcBlue.getRed() + shift.getRed())%255,(jfcBlue.getGreen() + shift.getGreen())%255, (jfcBlue.getBlue() + shift.getBlue())%255);
            g2.setColor(jfcBlue);
            for (int i = 1; i < lines.size(); i++) {
                jfcBlue = new Color((jfcBlue.getRed() + shift.getRed())%255,(jfcBlue.getGreen() + shift.getGreen())%255, (jfcBlue.getBlue() + shift.getBlue())%255);
                g2.setColor(jfcBlue);
                g2.draw(lines.get(i));
            }

            g2.dispose();
            return bufferedImage;
        }

    } // End class SamplingGraph
}