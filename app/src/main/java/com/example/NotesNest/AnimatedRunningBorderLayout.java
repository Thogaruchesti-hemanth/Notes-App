package com.example.NotesNest;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AnimatedRunningBorderLayout extends FrameLayout {

    private Paint borderPaint;
    private RectF rect;
    private Matrix matrix;
    private float rotation = 0f;
    private ValueAnimator animator;
    private boolean isLoading = false;

    private int[] gradientColors;
    private float[] gradientPositions;
    private SweepGradient sweepGradient;

    private final float strokeWidth = 10f;

    public AnimatedRunningBorderLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        rect = new RectF();
        matrix = new Matrix();

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(strokeWidth);
        borderPaint.setStrokeCap(Paint.Cap.ROUND);

        // Pre-calculate colors and positions
        int blue = Color.parseColor("#4285F4");
        int green = Color.parseColor("#34A853");
        int yellow = Color.parseColor("#FBBC05");
        int red = Color.parseColor("#EA4335");

        gradientColors = new int[]{
                blue, green, yellow, red, Color.TRANSPARENT, Color.TRANSPARENT
        };
        gradientPositions = new float[]{0f, 0.1f, 0.2f, 0.3f, 0.35f, 1f};

        animator = ValueAnimator.ofFloat(0, 360);
        animator.setDuration(1500); // 2 seconds for a full loop
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            rotation = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float inset = strokeWidth / 2f;
        rect.set(inset, inset, w - inset, h - inset);

        // Recreate gradient based on new dimensions
        sweepGradient = new SweepGradient(w / 2f, h / 2f, gradientColors, gradientPositions);
        borderPaint.setShader(sweepGradient);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (!isLoading || sweepGradient == null) return;

        // Rotate the matrix around the center
        matrix.setRotate(rotation, getWidth() / 2f, getHeight() / 2f);
        sweepGradient.setLocalMatrix(matrix);

        // Some Android versions require resetting the shader if the matrix changes internally
        borderPaint.setShader(sweepGradient);

        // Draw the rounded rectangle border
        float cornerRadius = 50f;
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint);
    }

    public void startLoading() {
        isLoading = true;
        if (!animator.isRunning()) animator.start();
        invalidate();
    }

    public void stopLoading() {
        isLoading = false;
        animator.cancel();
        invalidate();
    }
}