package io.taucoin.android.wallet.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ListView;

public class ScrollDisabledListView extends ListView {

    private int mPosition;

    public ScrollDisabledListView(Context context) {
        super(context);
    }

    public ScrollDisabledListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollDisabledListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final int actionMasked = ev.getActionMasked() & MotionEvent.ACTION_MASK;

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            // Record the position of the finger when it is pressed
            mPosition = pointToPosition((int) ev.getX(), (int) ev.getY());
            return super.dispatchTouchEvent(ev);
        }

        if (actionMasked == MotionEvent.ACTION_MOVE) {
            // The key point is to ignore MOVE events
            // ListView onTouch MOVE events cannot be retrieved, so scrolling does not occur
            return true;
        }

        // When the finger is raised
        if (actionMasked == MotionEvent.ACTION_UP
                || actionMasked == MotionEvent.ACTION_CANCEL) {
            // The finger press and lift are both in the same view and handed over to the parent control.
            // This is a click event.
            if (pointToPosition((int) ev.getX(), (int) ev.getY()) == mPosition) {
                super.dispatchTouchEvent(ev);
            } else {
                // If the finger has moved out of the Item when it is pressed,
                // it indicates a rolling behavior and cleans up the Item Pressed state.
                setPressed(false);
                invalidate();
                return true;
            }
        }

        return super.dispatchTouchEvent(ev);
    }
}