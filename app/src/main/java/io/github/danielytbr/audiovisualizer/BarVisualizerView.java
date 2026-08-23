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
    if (mFftData.length < 4) return;

    int width = getWidth();
    int height = getHeight();

    int numBins = mFftData.length / 2;
    int barCount = Math.min(numBins - 1, 128);
    float barWidth = (float) width / barCount;

    // The bottom of the canvas is always 'height'
    float bottomY = height;

    for (int i = 1; i <= barCount; i++) {
        float real = mFftData[2 * i] & 0xFF;
        float imag = mFftData[2 * i + 1] & 0xFF;
        float magnitude = (float) Math.sqrt(real * real + imag * imag);
        float normalized = Math.min(1f, magnitude / 255f);

        // Bar height scales from 0 to height
        float barHeight = normalized * height;

        // Top is height - barHeight, bottom is height
        float topY = height - barHeight;
        float leftX = (i - 1) * barWidth;
        float rightX = leftX + barWidth - 2;

        if (barHeight > 2) {
            canvas.drawRect(leftX, topY / 3, rightX, bottomY, mPaint);
        }
    }
}
}
