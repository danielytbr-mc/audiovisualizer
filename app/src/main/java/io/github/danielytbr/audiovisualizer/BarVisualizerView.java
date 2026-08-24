package io.github.danielytbr.audiovisualizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class BarVisualizerView extends View {

    private Paint mPaint = new Paint();
    private float[] mMagnitudes = null;
    private float[] mSmoothedValues = new float[0];

    // Configurable parameters
    private int mBarCount = 128;
    private float mHeightScale = 0.6f;
    private float mSmoothingFactor = 0.6f;
    private int mColor = Color.CYAN;

    public BarVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.FILL);
    }

    // Setters
    public void setBarCount(int count) { mBarCount = count; }
    public void setHeightScale(float scale) { mHeightScale = scale; }
    public void setSmoothingFactor(float factor) { mSmoothingFactor = factor; }
    public void setColor(int color) { mColor = color; mPaint.setColor(color); }

    // Update using FFT magnitudes (floats 0..1)
    public void updateMagnitudes(float[] magnitudes) {
        this.mMagnitudes = magnitudes;
        if (mSmoothedValues.length != magnitudes.length) {
            mSmoothedValues = new float[magnitudes.length];
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mMagnitudes == null || mMagnitudes.length == 0) return;

        int width = getWidth();
        int height = getHeight();

        int barCount = Math.min(mMagnitudes.length, mBarCount);
        float barWidth = (float) width / barCount;

        for (int i = 0; i < barCount; i++) {
            float raw = mMagnitudes[i];
            // Clamp to avoid out-of-range
            if (raw < 0) raw = 0;
            if (raw > 1) raw = 1;

            // Apply smoothing
            if (mSmoothedValues.length > i) {
                mSmoothedValues[i] = mSmoothedValues[i] * mSmoothingFactor + raw * (1 - mSmoothingFactor);
            } else {
                mSmoothedValues = new float[barCount];
                mSmoothedValues[i] = raw;
            }

            float barHeight = mSmoothedValues[i] * height * mHeightScale;

            if (barHeight > 2) {
                float left = i * barWidth;
                float right = left + barWidth - 2;
                float top = height - barHeight;
                canvas.drawRect(left, top, right, height, mPaint);
            }
        }
    }
}