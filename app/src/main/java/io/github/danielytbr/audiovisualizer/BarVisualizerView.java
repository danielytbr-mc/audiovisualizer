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

    // Configurable parameters
    private int mBarCount = 128;
    private float mHeightScale = 0.6f;
    private float mSmoothingFactor = 0.6f;
    private int mColor = Color.CYAN;

    private float[] mSmoothedValues = new float[0];

    public BarVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.FILL);
    }

    // Setters for configuration
    public void setBarCount(int count) {
        mBarCount = count;
    }

    public void setHeightScale(float scale) {
        mHeightScale = scale;
    }

    public void setSmoothingFactor(float factor) {
        mSmoothingFactor = factor;
    }

    public void setColor(int color) {
        mColor = color;
        mPaint.setColor(color);
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
        int barCount = Math.min(numBins - 1, mBarCount);
        float barWidth = (float) width / barCount;

        // Maximum possible magnitude for byte values (0-255)
        final float MAX_FFT_MAGNITUDE = (float) Math.sqrt(255 * 255 + 255 * 255);

        for (int i = 1; i <= barCount; i++) {
            float real = mFftData[2 * i] & 0xFF;
            float imag = mFftData[2 * i + 1] & 0xFF;

            float magnitude = (float) Math.sqrt(real * real + imag * imag);
            float normalized = Math.min(1f, magnitude / MAX_FFT_MAGNITUDE);

            // Apply smoothing
            if (mSmoothedValues.length > i) {
                mSmoothedValues[i] = mSmoothedValues[i] * mSmoothingFactor + normalized * (1 - mSmoothingFactor);
                normalized = mSmoothedValues[i];
            } else {
                mSmoothedValues = new float[barCount + 1];
                mSmoothedValues[i] = normalized;
            }

            // Apply height scale
            float barHeight = normalized * height * mHeightScale;

            if (barHeight > 2) {
                float left = (i - 1) * barWidth;
                float right = left + barWidth - 2;
                float top = height - barHeight;
                canvas.drawRect(left, top, right, height, mPaint);
            }
        }
    }
}
