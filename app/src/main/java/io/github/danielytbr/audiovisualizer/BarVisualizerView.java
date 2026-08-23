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

        for (int i = 1; i <= barCount; i++) {
            // Extract real and imaginary parts
            float real = mFftData[2 * i] & 0xFF;
            float imag = mFftData[2 * i + 1] & 0xFF;

            // Raw magnitude
            float magnitude = (float) Math.sqrt(real * real + imag * imag);
            float normalized = Math.min(1f, magnitude / 255f);

            // Apply smoothing
            if (mSmoothedValues.length > i) {
                mSmoothedValues[i] = mSmoothedValues[i] * SMOOTHING_FACTOR + normalized * (1 - SMOOTHING_FACTOR);
            } else {
                mSmoothedValues = new float[barCount + 1];
                mSmoothedValues[i] = normalized;
            }

            float barHeight = mSmoothedValues[i] * height;

            // Draw the bar
            if (barHeight > 2) {
                float left = (i - 1) * barWidth;
                float right = left + barWidth - 2;
                float top = height - barHeight;
                canvas.drawRect(left, top, right, height, mPaint);
            }
        }
    }
}
