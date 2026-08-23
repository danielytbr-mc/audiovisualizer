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
    protected void onDestroy() {
        super.onDestroy();
        if (mVisualizer != null) mVisualizer.release();
        if (mPlayer != null) mPlayer.release();
    }
}
