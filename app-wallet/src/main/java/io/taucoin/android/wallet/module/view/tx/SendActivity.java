package io.taucoin.android.wallet.module.view.tx;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.OnTextChanged;
import io.taucoin.android.wallet.R;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.OnTouch;
import io.reactivex.ObservableOnSubscribe;
import io.taucoin.android.wallet.core.Wallet;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.base.TransmitKey;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.module.bean.MessageEvent;
import io.taucoin.android.wallet.module.service.TxService;
import io.taucoin.android.wallet.module.presenter.TxPresenter;
import io.taucoin.android.wallet.module.view.main.iview.ISendView;
import io.taucoin.android.wallet.module.view.manage.SettingActivity;
import io.taucoin.android.wallet.util.FixMemLeak;
import io.taucoin.android.wallet.util.FmtMicrometer;
import io.taucoin.android.wallet.util.KeyboardUtils;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.android.wallet.util.MoneyValueFilter;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.util.UserUtil;
import io.taucoin.android.wallet.widget.CommonDialog;
import io.taucoin.android.wallet.widget.EditInput;
import io.taucoin.android.wallet.widget.SelectionEditText;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class SendActivity extends BaseActivity implements ISendView {

    @BindView(R.id.ll_root)
    LinearLayout llRoot;
    @BindView(R.id.et_address)
    EditText etAddress;
    @BindView(R.id.et_amount)
    EditText etAmount;
    @BindView(R.id.et_memo)
    EditText etMemo;
    @BindView(R.id.iv_fee)
    ImageView ivFee;
    @BindView(R.id.et_fee)
    EditInput etFee;
    @BindView(R.id.btn_send)
    Button btnSend;
    @BindView(R.id.tv_total_amount)
    TextView tvTotalAmount;

    private TxPresenter mTxPresenter;
    private ViewHolder mViewHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        ButterKnife.bind(this);
        mTxPresenter = new TxPresenter();
        TxService.startTxService(TransmitKey.ServiceType.GET_SEND_DATA);
        initView();
    }

    private void initView() {
        MiningUtil.initSenderTxFee(etFee);
        etAmount.setFilters(new InputFilter[]{new MoneyValueFilter()});
        initTxFeeView();

        KeyboardUtils.registerSoftInputChangedListener(this, height -> {
            if(etFee != null){
                boolean isFeeFocus = etFee.hasFocus();
                boolean isVisible = KeyboardUtils.isSoftInputVisible(SendActivity.this);
                if(isFeeFocus && !isVisible){
                    resetViewFocus(llRoot);
                }
            }
        });

        Observable.create((ObservableOnSubscribe<View>)
                e -> btnSend.setOnClickListener(e::onNext))
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribe(new LogicObserver<View>() {
                    @Override
                    public void handleData(View view) {
                        KeyboardUtils.hideSoftInput(SendActivity.this);
                        checkForm();
                    }
                });
    }

    private void initTxFeeView() {
        SelectionEditText editText = etFee.getEditText();
        editText.setTextAppearance(this, R.style.style_normal_yellow);
        editText.setFilters(new InputFilter[]{new MoneyValueFilter().setDigits(2).setEndSpace()});
        editText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editText.setMaxLines(1);
    }

    @OnTextChanged({R.id.et_amount, R.id.et_input})
    void onTextChanged(CharSequence text){
        String amount = etAmount.getText().toString().trim();
        String fee = etFee.getText().trim();

        if(StringUtil.isEmpty(fee)){
            fee = BigInteger.ZERO.toString();
        }
        String total = FmtMicrometer.fmtFormatAdd(amount, fee);
        if(StringUtil.isNotEmpty(amount) ){
            String totalAmount = getText(R.string.send_tx_total_amount).toString();
            totalAmount = String.format(totalAmount, total);
            tvTotalAmount.setText(totalAmount);
            tvTotalAmount.setVisibility(View.VISIBLE);
        }else{
            tvTotalAmount.setVisibility(View.GONE);
        }
    }

    @OnClick({R.id.iv_fee})
    void onFeeSelectedClicked() {
        showSoftInput();
    }

    @OnTouch(R.id.et_fee)
    boolean onTxFeeClick(){
        return true;
    }

    private void resetViewFocus(View view) {
        if(view != null){
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.requestFocus();
        }
    }

    @Override
    public void checkForm() {
        String address = etAddress.getText().toString().trim();
        String amount = etAmount.getText().toString().trim();
        String memo = etMemo.getText().toString().trim();
        String fee = etFee.getText().trim();

        TransactionHistory tx = new TransactionHistory();
        tx.setToAddress(address);
        tx.setMemo(memo);
        tx.setAmount(amount);
        tx.setFee(fee);

        Wallet.validateTxParameter(tx, new LogicObserver<Boolean>() {
            @Override
            public void handleData(Boolean isSuccess) {
                if(isSuccess){
                    showSureDialog(tx);
                }
            }
        });
    }

    private void showSureDialog(TransactionHistory tx) {
        String amount = etAmount.getText().toString().trim();
        View view = LinearLayout.inflate(this, R.layout.view_dialog_send, null);
        mViewHolder = new ViewHolder(view);
        mViewHolder.tvToAddress.setText(tx.getToAddress());
        mViewHolder.tvToAmount.setText(amount);
        mViewHolder.tvToMemo.setText(tx.getMemo());
        loadTransExpiryView();
        new CommonDialog.Builder(this)
                .setContentView(view)
                .setNegativeButton(R.string.send_dialog_no, (dialog, which) -> {
                    dialog.cancel();
                    mViewHolder = null;
                })
                .setPositiveButton(R.string.send_dialog_yes, (dialog, which) -> {
                    dialog.cancel();
                    mViewHolder = null;
                    handleSendTransaction(tx);
                })
                .create().show();

    }

    private void loadTransExpiryView() {
        if(mViewHolder == null || mViewHolder.tvTransExpiry == null){
            return;
        }
        String transExpiry = getText(R.string.send_transaction_expiry).toString();
        transExpiry = String.format(transExpiry, UserUtil.getTransExpiryBlock(), UserUtil.getTransExpiryTime());
        mViewHolder.tvTransExpiry.setText(Html.fromHtml(transExpiry));
    }

    private LogicObserver<Boolean> sendLogicObserver = new LogicObserver<Boolean>() {
        @Override
        public void handleData(Boolean isSuccess) {
            ProgressManager.closeProgressDialog();
            if(isSuccess){
                // clear all editText data
                clearAllForm();
            }
        }
    };

    private void handleSendTransaction(TransactionHistory tx) {
        ProgressManager.showProgressDialog(this);
        mTxPresenter.handleSendTransaction(tx, sendLogicObserver);
    }
    @Override
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent object) {
        if (object == null) {
            return;
        }
        switch (object.getCode()) {
            case CLEAR_SEND:
                clearAllForm();
                break;
        }
    }

    private void clearAllForm() {
        if(etAddress == null){
            return;
        }
        etAddress.getText().clear();
        etAmount.getText().clear();
        etMemo.getText().clear();
        MiningUtil.initSenderTxFee(etFee);
    }

    private void showSoftInput() {
        etFee.setText(etFee.getText());
        resetViewFocus(etFee.getEditText());
        KeyboardUtils.showSoftInput(etFee.getEditText());
    }

    @Override
    protected void onDestroy() {
        if(KeyboardUtils.isSoftInputVisible(this)){
            KeyboardUtils.hideSoftInput(this);
            // handler InputMethodManager Leak
            FixMemLeak.fixLeak(this);
        }
        KeyboardUtils.unregisterSoftInputChangedListener(this);
        super.onDestroy();
    }

    class ViewHolder {
        @BindView(R.id.tv_to_address)
        TextView tvToAddress;
        @BindView(R.id.tv_to_amount)
        TextView tvToAmount;
        @BindView(R.id.tv_to_Memo)
        TextView tvToMemo;
        @BindView(R.id.tv_trans_expiry)
        TextView tvTransExpiry;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }

        @OnClick(R.id.tv_trans_expiry)
        void onClick(){
            Intent intent = new Intent(SendActivity.this, SettingActivity.class);
            startActivityForResult(intent, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        loadTransExpiryView();
    }
}