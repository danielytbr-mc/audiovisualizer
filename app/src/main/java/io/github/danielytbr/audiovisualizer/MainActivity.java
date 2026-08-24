// main
package io.github.danielytbr.audiovisualizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mPlayer;
    private Visualizer mVisualizer;
    private BarVisualizerView mVisualizerView;
    private boolean mFirstDataReceived = false;
    private VisualizerConfig config;

    // For manual FFT
    private float[] mFftReal;
    private float[] mFftImag;
    private int mCaptureSize = 512; // must be power of 2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVisualizerView = findViewById(R.id.visualizer_view);

        // Check RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return;
        }
        setupVisualizer();
    }

    private void setupVisualizer() {
        config = loadConfig();

        // Apply config to the view
        mVisualizerView.setBarCount(config.barCount);
        mVisualizerView.setHeightScale(config.heightScale);
        mVisualizerView.setSmoothingFactor(config.smoothingFactor);
        mVisualizerView.setColor(config.colorArgb);

        try {
            // ---------- Load audio file ----------
            mPlayer = new MediaPlayer();

            // If useBuiltinFile is true, load from res/raw/test
            if (config.useBuiltinFile) {
                // Using MediaPlayer.create() loads from resources and prepares automatically
                mPlayer = MediaPlayer.create(this, R.raw.test);
                if (mPlayer == null) {
                    showToast("❌ Built-in file not found (res/raw/test)");
                    return;
                }
            } else {
                // Load from external file path
                File audioFile = new File(config.filePath);
                if (!audioFile.exists()) {
                    showToast("❌ File not found: " + config.filePath);
                    return;
                }
                mPlayer.setDataSource(config.filePath);
                mPlayer.prepare(); // synchronous, blocks until ready
            }

            showToast("✅ File loaded! Duration: " + mPlayer.getDuration() + "ms");

            mPlayer.setLooping(true);
            mPlayer.start();

            if (!mPlayer.isPlaying()) {
                showToast("⚠️ Not playing – check volume or file codec");
                return;
            }
            showToast("🔊 Playing (looping)...");

            // ---------- Create Visualizer ----------
            int sessionId = mPlayer.getAudioSessionId();
            showToast("🎧 Session ID: " + sessionId);

            mVisualizer = new Visualizer(sessionId);

            // Capture size must be power of 2 for our FFT
            // Use the max possible that is a power of 2 (e.g., 512, 1024, 2048)
            int[] range = Visualizer.getCaptureSizeRange();
            int max = range[1];
            // Find largest power of 2 <= max
            int captureSize = 512;
            while (captureSize * 2 <= max) {
                captureSize *= 2;
            }
            mCaptureSize = captureSize;
            mVisualizer.setCaptureSize(mCaptureSize);
            showToast("📏 Capture size: " + mCaptureSize);

            // ---------- Set up data listener ----------
            int listenerRate = 1000 / config.updateIntervalMs; // Hz

            mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    // This is our main path (manual FFT)
                    processWaveform(waveform);
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    // If device supports hardware FFT, use it (skip manual)
                    // But we'll still convert to magnitudes and use our view
                    processHardwareFft(fft);
                }
            }, listenerRate, true, true); // waveform true, fft true

            // ---------- Enable Visualizer ----------
            mVisualizer.setEnabled(true);
            if (mVisualizer.getEnabled()) {
                showToast("📊 Visualizer ENABLED");
            } else {
                showToast("❌ Visualizer enable failed");
                return;
            }

            // ---------- Fallback timeout ----------
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!mFirstDataReceived) {
                    runOnUiThread(() ->
                            showToast("⏰ No data after 3s. Check audio file or permissions.")
                    );
                }
            }, 3000);

        } catch (Exception e) {
            showToast("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ---------- Manual FFT on waveform ----------
    private void processWaveform(byte[] waveform) {
        if (waveform.length != mCaptureSize) return;

        // Initialize arrays if needed
        if (mFftReal == null || mFftReal.length != mCaptureSize) {
            mFftReal = new float[mCaptureSize];
            mFftImag = new float[mCaptureSize];
        }

        // 1. Convert bytes to floats (signed 8-bit PCM)
        for (int i = 0; i < mCaptureSize; i++) {
            mFftReal[i] = waveform[i]; // byte is signed -128..127
            mFftImag[i] = 0.0f;
        }

        // 2. Apply Hann window
        FFT.applyHannWindow(mFftReal);

        // 3. Perform FFT (in-place)
        FFT.fft(mFftReal, mFftImag);

        // 4. Compute magnitudes (skip DC at bin 0)
        int numBins = mCaptureSize / 2;
        float[] magnitudes = new float[numBins - 1]; // skip DC
        float maxMag = 0;
        for (int i = 1; i < numBins; i++) {
            float real = mFftReal[i];
            float imag = mFftImag[i];
            float mag = (float) Math.sqrt(real * real + imag * imag);
            magnitudes[i - 1] = mag;
            if (mag > maxMag) maxMag = mag;
        }

        // Normalize (avoid division by zero)
        if (maxMag > 0) {
            for (int i = 0; i < magnitudes.length; i++) {
                magnitudes[i] = Math.min(1.0f, magnitudes[i] / maxMag);
            }
        }

        // 5. Group into bars with logarithmic spacing
        int barCount = config.barCount;
        float[] barMagnitudes = new float[barCount];
        for (int i = 0; i < magnitudes.length; i++) {
            // Map index to logarithmic bar index
            float logIndex = (float) (Math.log(i + 1) / Math.log(magnitudes.length + 1));
            int barIndex = (int) (logIndex * barCount);
            if (barIndex >= barCount) barIndex = barCount - 1;
            if (magnitudes[i] > barMagnitudes[barIndex]) {
                barMagnitudes[barIndex] = magnitudes[i];
            }
        }

        // 6. Update the view (on UI thread)
        runOnUiThread(() -> {
            if (!mFirstDataReceived) {
                mFirstDataReceived = true;
                showToast("🌊 Waveform → FFT processing active!");
            }
            mVisualizerView.updateMagnitudes(barMagnitudes);
        });
    }

    // ---------- Hardware FFT (if available) ----------
    private void processHardwareFft(byte[] fft) {
        // fft contains interleaved real/imag bytes (each 0..255)
        int numBins = fft.length / 2;
        float[] magnitudes = new float[numBins - 1]; // skip DC
        float maxMag = 0;
        for (int i = 1; i < numBins; i++) {
            float real = fft[2 * i] & 0xFF;
            float imag = fft[2 * i + 1] & 0xFF;
            float mag = (float) Math.sqrt(real * real + imag * imag);
            magnitudes[i - 1] = mag;
            if (mag > maxMag) maxMag = mag;
        }
        if (maxMag > 0) {
            for (int i = 0; i < magnitudes.length; i++) {
                magnitudes[i] = Math.min(1.0f, magnitudes[i] / maxMag);
            }
        }

        // Same logarithmic grouping as above
        int barCount = config.barCount;
        float[] barMagnitudes = new float[barCount];
        for (int i = 0; i < magnitudes.length; i++) {
            float logIndex = (float) (Math.log(i + 1) / Math.log(magnitudes.length + 1));
            int barIndex = (int) (logIndex * barCount);
            if (barIndex >= barCount) barIndex = barCount - 1;
            if (magnitudes[i] > barMagnitudes[barIndex]) {
                barMagnitudes[barIndex] = magnitudes[i];
            }
        }

        runOnUiThread(() -> {
            if (!mFirstDataReceived) {
                mFirstDataReceived = true;
                showToast("🎵 Hardware FFT data arrived!");
            }
            mVisualizerView.updateMagnitudes(barMagnitudes);
        });
    }

    // ---------- Helper to show toasts only if enabled ----------
    private void showToast(String msg) {
        if (config != null && config.toasts) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
        }
    }

    // ---------- Config loading ----------
    private VisualizerConfig loadConfig() {
        VisualizerConfig config = new VisualizerConfig();

        File configDir = new File(
                Environment.getExternalStorageDirectory(),
                "Android/media/" + getPackageName()
        );
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File configFile = new File(configDir, "config.yaml");
        if (!configFile.exists()) {
            // Write default config with new fields
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(
                        "# Visualizer Configuration\n" +
                        "# Edit and restart to apply.\n" +
                        "barCount: 64\n" +
                        "heightScale: 0.7\n" +
                        "smoothingFactor: 0.6\n" +
                        "updateIntervalMs: 50\n" +
                        "colorArgb: 0xFF00FFFF\n" +
                        "toasts: true\n" +
                        "useBuiltinFile: true\n" +
                        "filePath: \"/sdcard/Music/test.mp3\"\n"
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
            return config;
        }

        // Parse YAML
        try (FileReader reader = new FileReader(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(reader);
            if (data != null) {
                if (data.containsKey("barCount")) config.barCount = (int) data.get("barCount");
                if (data.containsKey("heightScale")) config.heightScale = ((Number) data.get("heightScale")).floatValue();
                if (data.containsKey("smoothingFactor")) config.smoothingFactor = ((Number) data.get("smoothingFactor")).floatValue();
                if (data.containsKey("updateIntervalMs")) config.updateIntervalMs = (int) data.get("updateIntervalMs");
                if (data.containsKey("colorArgb")) config.colorArgb = (int) data.get("colorArgb");
                if (data.containsKey("toasts")) config.toasts = (boolean) data.get("toasts");
                if (data.containsKey("useBuiltinFile")) config.useBuiltinFile = (boolean) data.get("useBuiltinFile");
                if (data.containsKey("filePath")) config.filePath = (String) data.get("filePath");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }

    // ---------- Permission result ----------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            config = loadConfig();
            showToast("✅ Permission granted – starting visualizer");
            setupVisualizer();
        } else {
            config = loadConfig();
            showToast("❌ Permission denied – can't access audio");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVisualizer != null) mVisualizer.release();
        if (mPlayer != null) mPlayer.release();
    }
}