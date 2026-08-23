package io.github.danielytbr.audiovisualizer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
        invalidate(); // this triggers onDraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mFftData.length == 0) return;

        int width = getWidth();
        int height = getHeight();

        // FFT data: first half is real, second half is imaginary.
        // We just use the magnitudes from the first half.
        int barCount = Math.min(mFftData.length / 2, 128); // limit to 128 bars
        float barWidth = (float) width / barCount;

        for (int i = 0; i < barCount; i++) {
            // Convert byte to magnitude (0 to 255)
            int magnitude = mFftData[i] & 0xFF;
            float barHeight = (float) magnitude / 255 * height;

            // Draw each bar (skip tiny ones for a cleaner look)
            if (barHeight > 5) {
                canvas.drawRect(
                        i * barWidth,
                        height - barHeight,
                        i * barWidth + barWidth - 2,
                        height,
                        mPaint
                );
            }
        }
    }
}
