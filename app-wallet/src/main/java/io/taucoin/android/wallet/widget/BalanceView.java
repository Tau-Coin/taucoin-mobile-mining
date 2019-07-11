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

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import io.taucoin.android.wallet.R;
import io.taucoin.foundation.util.DimensionsUtil;

public class BalanceView extends View {

    private float mWidth;
    private float mHeight;
    private int mDefaultMinWidth;

    private Paint mCirclePaint = new Paint();
    private int mLeftCircleRadius;
    private int mRightCircleRadius;
    private int mCircleTextSize;

    private Paint mTrianglePaint = new Paint();
    private Path mTrianglePath = new Path();
    private float mTriangleWidth;
    private float mTriangleHeight;
    private float mLineWidth;

    private int mColor;
    private int mFontColor;
    private float mPadding;
    private float mPaddingBottom;

    private float mRotateRange;
    private float mOldDegree = 0;
    private float mCurrentDegree = 0;
    private float mRotateDegree = 0;

    private Paint mTextPaint = new Paint();
    private String mCurrentText;
    private int mCurrentTextSize;
    private int mCurrentTextColor;

    private ValueAnimator mAnimator;

    public BalanceView(Context context) {
        this(context, null);
    }

    public BalanceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BalanceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        resetData(0, 0);
        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCurrentDegree > 0){
                    resetData(1, 0);
                }else if(mCurrentDegree == 0){
                    resetData(0, 1);
                }else{
                    resetData(0, 0);
                }
            }
        });
    }


    private void init(){
        mAnimator = ValueAnimator.ofFloat(-mRotateRange, mRotateRange);

        mDefaultMinWidth = DimensionsUtil.dip2px(getContext(), 150);
        mLeftCircleRadius = DimensionsUtil.dip2px(getContext(), 25);
        mRightCircleRadius = DimensionsUtil.dip2px(getContext(), 15);
        mTriangleWidth = DimensionsUtil.dip2px(getContext(), 20);
        mLineWidth = DimensionsUtil.dip2px(getContext(), 1.5f);
        mPadding = DimensionsUtil.dip2px(getContext(), 5);
        mPaddingBottom = DimensionsUtil.dip2px(getContext(), 20);
        mColor = getResources().getColor(R.color.color_progress_bg);
        mFontColor = getResources().getColor(R.color.color_blue);
        mCircleTextSize = DimensionsUtil.dip2px(getContext(), 16);
        mCurrentTextSize = DimensionsUtil.dip2px(getContext(), 10);
        mCurrentTextColor = getResources().getColor(R.color.color_grey_light);
        mRotateRange = 6;
    }

    private void resetData(int left, int right) {
        String type;
        if(right > left){
            mCurrentDegree = mRotateRange;
            type = "<";
        }else if(right == left){
            mCurrentDegree = 0;
            type = "=";
        }else{
            mCurrentDegree = -mRotateRange;
            type = ">";
        }
        mCurrentText = "Current: H%sT*P*C";
        mCurrentText = String.format(mCurrentText, type);
        if(mAnimator != null && !mAnimator.isRunning()){
            startAnimator(mCurrentDegree);
        }
    }

    private synchronized void startAnimator(float currentDegree) {
        float degreeInterval = currentDegree - mOldDegree;
        if(degreeInterval == 0){
            return;
        }
        degreeInterval = Math.abs(degreeInterval);
        mAnimator = ValueAnimator.ofFloat(0, degreeInterval);
        mAnimator.setDuration(1000);
        mAnimator.addUpdateListener(animation -> {
            if(currentDegree < mOldDegree){
                mRotateDegree = mOldDegree - (float) animation.getAnimatedValue();
            }else{
                mRotateDegree = mOldDegree + (float) animation.getAnimatedValue();
            }
            invalidate();
        });

        mAnimator.addListener(new Animator.AnimatorListener(){
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mOldDegree = currentDegree;
                if(mCurrentDegree != currentDegree){
                    startAnimator(mCurrentDegree);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mAnimator.start();
    }

    private void resetParams() {
        mWidth = getWidth();
        mHeight = getHeight();

        mCirclePaint.reset();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setColor(mColor);

        mTrianglePaint.reset();
        mTrianglePaint.setAntiAlias(true);
        mTrianglePaint.setStrokeWidth(mLineWidth);
        mTrianglePaint.setColor(mColor);

        mTextPaint.reset();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(mCurrentTextColor);
        mTextPaint.setTextSize(mCurrentTextSize);

        mTrianglePath = new Path();
        mTrianglePath.moveTo(mWidth / 2 - mTriangleWidth / 2, mHeight - mPaddingBottom);
        mTrianglePath.lineTo(mWidth / 2 + mTriangleWidth / 2, mHeight - mPaddingBottom);
        mTriangleHeight = (float) (mTriangleWidth / 2 / Math.sin(45));
        mTriangleHeight = Math.abs(mTriangleHeight) + mPaddingBottom;
        mTrianglePath.lineTo(mWidth / 2, mHeight - mTriangleHeight);
        mTrianglePath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        resetParams();
        // draw triangle
        canvas.drawPath(mTrianglePath, mTrianglePaint);

        // draw current text
        int textWidth = getTextWidth(mTextPaint, mCurrentText);
        float x = mWidth / 2 - textWidth / 2;
        float y = mHeight - mPaddingBottom + mCurrentTextSize * 1.2f;
        canvas.drawText(mCurrentText, x, y, mTextPaint);

        canvas.save();
        canvas.rotate(mRotateDegree, mWidth / 2, mHeight - mTriangleHeight);
        // draw left circle
        float circleLeftY = mHeight - mLeftCircleRadius - mTriangleHeight - mLineWidth * 2;
        float circleLeftX = mLeftCircleRadius + mPadding;
        canvas.drawCircle(circleLeftX, circleLeftY, mLeftCircleRadius, mCirclePaint);

        // draw right circle
        float circleRightY1 = mHeight - mRightCircleRadius - mTriangleHeight - mLineWidth * 2;
        float circleRightX1 = mWidth - mRightCircleRadius - mPadding;
        canvas.drawCircle(circleRightX1, circleRightY1, mRightCircleRadius, mCirclePaint);

        float circleRightX2 = circleRightX1 - mRightCircleRadius * 2;
        canvas.drawCircle(circleRightX2, circleRightY1, mRightCircleRadius, mCirclePaint);

        float circleRightY3 = (float)(mRightCircleRadius * 2 * Math.sin(60));
        circleRightY3 = Math.abs(circleRightY3) + mRightCircleRadius;
        circleRightY3 = circleRightY1 - circleRightY3 - mLineWidth;
        float circleRightX3 = mWidth - mRightCircleRadius * 2 - mPadding;
        canvas.drawCircle(circleRightX3, circleRightY3, mRightCircleRadius, mCirclePaint);

        // draw line
        float lineY = mHeight - mTriangleHeight - mLineWidth / 2;
        canvas.drawLine(mPadding, lineY, mWidth - mPadding, lineY, mTrianglePaint);
        canvas.restore();

        // draw text
        float fontRotateDegree = -mRotateDegree;
        mTrianglePaint.setColor(mFontColor);
        mTrianglePaint.setTextSize(mCircleTextSize);
        int textHeight = mCircleTextSize;

        canvas.save();
        canvas.rotate(mRotateDegree, mWidth / 2, mHeight - mTriangleHeight);
        canvas.save();
        canvas.rotate(fontRotateDegree, circleLeftX, circleLeftY);
        String text = "H";
        textWidth = getTextWidth(mTrianglePaint, text);
        canvas.drawText(text, circleLeftX - textWidth / 2, circleLeftY + textHeight / 2, mTrianglePaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(fontRotateDegree, circleRightX1, circleRightY1);
        text = "C";
        textWidth = getTextWidth(mTrianglePaint, text);
        canvas.drawText(text, circleRightX1 - textWidth / 2, circleRightY1 + textHeight / 2, mTrianglePaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(fontRotateDegree, circleRightX2, circleRightY1);
        text = "P";
        textWidth = getTextWidth(mTrianglePaint, text);
        canvas.drawText(text, circleRightX2 - textWidth / 2, circleRightY1 + textHeight / 2, mTrianglePaint);
        canvas.restore();

        canvas.save();
        canvas.rotate(fontRotateDegree, circleRightX3, circleRightY3);
        text = "T";
        textWidth = getTextWidth(mTrianglePaint, text);
        canvas.drawText(text, circleRightX3 - textWidth / 2, circleRightY3 + textHeight / 2, mTrianglePaint);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec));
    }

    private int measure(int origin) {
        int result = mDefaultMinWidth;
        int specMode = MeasureSpec.getMode(origin);
        int specSize = MeasureSpec.getSize(origin);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    public int getTextWidth(Paint paint, String str) {
        int iRet = 0;
        if (str != null && str.length() > 0) {
            int len = str.length();
            float[] widths = new float[len];
            paint.getTextWidths(str, widths);
            for (int j = 0; j < len; j++) {
                iRet += (int) Math.ceil(widths[j]);
            }
        }
        return iRet;
    }

    @Override
    protected void onDetachedFromWindow() {
        if(mAnimator != null && mAnimator.isRunning()){
            mAnimator.cancel();
            mAnimator = null;
        }
        super.onDetachedFromWindow();
    }
}