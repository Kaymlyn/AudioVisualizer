package com.knightowlgames.audiovisualizer;

import lombok.NoArgsConstructor;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Parser {
    AudioInputStream audioInputStream;
    String errStr;
    double duration, seconds;
    File file;
    String fileName;
    SamplingGraph samplingGraph;
    String waveformFilename;
    Color imageBackgroundColor = new Color(20,20,20);

    public Parser(String fileRoot, String fileName) throws UnsupportedAudioFileException, IOException {
        this.fileName = fileRoot + fileName + ".wav";
        file = new File(this.fileName);
        this.waveformFilename = fileRoot + fileName + ".png";
    }

    public void createAudioInputStream() throws Exception {
        if (file != null && file.isFile()) {
            try {
                errStr = null;
                audioInputStream = AudioSystem.getAudioInputStream(file);
                long milliseconds = (long)((audioInputStream.getFrameLength() * 1000) / audioInputStream.getFormat().getFrameRate());
                duration = milliseconds / 1000.0;
                samplingGraph = new SamplingGraph();
                samplingGraph.createWaveForm(audioInputStream,200,6000, waveformFilename);
            } catch (Exception ex) {
                reportStatus(ex.toString());
                throw ex;
            }
        } else {
            reportStatus("Audio file required.");
        }
    }
    /**
     * Render a WaveForm.
     */
    @NoArgsConstructor
    class SamplingGraph {
        private final Font font12 = new Font("serif", Font.PLAIN, 12);
        Color jfcBlue = new Color(0, 0, 255);
        Color shift = new Color(71, 4, 2);

        public void createWaveForm(AudioInputStream audioInputStream, int height, int width, String destination) throws IOException {

            //This should be handled as a different concern and should be broken out from the core renderer since it's kinda optional
            int infopad = 15;

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
                    .map(value -> ((height - infopad) * value/ max))
                    .boxed()
                    .toList();

            Vector<Line2D.Double> lines = new Vector<>();

            int y_last = 0;
            for (int x = 0; x < width; x++) {
                Integer value = balanced.get((audioBytes.length / format.getFrameSize()/width) * format.getChannels() * x);
                //scale data to the viewport height
                int y_new = height * (128 - value) / 256;
                //add vertical line to array offset by x pixels.
                lines.add(new Line2D.Double(x, (double) y_last - 15, x, y_new - ((double) 15 /2)));
                //return the last height
                y_last = y_new;
            }

            // Write generated image to a file
            // Save as PNG
            ImageIO.write(
                    renderWaveform(lines, width, height, infopad),
                    "png",
                    new File(destination));
        }

        //translates data from byte input into an array of integers based on the
        //audio format. This is the magic.
        public int[] translate(AudioFormat format, byte[] audioBytes) {

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


        private BufferedImage renderWaveform(Vector<Line2D.Double> lines, int viewportWidth, int viewportHeight, int INFOPAD) {
            BufferedImage bufferedImage = new BufferedImage(viewportWidth, viewportHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bufferedImage.createGraphics();

            g2.setBackground(imageBackgroundColor);
            g2.clearRect(0, 0, viewportWidth, viewportHeight);
            g2.setColor(Color.white);
            g2.fillRect(0, viewportHeight-INFOPAD, viewportWidth, viewportHeight + INFOPAD);

            g2.setColor(Color.black);
            g2.setFont(font12);
            g2.drawString("File: " + fileName + "  Length: " + duration + "  Position: " + seconds, 3, viewportHeight-4);

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

    private void reportStatus(String msg) {
        if ((errStr = msg) != null) {
            System.out.println(errStr);
        }
    }
}