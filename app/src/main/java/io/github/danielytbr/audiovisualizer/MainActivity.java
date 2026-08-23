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
            // 1. Load the audio file from res/raw/test
            //    (rename your file to test.mp3, test.m4a, etc. – resource name is "test")
            mPlayer = MediaPlayer.create(this, R.raw.test);

            if (mPlayer == null) {
                Toast.makeText(this, "❌ File not found or corrupt! Check res/raw/test", Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this, "✅ File loaded! Duration: " + mPlayer.getDuration() + "ms", Toast.LENGTH_SHORT).show();

            // 2. Start playing
            mPlayer.start();

            if (mPlayer.isPlaying()) {
                Toast.makeText(this, "🔊 Playing... you should hear audio", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ Not playing – check file codec or volume", Toast.LENGTH_SHORT).show();
            }

            // 3. Attach Visualizer
            mVisualizer = new Visualizer(mPlayer.getAudioSessionId());
            mVisualizer.setEnabled(true);

            if (mVisualizer.getEnabled()) {
                Toast.makeText(this, "📊 Visualizer ENABLED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "❌ Visualizer FAILED – permission denied?", Toast.LENGTH_SHORT).show();
                return;
            }

            // Set capture size
            int captureSize = Visualizer.getCaptureSizeRange()[1];
            mVisualizer.setCaptureSize(captureSize);

            // 4. Set up the listener
            mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                    // Not used
                }

                @Override
                public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                    // Show toast only once when first data arrives
                    if (!mFirstFftReceived) {
                        mFirstFftReceived = true;
                        runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "🎵 FFT Data arriving! Length: " + fft.length, Toast.LENGTH_SHORT).show()
                        );
                    }
                    // Update the view
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