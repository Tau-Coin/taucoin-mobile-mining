package io.taucoin.android.wallet.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.taucoin.android.wallet.R;
import io.taucoin.foundation.util.StringUtil;

/**
 * Progress View
 */
public class ProgressView extends RelativeLayout {

    private ViewHolder mViewHolder;
    private int centerImage;

    public ProgressView(Context context) {
        this(context, null);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    private void initView(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ProgressView);
        String centerText = a.getString(R.styleable.ProgressView_center_text);
        centerImage = a.getResourceId(R.styleable.ProgressView_center_image, -1);
        a.recycle();

        View view = LayoutInflater.from(getContext()).inflate(R.layout.progress_view, this, true);
        mViewHolder = new ViewHolder(view);
        mViewHolder.image.setVisibility(GONE);
        if(centerImage != -1){
            mViewHolder.image.setVisibility(VISIBLE);
            mViewHolder.image.setImageResource(centerImage);
        }
        mViewHolder.text.setVisibility(GONE);
        if(StringUtil.isNotEmpty(centerText)){
            mViewHolder.text.setVisibility(VISIBLE);
            mViewHolder.text.setText(centerText);
        }
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
        @BindView(R.id.center_text)
        TextView text;
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
        if(centerImage != -1){
            mViewHolder.image.setVisibility(VISIBLE);
        }
        mViewHolder.connecting.setVisibility(GONE);
    }

    public void setError() {
        closeConnecting();
        mViewHolder.progress.setError();
    }
}
