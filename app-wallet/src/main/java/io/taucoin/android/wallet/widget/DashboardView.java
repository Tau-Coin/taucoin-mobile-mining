/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.util.Constant;
import io.taucoin.android.wallet.util.MiscUtil;
import io.taucoin.foundation.util.DimensionsUtil;

public class DashboardView extends View {

    private Context mContext;

    private int mDefaultSize;
    private boolean antiAlias;

    private double mValue;
    private double mMaxValue;

    private Paint mArcPaint;
    private float mArcWidth;
    private float mStartAngle, mSweepAngle;
    private RectF mRectF;
    private int[] mGradientColors = {Color.parseColor("#f19322"), Color.parseColor("#f19322")};
    private float mPercent;
    private long mAnimTime;
    private ValueAnimator mAnimator;

    private Paint mBgArcPaint;
    private int mBgArcColor;
    private float mBgArcWidth;

    private Paint mPointArcPaint;
    private float mPointRadius;
    private float mPointWidth;
    private int mPointArcColor;

    private Point mCenterPoint;
    private DashboardLayout.ValueUpdateListener mValueUpdateListener;

    public DashboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        mDefaultSize = DimensionsUtil.dip2px(mContext, Constant.DEFAULT_SIZE);
        mAnimator = new ValueAnimator();
        mRectF = new RectF();
        mCenterPoint = new Point();
        initAttrs(attrs);
        initPaint();
        setValue(mValue);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.DashboardView);

        antiAlias = typedArray.getBoolean(R.styleable.DashboardView_antiAlias, Constant.ANTI_ALIAS);

        mValue = typedArray.getFloat(R.styleable.DashboardView_value, Constant.DEFAULT_VALUE);
        mMaxValue = typedArray.getFloat(R.styleable.DashboardView_maxValue, Constant.DEFAULT_MAX_VALUE);

        mArcWidth = typedArray.getDimension(R.styleable.DashboardView_arcWidth, Constant.DEFAULT_ARC_WIDTH);
        mStartAngle = typedArray.getFloat(R.styleable.DashboardView_startAngle, Constant.DEFAULT_START_ANGLE);
        mSweepAngle = typedArray.getFloat(R.styleable.DashboardView_sweepAngle, Constant.DEFAULT_SWEEP_ANGLE);

        mPointWidth = typedArray.getDimension(R.styleable.DashboardView_pointWidth, Constant.DEFAULT_POINT_WIDTH);
        mPointRadius = typedArray.getDimension(R.styleable.DashboardView_pointRadius, Constant.DEFAULT_POINT_RADIUS);
        mPointArcColor = typedArray.getColor(R.styleable.DashboardView_pointArcColor,  Color.RED);

        mBgArcColor = typedArray.getColor(R.styleable.DashboardView_bgArcColor, Color.WHITE);
        mBgArcWidth = typedArray.getDimension(R.styleable.DashboardView_bgArcWidth, Constant.DEFAULT_ARC_WIDTH);

        //mPercent = typedArray.getFloat(R.styleable.DashboardView_percent, 0);
        mAnimTime = typedArray.getInt(R.styleable.DashboardView_animTime, Constant.DEFAULT_ANIM_TIME);

        int gradientArcColors = typedArray.getResourceId(R.styleable.DashboardView_arcColors, 0);
        if (gradientArcColors != 0) {
            try {
                int[] gradientColors = getResources().getIntArray(gradientArcColors);
                if (gradientColors.length == 0) {
                    int color = getResources().getColor(gradientArcColors);
                    mGradientColors = new int[2];
                    mGradientColors[0] = color;
                    mGradientColors[1] = color;
                } else if (gradientColors.length == 1) {
                    mGradientColors = new int[2];
                    mGradientColors[0] = gradientColors[0];
                    mGradientColors[1] = gradientColors[0];
                } else {
                    mGradientColors = gradientColors;
                }
            } catch (Resources.NotFoundException e) {
                throw new Resources.NotFoundException("the give resource not found.");
            }
        }

        typedArray.recycle();
    }

    private void initPaint() {

        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(antiAlias);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mArcWidth);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);

        mBgArcPaint = new Paint();
        mBgArcPaint.setAntiAlias(antiAlias);
        mBgArcPaint.setColor(mBgArcColor);
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mBgArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        mPointArcPaint = new Paint();
        mPointArcPaint.setAntiAlias(antiAlias);
        mPointArcPaint.setColor(mBgArcColor);
        mPointArcPaint.setStyle(Paint.Style.STROKE);
        mPointArcPaint.setStrokeWidth(mPointWidth);
        mPointArcPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MiscUtil.measure(widthMeasureSpec, mDefaultSize),
                MiscUtil.measure(heightMeasureSpec, mDefaultSize));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        // Finding the Maximum Width of Arc and Background Arc
        float maxArcWidth = Math.max(mArcWidth, mBgArcWidth);
        //Find the minimum as the actual value
        int minSize = Math.min(w - getPaddingLeft() - getPaddingRight() - 2 * (int) maxArcWidth,
                h - getPaddingTop() - getPaddingBottom() - 2 * (int) maxArcWidth);
        // Subtract the width of the arc, otherwise some of the arcs will be drawn on the periphery.
        float mRadius = minSize / 2 - mPointRadius;
        // Obtaining the Relevant Parameters of a Circle
        mCenterPoint.x = w / 2;
        mCenterPoint.y = h / 2;

        // Drawing the boundary of an arc
        mRectF.left = mCenterPoint.x - mRadius - maxArcWidth / 2;
        mRectF.top = mCenterPoint.y - mRadius - maxArcWidth / 2;
        mRectF.right = mCenterPoint.x + mRadius + maxArcWidth / 2;
        mRectF.bottom = mCenterPoint.y + mRadius + maxArcWidth / 2;
        updateArcPaint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArc(canvas);
    }

    /**
     * Drawing Background Arc
    * **/
    private void drawArc(Canvas canvas) {
        canvas.save();
        float currentAngle = mSweepAngle * mPercent;
        canvas.rotate(mStartAngle, mCenterPoint.x, mCenterPoint.y);
        canvas.drawArc(mRectF, currentAngle, mSweepAngle - currentAngle - 2.5f, false, mBgArcPaint);
        canvas.drawArc(mRectF, 2.5f, currentAngle, false, mArcPaint);

        canvas.save();
        canvas.rotate(currentAngle + 2.5f, mCenterPoint.x, mCenterPoint.y);
        float pointX = mCenterPoint.x * 2 - mBgArcWidth / 2 - mPointRadius;
        float pointY = mCenterPoint.y;

        mPointArcPaint.setColor(mPointArcColor);
        canvas.drawCircle(pointX, pointY, mPointRadius, mPointArcPaint);

        mPointArcPaint.setStyle(Paint.Style.FILL);
        mPointArcPaint.setColor(mBgArcColor);
        mPointArcPaint.setStrokeWidth(0);
        canvas.drawCircle(pointX, pointY, mPointRadius - mPointWidth, mPointArcPaint);
        canvas.restore();
    }

    private void updateArcPaint() {
        SweepGradient mSweepGradient = new SweepGradient(mCenterPoint.x, mCenterPoint.y, mGradientColors, null);
        mArcPaint.setShader(mSweepGradient);
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public double getValue() {
        return mValue;
    }

    public void setValue(double value) {
        if (value > mMaxValue) {
            value = mMaxValue;
        }
        float start = mPercent;
        float end = (float) (value / mMaxValue);
        startAnimator(start, end, mAnimTime);
    }

    private void startAnimator(float start, float end, long animTime) {
        mAnimator = ValueAnimator.ofFloat(start, end);
        mAnimator.setDuration(animTime);
        mAnimator.addUpdateListener(animation -> {
            mPercent = (float) animation.getAnimatedValue();
            mValue = mPercent * mMaxValue;
            if(mValueUpdateListener != null){
                mValueUpdateListener.update(mValue, mMaxValue);
            }
            invalidate();
        });
        mAnimator.start();
    }

    public double getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(double maxValue) {
        mMaxValue = maxValue;
    }

    public long getAnimTime() {
        return mAnimTime;
    }

    public void setAnimTime(long animTime) {
        mAnimTime = animTime;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mAnimator != null && mAnimator.isRunning()){
            mAnimator.cancel();
            mAnimator = null;
        }
    }

    public void setValueUpdateListener(DashboardLayout.ValueUpdateListener valueUpdateListener) {
        mValueUpdateListener = valueUpdateListener;
    }
}