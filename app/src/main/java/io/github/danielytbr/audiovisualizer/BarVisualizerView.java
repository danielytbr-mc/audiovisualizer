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
    private float[] mSmoothedValues = new float[0];
    private final float SMOOTHING_FACTOR = 0.6f; // 0.0 = super smooth, 1.0 = no smoothing

    public BarVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(Color.CYAN);
        mPaint.setStyle(Paint.Style.FILL);
    }

    public void updateFft(byte[] fft) {
        this.mFftData = fft;
        if (mSmoothedValues.length == 0 && fft.length > 0) {
            mSmoothedValues = new float[fft.length / 2];
        }
        invalidate();
    }

    @Override
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mFftData.length < 4) return;

    int width = getWidth();
    int height = getHeight();

    int numBins = mFftData.length / 2;
    int barCount = Math.min(numBins - 1, 128);
    float barWidth = (float) width / barCount;

    // Maximum possible magnitude for byte values (0-255)
    final float MAX_FFT_MAGNITUDE = (float) Math.sqrt(255 * 255 + 255 * 255); // ≈ 360.6
    // Adjust this sensitivity: 1.0 = full range, 0.5 = half height, etc.
    final float HEIGHT_SCALE = 0.6f; // 🎯 Tweak this to your liking

    for (int i = 1; i <= barCount; i++) {
        float real = mFftData[2 * i] & 0xFF;
        float imag = mFftData[2 * i + 1] & 0xFF;

        float magnitude = (float) Math.sqrt(real * real + imag * imag);
        float normalized = Math.min(1f, magnitude / MAX_FFT_MAGNITUDE);

        // Apply smoothing (if you have mSmoothedValues set up)
        if (mSmoothedValues.length > i) {
            mSmoothedValues[i] = mSmoothedValues[i] * SMOOTHING_FACTOR + normalized * (1 - SMOOTHING_FACTOR);
            normalized = mSmoothedValues[i];
        } else {
            mSmoothedValues = new float[barCount + 1];
            mSmoothedValues[i] = normalized;
        }

        // Apply the height scale so bars don't hit the top too aggressively
        float barHeight = normalized * height * HEIGHT_SCALE;

        if (barHeight > 2) {
            float left = (i - 1) * barWidth;
            float right = left + barWidth - 2;
            float top = height - barHeight;
            canvas.drawRect(left, top, right, height, mPaint);
        }
    }
}
}
