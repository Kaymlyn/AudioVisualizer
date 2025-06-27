package com.knightowlgames.audiovisualizer.wave;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.knightowlgames.audiovisualizer.wave.WaveformRenderer.InfoBlock;
import static com.knightowlgames.audiovisualizer.wave.WaveformRenderer.Canvas;

public class Render {
    
    private final int[] audioData;
    private final AudioFormat format;
    private final Canvas canvas;
    private final InfoBlock info;
    private Fade globalFade;
    private double globalRenderPercentage;

    Render(byte[] audioBytes, AudioFormat format, TimeUnit timeUnit, double duration, Canvas canvas, InfoBlock info) {

        int desiredDataLength = (int)(
                timeUnit.toSeconds((long)(duration * 1000))/1000.0 * format.getSampleRate() * format.getFrameSize()
        );

        if(desiredDataLength < audioBytes.length && desiredDataLength > 0) {
            audioData = decodeAudio(format, Arrays.copyOf(audioBytes, desiredDataLength));
        } else {
            audioData = decodeAudio(format, Arrays.copyOf(audioBytes,audioBytes.length));
        }

        this.format = format;
        this.canvas = canvas;
        this.info = info;
        this.globalFade = new Fade(0,0,0);
        this.globalRenderPercentage = -1;
    }

    Render(byte[] audioBytes, AudioFormat format, Canvas canvas, InfoBlock info) {
        this(audioBytes,format,TimeUnit.SECONDS,-1,canvas,info);
    }

    //Generates the List that represents the raw waveform image from raw audio data
    private List<Line2D.Double> constructWaveForm() {

        //Super complicated way of finding the maximum integer in the audio data for normalization
        int max = Arrays.stream(audioData)
                .map(Math::abs)
                .boxed()
                .sorted(Comparator.naturalOrder())
                .toList()
                .getLast();

        //Scales all the integers in the audioData to the canvas height
        List<Integer> balanced = Arrays.stream(audioData)
                .map(value -> (canvas.imageBounds().height * value / max))
                .boxed()
                .toList();

        List<Line2D.Double> lines = new ArrayList<>();

        //Raw Waveform data generation.
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
    private int[] decodeAudio(AudioFormat format, byte[] audioBytes) {

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

    private Graphics2D renderWaveform(Graphics2D graphics, List<Line2D.Double> lines, int lineLimit, Fade fadeRate) {
        //total number of lines from the left to render. Fade is not considered in this calculation
        int totalLines;
        if(lineLimit < 0 || lineLimit > lines.size()) {
            totalLines = lines.size();
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
        for (int i = 1; i < lines.size() && i < totalLines; i++) {
            currentColor = new Color((currentColor.getRed() + canvas.shift().getRed()) % 255,
                    (currentColor.getGreen() + canvas.shift().getGreen()) % 255,
                    (currentColor.getBlue() + canvas.shift().getBlue()) % 255);

            graphics.setColor(new Color(
                    fadeComponent(currentColor.getRed(), canvas.globalBackground().getRed(), fadeRate.redFade, totalLines - i),
                    fadeComponent(currentColor.getGreen(), canvas.globalBackground().getGreen(), fadeRate.greenFade, totalLines - i),
                    fadeComponent(currentColor.getBlue(), canvas.globalBackground().getBlue(), fadeRate.blueFade, totalLines - i)
            ));
            graphics.draw(lines.get(i));
        }
        return graphics;
    }

    private int fadeComponent(int raw, int target, double fadeRate, int fadeScale) {
        //.01 is arbitrary and makes the fade globally linear. need to make this dynamically defined
        //My goal is to eventually make the fade rate = to some % of the total canvas.
        //Examples:
        // a fade rate of 1 means a fade that stretches the whole canvas.
        // a fade rate of .5 means a fade that is complete after halfway across the canvas
        // a fade rate of 2 means a fade that would be complete after double the length of the canvas
        // a fade rate that is negative would switch the directionality
        //      e.g. image fades from left to right vs right to left like it does currently
        // also need a way to turn off the fade.
        // currently not possible with this scheme since 0 is a completely empty canvas
        double adjuster = .01 * fadeRate * fadeScale;
        if(adjuster > 1) {
            return target;
        } else {
            return (int) (raw + ((target - raw) * adjuster));
        }
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

    //this method is unintelligent, need to add a way to change the image format, this is handled currently by a magic
    //string, I need to find out if there is an enumeration that provides all the available image formats.
    //this will need to be refactored when video is generated.
    public void saveToFile(File imageFile) throws IOException {

        if(!imageFile.getParentFile().exists()){
            if(!imageFile.mkdirs()) {
                throw new IOException("Unable to create file to store image.");
            }
        }

        ImageIO.write(
                prepareImage(
                        globalRenderPercentage,
                        globalFade
                ),
                "png",
                imageFile
        );
    }

    //TODO: actual documentation
    public Render withFade(Fade globalFade) {
        this.globalFade = globalFade;
        return this;
    }

    public Render renderPercentage(double percent ) {
        this.globalRenderPercentage = percent;
        return this;
    }

    public record Fade(double redFade, double greenFade, double blueFade) {}
}
