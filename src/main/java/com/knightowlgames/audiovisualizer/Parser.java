package com.knightowlgames.audiovisualizer;

import lombok.NoArgsConstructor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.*;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;




public class Parser {
    AudioInputStream audioInputStream;
    Vector<Line2D.Double> lines = new Vector<>();
    String errStr;
    Capture capture = new Capture();
    double duration, seconds;
    File file;
    String fileName;
    SamplingGraph samplingGraph;
    String waveformFilename;
    Color imageBackgroundColor = new Color(20,20,20);

    public Parser(String fileRoot, String fileName) throws UnsupportedAudioFileException, IOException {
        this.fileName = fileRoot + fileName + ".wav";
        System.out.println(this.fileName);
        file = new File(this.fileName);
        this.waveformFilename = fileRoot + fileName + ".png";
    }

    public void createAudioInputStream() throws Exception {
        if (file != null && file.isFile()) {
            try {
                errStr = null;
                audioInputStream = AudioSystem.getAudioInputStream(file);
                fileName = file.getName();
                long milliseconds = (long)((audioInputStream.getFrameLength() * 1000) / audioInputStream.getFormat().getFrameRate());
                duration = milliseconds / 1000.0;
                samplingGraph = new SamplingGraph();
                samplingGraph.createWaveForm(null);
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
    class SamplingGraph implements Runnable {

        private Thread thread;
//        private final Font font10 = new Font("serif", Font.PLAIN, 10);
        private final Font font12 = new Font("serif", Font.PLAIN, 12);
        Color jfcBlue = new Color(000, 000, 255);
        Color shift = new Color(71,004, 002);
        Color pink = new Color(255, 175, 175);

        public void createWaveForm(byte[] audioBytes) {

            lines.removeAllElements();  // clear the old vector

            AudioFormat format = audioInputStream.getFormat();
            if (audioBytes == null) {
                try {
                    audioBytes = new byte[
                            (int) (audioInputStream.getFrameLength()
                                    * format.getFrameSize())];
                    audioInputStream.read(audioBytes);
                } catch (Exception ex) {
                    reportStatus(ex.getMessage());
                    return;
                }
            }
            int w = 6000;
            int h = 200;
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
                if (format.getEncoding().toString().startsWith("PCM_SIGN")) {
                    for (int i = 0; i < audioBytes.length; i++) {
                        audioData[i] = audioBytes[i];
                    }
                } else {
                    for (int i = 0; i < audioBytes.length; i++) {
                        audioData[i] = audioBytes[i] - 128;
                    }
                }
            }

            int frames_per_pixel = audioBytes.length / format.getFrameSize()/w;
            List<Integer> balancedAudio = balance(audioData,h);
            int y_last = 0;
            for (int x = 0; x < w; x++) {
                y_last = addLine(y_last, x, balancedAudio.get(frames_per_pixel * format.getChannels() * x), h, 15);
            }
            try {
                saveToFile(waveformFilename,h,w,15);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public int findAbsoluteMax(int[] data) {
            List<Integer> dataList = new ArrayList<>(Arrays.stream(data).map(Math::abs).boxed().toList());
            dataList.sort(Comparator.naturalOrder());
            System.out.println(dataList.getLast());
            return dataList.getLast();
        }

        public List<Integer> balance(int[] data, int maximum) {
            List<Integer> balanced = new ArrayList<>();
            int absMax = findAbsoluteMax(data);
            Arrays.stream(data).forEach(value -> balanced.add(maximum*value/absMax));
            return balanced;
        }

        int addLine(double y_last, int x, int audioData, int viewportHeight, int verticalOffset) {

//            System.out.println(audioData);
            //scale data to the viewport height
            int y_new = viewportHeight * (128 - audioData) / 256;
            //add vertical line to array offset by x pixels.
            lines.add(new Line2D.Double(x, y_last - ((double) verticalOffset /2), x, y_new - ((double) verticalOffset /2)));
            //return the last height
            return y_new;
        }

        public void saveToFile(String filename, int viewportHeight, int viewportWidth, int infopadHeight) throws IOException {

            BufferedImage bufferedImage = new BufferedImage(viewportWidth, viewportHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = bufferedImage.createGraphics();

            createSampleOnGraphicsContext(viewportWidth, viewportHeight, infopadHeight, g2);
            g2.dispose();

            // Write generated image to a file
            // Save as PNG
            File file = new File(filename);
            ImageIO.write(bufferedImage, "png", file);
        }


        private void createSampleOnGraphicsContext(int viewportWidth, int viewportHeight, int INFOPAD, Graphics2D g2) {
            g2.setBackground(imageBackgroundColor);
            g2.clearRect(0, 0, viewportWidth, viewportHeight);
            g2.setColor(Color.white);
            g2.fillRect(0, viewportHeight-INFOPAD, viewportWidth, viewportHeight + INFOPAD);

            if (errStr != null) {
//                jfcBlue = new Color((jfcBlue.getRed() + shift.getRed())%255,(jfcBlue.getGreen() + shift.getGreen())%255, (jfcBlue.getBlue() + shift.getBlue())%255);
                System.out.println(jfcBlue);
                g2.setColor(jfcBlue);
                g2.setFont(new Font("serif", Font.BOLD, 18));
                g2.drawString("ERROR", 5, 20);
                AttributedString as = new AttributedString(errStr);
                as.addAttribute(TextAttribute.FONT, font12, 0, errStr.length());
                AttributedCharacterIterator aci = as.getIterator();
                FontRenderContext frc = g2.getFontRenderContext();
                LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
                float x = 5, y = 25;
                lbm.setPosition(0);
                while (lbm.getPosition() < errStr.length()) {
                    TextLayout tl = lbm.nextLayout(viewportWidth-x-5);
                    if (!tl.isLeftToRight()) {
                        x = viewportWidth - tl.getAdvance();
                    }
                    tl.draw(g2, x, y += tl.getAscent());
                    y += tl.getDescent() + tl.getLeading();
                }
            } else if (capture.thread != null) {
                g2.setColor(Color.black);
                g2.setFont(font12);
                g2.drawString("Length: " + String.valueOf(seconds), 3, viewportHeight-4);
            } else {
                g2.setColor(Color.black);
                g2.setFont(font12);
                g2.drawString("File: " + fileName + "  Length: " + String.valueOf(duration) + "  Position: " + String.valueOf(seconds), 3, viewportHeight-4);

                if (audioInputStream != null) {
                    // .. render sampling graph ..
                    jfcBlue = new Color((jfcBlue.getRed() + shift.getRed())%255,(jfcBlue.getGreen() + shift.getGreen())%255, (jfcBlue.getBlue() + shift.getBlue())%255);
                    System.out.println(jfcBlue);
                    g2.setColor(jfcBlue);
                    for (int i = 1; i < lines.size(); i++) {
                        jfcBlue = new Color((jfcBlue.getRed() + shift.getRed())%255,(jfcBlue.getGreen() + shift.getGreen())%255, (jfcBlue.getBlue() + shift.getBlue())%255);
                        g2.setColor(jfcBlue);
                        g2.draw(lines.get(i));
                    }

                    // .. draw current position ..
                    if (seconds != 0) {
                        double loc = seconds/duration*viewportWidth;
                        g2.setColor(pink);
                        g2.setStroke(new BasicStroke(3));
                        g2.draw(new Line2D.Double(loc, 0, loc, viewportHeight-INFOPAD-2));
                    }
                }
            }
        }

        public void start() {
            thread = new Thread(this);
            thread.setName("SamplingGraph");
            thread.start();
            seconds = 0;
        }

        public void stop() {
            if (thread != null) {
                thread.interrupt();
            }
            thread = null;
        }

        public void run() {
            seconds = 0;
            while (thread != null) {
                if ( (capture.line != null) && (capture.line.isActive()) ) {
                    long milliseconds = capture.line.getMicrosecondPosition() / 1000;
                    seconds =  milliseconds / 1000.0;
                }
                try { Thread.sleep(100); } catch (Exception e) { break; }
                while ((capture.line != null && !capture.line.isActive()))
                {
                    try { Thread.sleep(10); } catch (Exception e) { break; }
                }
            }
            seconds = 0;
        }
    } // End class SamplingGraph

    /**
     * Reads data from the input channel and writes to the output stream
     */
    class Capture implements Runnable {

        TargetDataLine line;
        Thread thread;

        public void start() {
            errStr = null;
            thread = new Thread(this);
            thread.setName("Capture");
            thread.start();
        }

        public void stop() {
            thread = null;
        }

        private void shutDown(String message) {
            if ((errStr = message) != null && thread != null) {
                thread = null;
                samplingGraph.stop();
                System.err.println(errStr);
            }
        }

        public void run() {

            duration = 0;
            audioInputStream = null;

            // define the required attributes for our line,
            // and make sure a compatible line is supported.

            AudioFormat format = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                    format);

            if (!AudioSystem.isLineSupported(info)) {
                shutDown("Line matching " + info + " not supported.");
                return;
            }

            // get and open the target data line for capture.

            try {
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format, line.getBufferSize());
            } catch (LineUnavailableException ex) {
                shutDown("Unable to open the line: " + ex);
                return;
            } catch (Exception ex) {
                shutDown(ex.toString());
                return;
            }

            // play back the captured audio data
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int frameSizeInBytes = format.getFrameSize();
            int bufferLengthInFrames = line.getBufferSize() / 8;
            int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
            byte[] data = new byte[bufferLengthInBytes];
            int numBytesRead;

            line.start();

            while (thread != null) {
                if((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
                    break;
                }
                out.write(data, 0, numBytesRead);
            }

            // we reached the end of the stream.  stop and close the line.
            line.stop();
            line.close();
            line = null;

            // stop and close the output stream
            try {
                out.flush();
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // load bytes into the audio input stream for playback

            byte[] audioBytes = out.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
            audioInputStream = new AudioInputStream(bais, format, audioBytes.length / frameSizeInBytes);

            long milliseconds = (long)((audioInputStream.getFrameLength() * 1000) / format.getFrameRate());
            duration = milliseconds / 1000.0;

            try {
                audioInputStream.reset();
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            samplingGraph.createWaveForm(audioBytes);
        }
    } // End class Capture


    private void reportStatus(String msg) {
        if ((errStr = msg) != null) {
            System.out.println(errStr);
        }
    }
}