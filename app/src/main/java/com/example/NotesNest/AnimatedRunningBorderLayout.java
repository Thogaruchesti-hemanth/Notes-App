package com.example.NotesNest;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class AnimatedRunningBorderLayout extends FrameLayout {

    private Paint borderPaint1, borderPaint2;
    private RectF rect;
    private LinearGradient gradient1, gradient2;
    private Matrix matrix1, matrix2;
    private float shift1 = 0f, shift2 = 0f;
    private ValueAnimator animator;
    private boolean isLoading = false;

    public AnimatedRunningBorderLayout(Context context) {
        super(context);
        init();
    }

    public AnimatedRunningBorderLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AnimatedRunningBorderLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        rect = new RectF();

        // Paint 1
        borderPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint1.setStyle(Paint.Style.STROKE);
        borderPaint1.setStrokeWidth(6f);

        // Paint 2 (opposite direction)
        borderPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint2.setStyle(Paint.Style.STROKE);
        borderPaint2.setStrokeWidth(6f);
        borderPaint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD)); // blend together

        matrix1 = new Matrix();
        matrix2 = new Matrix();

        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(2000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            shift1 = progress;
            shift2 = 1f - progress; // opposite direction
            invalidate();
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isLoading) return;

        // Make gradient hug button tightly (no inner gap)
        float stroke = borderPaint1.getStrokeWidth();
        rect.set(
                stroke / 1.5f,
                stroke / 1.5f,
                getWidth() - stroke / 1.5f,
                getHeight() - stroke / 1.5f
        );

        float w = rect.width();
        float h = rect.height();
        float radius = 66f; // adjust for rounded button corners

        // Premium flowing color palette (you can customize)
        int[] colors = new int[]{
                Color.parseColor("#00BCD4"), // cyan
                Color.parseColor("#3F51B5"), // indigo
                Color.parseColor("#E91E63"), // pink
                Color.parseColor("#00BCD4")  // loop back to start
        };

        float[] positions = new float[]{0f, 0.4f, 0.8f, 1f};

        // Diagonal top-left → bottom-right
        gradient1 = new LinearGradient(
                -w * shift1, -h * shift1,
                w + w * shift1, h + h * shift1,
                colors, positions,
                Shader.TileMode.MIRROR
        );

        // Diagonal top-right → bottom-left
        gradient2 = new LinearGradient(
                w * shift2, -h * shift2,
                -w - w * shift2, h + h * shift2,
                colors, positions,
                Shader.TileMode.MIRROR
        );

        borderPaint1.setShader(gradient1);
        borderPaint2.setShader(gradient2);

        int layer = canvas.saveLayer(null, null);
        canvas.drawRoundRect(rect, radius, radius, borderPaint1);
        canvas.drawRoundRect(rect, radius, radius, borderPaint2);
        canvas.restoreToCount(layer);
    }

    public void startLoading() {
        if (!animator.isRunning()) animator.start();
        isLoading = true;
        invalidate();
    }

    public void stopLoading() {
        isLoading = false;
        animator.cancel();
        invalidate();
    }
}
