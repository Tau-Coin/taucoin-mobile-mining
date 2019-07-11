package io.taucoin.android.wallet.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class JudgeNestedScrollView extends NestedScrollView {

    private boolean isNeedScroll;

    public JudgeNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public JudgeNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public JudgeNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_MOVE:
                return isNeedScroll;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * Change the method to handle whether NestedScrollView intercepts sliding events
     */
    public void setNeedScroll(boolean isNeedScroll) {
        this.isNeedScroll = isNeedScroll;
    }
}
