// if ur wondering, yes, i used sm ai while coding, but im gonna edit it myself later
package io.github.danielytbr.audiovisualizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mPlayer;
    private Visualizer mVisualizer;
    private BarVisualizerView mVisualizerView;
    private boolean mFirstFftReceived = false; // to avoid spam

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVisualizerView = findViewById(R.id.visualizer_view);

        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            // Return early – setup will be called after user responds
            return;
        }
        setupVisualizer();
    }

private void setupVisualizer() {
    try {
        // 1. Load file
        mPlayer = MediaPlayer.create(this, R.raw.test);
        if (mPlayer == null) {
            Toast.makeText(this, "❌ File missing!", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "✅ File loaded! Duration: " + mPlayer.getDuration() + "ms", Toast.LENGTH_SHORT).show();

        mPlayer.start();
        if (mPlayer.isPlaying()) {
            Toast.makeText(this, "🔊 Playing...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "⚠️ Not playing", Toast.LENGTH_SHORT).show();
        }

        // 2. Create Visualizer
        mVisualizer = new Visualizer(mPlayer.getAudioSessionId());

        // 3. Set capture size BEFORE enabling
        int[] range = Visualizer.getCaptureSizeRange();
        int captureSize = Math.min(range[1], 1024); // safe max
        mVisualizer.setCaptureSize(captureSize);

        // 4. Now enable it
        mVisualizer.setEnabled(true);
        if (mVisualizer.getEnabled()) {
            Toast.makeText(this, "📊 Visualizer ready", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ Visualizer enable failed", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. Set listener
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {}

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                if (!mFirstFftReceived) {
                    mFirstFftReceived = true;
                    runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "🎵 FFT data arriving!", Toast.LENGTH_SHORT).show()
                    );
                }
                runOnUiThread(() -> mVisualizerView.updateFft(fft));
            }
        }, Visualizer.getMaxCaptureRate(), false, true);

    } catch (Exception e) {
        Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        e.printStackTrace();
    }
}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "✅ Permission granted – starting visualizer", Toast.LENGTH_SHORT).show();
            setupVisualizer();
        } else {
            Toast.makeText(this, "❌ Permission denied – can't access audio", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVisualizer != null) mVisualizer.release();
        if (mPlayer != null) mPlayer.release();
    }
}
