//main
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
    private boolean mFirstFftReceived = false;

    
    private VisualizerConfig config;

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
      // Check READ_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }
        setupVisualizer();
    }

    private void setupVisualizer() {
        // 1. Load configuration (assign to the field)
        config = loadConfig();

        // 2. Apply config to the view
        // Apply to the view
        mVisualizerView.setBarCount(config.barCount);
        mVisualizerView.setHeightScale(config.heightScale);
        mVisualizerView.setSmoothingFactor(config.smoothingFactor);
        mVisualizerView.setColor(config.colorArgb);
        mVisualizerView.setBackgroundColor(config.backgroundColor);

        try {
            // 3. Load audio file
            //String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
            //    + "/Android/media/" + getPackageName() + "/song.mp3";

        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(config.filePath);
            mPlayer.prepare();
            mPlayer.setOnPreparedListener(MediaPlayer::start);
        } catch (IOException e) {
            e.printStackTrace();
        }
            if (mPlayer == null) {
                Toast.makeText(this, "Error: Music file missing: " + config.filePath, Toast.LENGTH_LONG).show();
                return;
            }
            if (config.toasts) Toast.makeText(this, "✅ File loaded! Duration: " + mPlayer.getDuration() + "ms", Toast.LENGTH_SHORT).show();

            mPlayer.setLooping(true);
            mPlayer.start();

            if (!mPlayer.isPlaying()) {
                if (config.toasts) Toast.makeText(this, "⚠️ Not playing – check volume", Toast.LENGTH_SHORT).show();
                return;
            }
            if (config.toasts) Toast.makeText(this, "🔊 Playing (looping)...", Toast.LENGTH_SHORT).show();

            // 4. Create Visualizer
            int sessionId = mPlayer.getAudioSessionId();
            if (config.toasts) Toast.makeText(this, "🎧 Session ID: " + sessionId, Toast.LENGTH_SHORT).show();

            mVisualizer = new Visualizer(sessionId);
            mVisualizer.setCaptureSize(256);

            // 5. Set listener with rate from config (convert ms to Hz)
            int listenerRate = 1000 / config.updateIntervalMs; // e.g., 50ms → 20 Hz
            mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    if (!mFirstFftReceived) {
                        mFirstFftReceived = true;
                        
                        runOnUiThread(() -> {
                            if (config.toasts) Toast.makeText(MainActivity.this, "🌊 Waveform data arrived!", Toast.LENGTH_SHORT).show();
                        });
                    }
                    // Optionally use waveform as fallback
                    runOnUiThread(() -> mVisualizerView.updateFft(waveform));
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    if (!mFirstFftReceived) {
                        mFirstFftReceived = true;
                        
                        runOnUiThread(() -> {
                            if (config.toasts) Toast.makeText(MainActivity.this, "🎵 FFT data arrived! Length: " + fft.length, Toast.LENGTH_SHORT).show();
                        });
                    }
                    runOnUiThread(() -> mVisualizerView.updateFft(fft));
                }
            }, listenerRate, true, true);

            // 6. Enable Visualizer
            mVisualizer.setEnabled(true);
            if (mVisualizer.getEnabled()) {
                if (config.toasts) Toast.makeText(this, "📊 Visualizer ENABLED", Toast.LENGTH_SHORT).show();
            } else {
                if (config.toasts) Toast.makeText(this, "❌ Visualizer enable failed", Toast.LENGTH_SHORT).show();
            }

            // 7. Fallback timeout (no data after 3s)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!mFirstFftReceived) {
                    
                    runOnUiThread(() -> {
                        if (config.toasts) Toast.makeText(MainActivity.this, "⏰ No data after 3s. Check audio file.", Toast.LENGTH_LONG).show();
                    });
                }
            }, 3000);

        } catch (Exception e) {
            if (config.toasts) Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
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
            // Write default config file
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(
                        "# Visualizer Configuration\n" +
                                "# Edit these values and restart the app to apply changes.\n" +
                                "barCount: 48\n" +
                                "heightScale: 0.4\n" +
                                "smoothingFactor: 0.7\n" +
                                "updateIntervalMs: 1024\n" +
                                "colorArgb: 0xFFFFFFFF\n" +
                                "backgroundColor: 0xFF222222\n" +
                                "useBuiltinFile: false\n" +
                                "toasts: false\n" +
                                "filePath: \"/sdcard/Music/test.mp3\""
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
                if (data.containsKey("backgroundColor")) config.backgroundColor = (int) data.get("backgroundColor");
                if (data.containsKey("useBuiltinFile")) config.useBuiltinFile = (boolean) data.get("useBuiltinFile");
                if (data.containsKey("toasts")) config.toasts = (String) data.get("toasts");
                if (data.containsKey("filePath")) config.filePath = (String) data.get("filePath");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // fallback to defaults
        }
        return config;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            
            config = loadConfig(); // ensure config is available
            if (config.toasts) Toast.makeText(this, "✅ Permission granted – starting visualizer", Toast.LENGTH_SHORT).show();
            setupVisualizer();
        } else {
            
            config = loadConfig(); // safe even if file missing
            if (config.toasts) Toast.makeText(this, "❌ Permission denied – can't access audio", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVisualizer != null) mVisualizer.release();
        if (mPlayer != null) mPlayer.release();
    }
}
