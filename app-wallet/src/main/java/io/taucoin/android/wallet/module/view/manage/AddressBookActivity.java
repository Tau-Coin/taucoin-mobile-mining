package io.taucoin.android.wallet.module.view.manage;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;
import butterknife.OnTextChanged;
import io.taucoin.android.wallet.R;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.db.entity.KeyValue;
import io.taucoin.android.wallet.module.presenter.UserPresenter;
import io.taucoin.android.wallet.module.view.manage.iview.IAddressView;
import io.taucoin.android.wallet.util.ProgressManager;
import io.taucoin.android.wallet.widget.CommonDialog;
import io.taucoin.android.wallet.widget.InputDialog;
import io.taucoin.foundation.net.callback.LogicObserver;
import io.taucoin.foundation.util.StringUtil;

public class AddressBookActivity extends BaseActivity implements IAddressView {

    @BindView(R.id.list_view_help)
    ListView listViewHelp;
    @BindView(R.id.refresh_layout)
    SmartRefreshLayout refreshLayout;
    @BindView(R.id.et_search_key)
    EditText etSearchKey;

    private UserPresenter mUserPresenter;
    private AddressAdapter mAdapter;
    private List<KeyValue> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);
        ButterKnife.bind(this);
        mUserPresenter = new UserPresenter(this);
        initView();
        ProgressManager.showProgressDialog(this);
        getData();
    }

    private void getData() {
        String key = StringUtil.getText(etSearchKey).trim();
        mUserPresenter.getAddressList(key);
    }

    public void loadData(List<KeyValue> data) {
        ProgressManager.closeProgressDialog();
        if(data != null){
            mDataList.clear();
            mDataList.addAll(data);
            mAdapter.setListData(data);
            refreshLayout.finishRefresh();
        }
    }

    @Override
    public void refreshView() {
        getData();
    }

    private void initView() {
        refreshLayout.setOnRefreshListener(this);
        mAdapter = new AddressAdapter(this);
        listViewHelp.setAdapter(mAdapter);
    }

    @Override
    public void onRefresh(RefreshLayout refreshlayout) {
        getData();
    }

    @OnTextChanged({R.id.et_search_key})
    void onTextChanged(CharSequence text){
        getData();
    }

    void switchAddress(KeyValue keyValue){
        mUserPresenter.switchAddress(AddressBookActivity.this, keyValue);
    }

    public void editName(KeyValue keyValue, boolean isSelf) {
        new InputDialog.Builder(this)
            .setNegativeButton(R.string.common_cancel, (InputDialog.InputDialogListener) (dialog, text) -> dialog.cancel())
            .setPositiveButton(R.string.common_done, (InputDialog.InputDialogListener) (dialog, text) -> {
                mUserPresenter.saveName(keyValue.getPubKey(), text, isSelf, new LogicObserver<KeyValue>() {
                    @Override
                    public void handleData(KeyValue keyValue) {
                        getData();
                    }
                });
                dialog.cancel();
            }).create().show();
    }

    public void deleteAddress(KeyValue keyValue, boolean isSelf) {
        if(isSelf){
            return;
        }
        View view = LinearLayout.inflate(this, R.layout.view_dialog_keys, null);
        TextView tvMsg = view.findViewById(R.id.tv_msg);
        String deleteText = getString(R.string.address_book_deleted_sure);
        tvMsg.setText(Html.fromHtml(deleteText));
        new CommonDialog.Builder(this)
            .setContentView(view)
            .setHorizontal()
            .setPositiveButton(R.string.common_no, (dialog, which) -> dialog.cancel())
            .setNegativeBgResource(R.drawable.grey_rect_round_bg)
            .setNegativeButton(R.string.common_yes, (dialog, which) -> {
                dialog.cancel();
                mUserPresenter.deleteAddress(keyValue.getPubKey(), new LogicObserver<Boolean>(){
                    @Override
                    public void handleData(Boolean aBoolean) {
                        getData();
                    }
                });
            }).create().show();
    }
}