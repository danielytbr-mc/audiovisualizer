package io.github.danielytbr.audiovisualizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class BarVisualizerView extends View {

    private byte[] mFftData = new byte[0];
    private Paint mPaint = new Paint();

    public BarVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(Color.CYAN);
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void updateFft(byte[] fft) {
        this.mFftData = fft;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFftData.length < 4) return; // Need at least DC + one bin

        int width = getWidth();
        int height = getHeight();

        // Number of frequency bins = fft.length / 2
        int numBins = mFftData.length / 2;
        int barCount = Math.min(numBins - 1, 128); // Skip DC (bin 0)

        float barWidth = (float) width / barCount;

        for (int i = 1; i <= barCount; i++) {
            // Get real and imaginary parts for this frequency bin
            float real = mFftData[2 * i] & 0xFF;
            float imag = mFftData[2 * i + 1] & 0xFF;

            // Compute true magnitude
            float magnitude = (float) Math.sqrt(real * real + imag * imag);

            // Normalize: max possible is ~360 (sqrt(255^2 + 255^2))
            float normalized = Math.min(1f, magnitude / 255f);
            float barHeight = normalized * height;

            // Draw if visible
            if (barHeight > 2) {
                float left = (i - 1) * barWidth;
                float right = left + barWidth - 2;
                canvas.drawRect(left, height - barHeight, right, height, mPaint);
            }
        }
    }
}
