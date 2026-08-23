// if ur wondering, yes, i used sm ai while coding, but im gonna edit it myself later
package io.github.danielytbr.audiovisualizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.res.AssetFileDescriptor;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mPlayer;
    private Visualizer mVisualizer;
    private BarVisualizerView mVisualizerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVisualizerView = findViewById(R.id.visualizer_view);

      byte[] testFft = new byte[256];
      for (int i = 0; i < testFft.length; i++) {
          testFft[i] = (byte) (Math.sin(i * 0.3) * (Math.random() * 100) + 128);
      }
      mVisualizerView.updateFft(testFft);

        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            setupVisualizer();
        }
    }

private void setupVisualizer() {
    try {
        // 1. Load the file using the shortcut (returns a PREPARED player)
        mPlayer = MediaPlayer.create(this, R.raw.test);

        if (mPlayer == null) {
            Log.e("Visualizer", "MediaPlayer.create() returned NULL! File not found or corrupted.");
            return;
        }

        Log.d("Visualizer", "MediaPlayer created successfully. Duration: " + mPlayer.getDuration() + "ms");

        // 2. Check if it's actually playing (start it)
        mPlayer.start();

        if (mPlayer.isPlaying()) {
            Log.d("Visualizer", "MediaPlayer is PLAYING. You should hear audio.");
        } else {
            Log.d("Visualizer", "MediaPlayer is NOT playing. Check volume or file codec.");
        }

        // 3. Attach Visualizer
        mVisualizer = new Visualizer(mPlayer.getAudioSessionId());
        mVisualizer.setEnabled(true);

        // Check if Visualizer actually enabled
        if (mVisualizer.getEnabled()) {
            Log.d("Visualizer", "Visualizer ENABLED successfully.");
        } else {
            Log.d("Visualizer", "Visualizer FAILED to enable.");
        }

        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {}

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                // This log will prove if data is arriving!
                Log.d("Visualizer", "✅ FFT Data Arriving! Length: " + fft.length);
                runOnUiThread(() -> mVisualizerView.updateFft(fft));
            }
        }, Visualizer.getMaxCaptureRate(), false, true);

    } catch (Exception e) {
        Log.e("Visualizer", "Setup crashed: " + e.getMessage());
        e.printStackTrace();
    }
}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupVisualizer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVisualizer != null) mVisualizer.release();
        if (mPlayer != null) mPlayer.release();
    }
}
