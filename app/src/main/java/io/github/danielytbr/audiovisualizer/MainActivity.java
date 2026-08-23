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
            return;
        }

        // 2. Create Visualizer
        mVisualizer = new Visualizer(mPlayer.getAudioSessionId());

        // 3. Force a small, safe capture size (512)
        int captureSize = 512;
        try {
            mVisualizer.setCaptureSize(captureSize);
            Toast.makeText(this, "🔧 Capture size set to " + captureSize, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ setCaptureSize failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Enable the Visualizer
        mVisualizer.setEnabled(true);
        if (mVisualizer.getEnabled()) {
            Toast.makeText(this, "📊 Visualizer ENABLED", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ Visualizer enable failed", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. Set listener with a conservative rate (half the max)
        int rate = Visualizer.getMaxCaptureRate() / 2; // e.g., 10000 Hz
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                // Waveform not used, but we'll show a toast once to confirm it works
                if (!mFirstFftReceived) {
                    mFirstFftReceived = true;
                    runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "🌊 Waveform data arrived! (testing)", Toast.LENGTH_SHORT).show()
                    );
                }
                // Optionally update view with waveform? Not now.
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                if (!mFirstFftReceived) {
                    mFirstFftReceived = true;
                    runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "🎵 FFT data arrived! Length: " + fft.length, Toast.LENGTH_SHORT).show()
                    );
                }
                runOnUiThread(() -> mVisualizerView.updateFft(fft));
            }
        }, rate, true, true); // waveform=true, fft=true – both enabled for testing

        Toast.makeText(this, "👂 Listener attached with rate " + rate, Toast.LENGTH_SHORT).show();

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
