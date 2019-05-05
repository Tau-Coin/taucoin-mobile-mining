package io.taucoin.android.wallet.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;

/**
 * Progress View
 */
public class ProgressView extends RelativeLayout {

    private ViewHolder mViewHolder;

    public ProgressView(Context context) {
        this(context, null);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.progress_view, this, true);
        mViewHolder = new ViewHolder(view);
        setOff();
    }

    public void closeLoading() {
        mViewHolder.progress.closeLoading();
    }

    class ViewHolder{
        @BindView(R.id.progress_circular)
        CircleProgress progress;
        @BindView(R.id.progress_image)
        ImageView image;
        @BindView(R.id.progress_connecting)
        ImageView connecting;
        ViewHolder(View view){
            ButterKnife.bind(this, view);
        }
    }

    public void setOff() {
        closeConnecting();
        mViewHolder.progress.setOff();
    }

    public void setOn() {
        closeConnecting();
        mViewHolder.progress.setOn();
    }

    public void setConnecting() {
        mViewHolder.image.setVisibility(GONE);
        mViewHolder.connecting.setVisibility(VISIBLE);
        mViewHolder.progress.setConnecting();
    }

    private void closeConnecting() {
        mViewHolder.image.setVisibility(VISIBLE);
        mViewHolder.connecting.setVisibility(GONE);
    }

    public void setError() {
        closeConnecting();
        mViewHolder.progress.setError();
    }
}
