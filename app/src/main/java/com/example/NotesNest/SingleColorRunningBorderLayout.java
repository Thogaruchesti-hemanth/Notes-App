package com.example.NotesNest;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SingleColorRunningBorderLayout extends FrameLayout {

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
    private final int themeColor = Color.parseColor("#FFF3B64D"); // Your app color

    public SingleColorRunningBorderLayout(Context context) {
        super(context);
        init();
    }

    public SingleColorRunningBorderLayout(Context context, @Nullable AttributeSet attrs) {
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
        gradientColors = new int[]{themeColor, Color.TRANSPARENT};
        gradientPositions = new float[]{0f, 1f};

        animator = ValueAnimator.ofFloat(0, 360);
        animator.setDuration(1500); // Full rotation in 1.5 seconds
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

        // Reset the shader to apply matrix changes
        borderPaint.setShader(sweepGradient);

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
