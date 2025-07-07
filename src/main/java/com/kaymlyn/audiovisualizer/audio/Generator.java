package com.kaymlyn.audiovisualizer.audio;

import java.util.List;

public interface Generator <T extends Renderer> {
    T generate();
    List<? extends Number> balance(int[] data);
}
