package io.github.danielytbr.audiovisualizer;

public class VisualizerConfig {
    public int barCount = 48;
    public float heightScale = 0.4f;
    public float smoothingFactor = 0.7f;
    public int updateIntervalMs = 1024;
    public int colorArgb = 0xFF00FFFF; // cyan
    public int backgroundColor = 0xFF222222; // dark gray
    public boolean toasts = false;
    public boolean useBuiltinFile = false;
    public String filePath = "/sdcard/Music/test.mp3";
}
