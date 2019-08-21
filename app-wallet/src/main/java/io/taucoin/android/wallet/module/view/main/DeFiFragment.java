package io.taucoin.android.wallet.module.view.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.OnClick;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseFragment;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.foundation.util.DrawablesUtil;

public class DeFiFragment extends BaseFragment {

    @BindView(R.id.tv_telegram_group)
    TextView tvTelegramGroup;

    @Override
    public View getViewLayout(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_de_fi, container, false);
        butterKnifeBinder(this, view);
        initView();
        return view;
    }

    private void initView() {
        DrawablesUtil.setUnderLine(tvTelegramGroup);
    }

    @OnClick({R.id.tv_telegram_group})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_telegram_group:
                ActivityUtil.openUri(getActivity(), TransmitKey.ExternalUrl.DE_FI_GROUP);
                break;
            default:
                break;
        }
    }
}