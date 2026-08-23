// if ur wondering, yes, i used sm ai while coding, but im gonna edit it myself later
package io.github.danielytbr.audiovisualizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Handler;
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
        // 1. Load file (using a loud, known MP3)
        mPlayer = MediaPlayer.create(this, R.raw.test);
        if (mPlayer == null) {
            Toast.makeText(this, "❌ File missing!", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "✅ File loaded! Duration: " + mPlayer.getDuration() + "ms", Toast.LENGTH_SHORT).show();

        // Loop it so we get continuous data
        mPlayer.setLooping(true);
        mPlayer.start();

        if (!mPlayer.isPlaying()) {
            Toast.makeText(this, "⚠️ Not playing – check volume", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "🔊 Playing (looping)...", Toast.LENGTH_SHORT).show();

        // 2. Create Visualizer with session ID
        int sessionId = mPlayer.getAudioSessionId();
        Toast.makeText(this, "🎧 Session ID: " + sessionId, Toast.LENGTH_SHORT).show();

        mVisualizer = new Visualizer(sessionId);

        // 3. Set capture size (256 = safe on all devices)
        try {
            mVisualizer.setCaptureSize(256);
            Toast.makeText(this, "🔧 Capture size set to 256", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ setCaptureSize failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Set listener BEFORE enabling (important!)
        int rate = Visualizer.getMaxCaptureRate() / 2; // conservative
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                // Waveform arrived! We'll use this as a fallback.
                if (!mFirstFftReceived) {
                    mFirstFftReceived = true;
                    runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "🌊 Waveform data arrived! Using waveform instead of FFT.", Toast.LENGTH_LONG).show()
                    );
                    // You could adapt your view to use waveform data here,
                    // but for now we'll convert it to a simple bar display.
                    runOnUiThread(() -> mVisualizerView.updateFft(waveform)); // waveform is byte[], works with your view
                }
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
        }, rate, true, true); // waveform = true, fft = true

        // 5. Enable the Visualizer NOW (after listener)
        mVisualizer.setEnabled(true);
        if (mVisualizer.getEnabled()) {
            Toast.makeText(this, "📊 Visualizer ENABLED (listener active)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ Visualizer enable failed", Toast.LENGTH_SHORT).show();
        }

        // 6. Fallback: if no data after 3 seconds, show a diagnostic toast
        new Handler().postDelayed(() -> {
            if (!mFirstFftReceived) {
                runOnUiThread(() ->
                    Toast.makeText(MainActivity.this, "⏰ No data after 3s. Check if audio file has sound or try a different MP3.", Toast.LENGTH_LONG).show()
                );
            }
        }, 3000);

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
