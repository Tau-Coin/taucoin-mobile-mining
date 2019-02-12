package io.taucoin.android_app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class TestsFragment extends Fragment implements View.OnClickListener {

    static final int REQUEST_CODE_KEY = 1000;
    Button keyButton;
    Button syncButton;
    Button sendButton;
    Button miningButton;

    boolean isMiningStart = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.getAction() != null){
                switch (intent.getAction()){
                    case RemoteConnectorManager.ACTION_BLOCK_SYNC:
                        sendButton.setEnabled(true);
                        miningButton.setEnabled(true);
                        break;
                    default:
                        break;
                }
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RemoteConnectorManager.ACTION_BLOCK_SYNC);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_tests, container, false);

        keyButton = (Button)view.findViewById(R.id.keyButton);
        syncButton = (Button)view.findViewById(R.id.syncButton);
        sendButton = (Button)view.findViewById(R.id.sendButton);
        miningButton = (Button)view.findViewById(R.id.miningButton);

        keyButton.setOnClickListener(this);
        syncButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);
        miningButton.setOnClickListener(this);

        changeButtonState(false);
        return view;
    }

    private void changeButtonState(boolean isImportKey) {
        syncButton.setEnabled(isImportKey);
        if(!isImportKey){
            sendButton.setEnabled(false);
            miningButton.setEnabled(false);

            changeMiningState();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_KEY && resultCode == Activity.RESULT_OK){
            changeButtonState(true);
        }
    }

    private void changeMiningState() {
        if(miningButton.isEnabled()){
            isMiningStart = !isMiningStart;
        }else{
            isMiningStart = false;
        }
        miningButton.setText(isMiningStart ? R.string.btn_mining_end : R.string.btn_mining_start);
    }

    @Override
    public void onClick(final View v) {

        switch(v.getId()){
            case R.id.keyButton:
                Intent intent = new Intent(getActivity(), KeyActivity.class);
                startActivityForResult(intent, REQUEST_CODE_KEY);
                break;
            case R.id.syncButton:
                TaucoinApplication.getRemoteConnector().startSync();
                break;
            case R.id.sendButton:
                intent = new Intent(getActivity(), SendActivity.class);
                startActivity(intent);
                break;
            case R.id.miningButton:
                if(isMiningStart){
                    TaucoinApplication.getRemoteConnector().stopBlockForging();
                }else{
                    TaucoinApplication.getRemoteConnector().startBlockForging();
                }
                changeMiningState();
                break;
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
}