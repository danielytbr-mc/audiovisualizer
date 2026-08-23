package io.github.danielytbr.audiovisualizer;

import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mPlayer;
    private Visualizer mVisualizer;
    private BarVisualizerView mVisualizerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
            return; // The visualizer code will run after the user grants it
        }

        mVisualizerView = findViewById(R.id.visualizer_view);

        // Put an MP3 file in your device's Music folder, or change this path
        String audioPath = Environment.getExternalStorageDirectory().getPath() + "/Music/test.mp3";

        try {
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(audioPath);
            mPlayer.prepare();

            // Attach the visualizer to the player's audio session
            mVisualizer = new Visualizer(mPlayer.getAudioSessionId());
            mVisualizer.setEnabled(true);
            mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]); // max quality

            mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(byte[] waveform, int samplingRate) {
                    // Not used, but must be implemented
                }

                @Override
                public void onFftDataCapture(byte[] fft, int samplingRate) {
                    // Update the bars on the UI thread
                    runOnUiThread(() -> mVisualizerView.updateFft(fft));
                }
            }, Visualizer.getMaxCaptureRate(), false, true); // waveform=false, fft=true

            mPlayer.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Re-run your setup code here
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
