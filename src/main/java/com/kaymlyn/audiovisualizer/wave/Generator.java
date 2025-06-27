package com.kaymlyn.audiovisualizer.wave;

public interface Generator <T extends Renderer> {
    T generate();
}
