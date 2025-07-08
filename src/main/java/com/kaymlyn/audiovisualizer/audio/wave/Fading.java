package com.kaymlyn.audiovisualizer.audio.wave;

import com.kaymlyn.audiovisualizer.audio.Renderer;

public interface  Fading<T extends Renderer> {

    record Fade(double redFade, double greenFade, double blueFade) {}

    T withFade(Fade fade);

    default int fadeComponent(int raw, int target, double fadeRate, int fadePoint, int canvasLength) {
        // a fade rate of 1 means a fade that stretches the whole canvas.
        // a fade rate of .5 means a fade that is complete after halfway across the canvas
        // a fade rate of 2 means a fade that would be complete after double the length of the canvas
        // a fade rate that is negative would switch the directionality
        //      e.g. image fades from left to right vs right to left like it does currently

        double adjuster = fadeRate * fadePoint/canvasLength;
        if(adjuster > 1) {
            return target;
        } else {
            return (int) (raw + ((target - raw) * adjuster));
        }
    }
}
