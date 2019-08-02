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
import android.widget.Toast;

public class TestsFragment extends Fragment implements View.OnClickListener {

    static final int REQUEST_CODE_KEY = 1000;
    static final int REQUEST_START_MINING = 1001;
    Button keyButton;
    Button syncButton;
    Button sendButton;
    Button miningButton;
    Button blockButton;
    Button txButton;
    Button accountButton;
    Button txLoopButton;
    Button createChain;

    boolean isMiningStart = false;
    int targetAmount = -1;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.getAction() != null){
                switch (intent.getAction()){
                    case RemoteConnectorManager.ACTION_BLOCK_SYNC:
                        sendButton.setEnabled(true);
                        miningButton.setEnabled(true);
                        blockButton.setEnabled(true);
                        txButton.setEnabled(true);
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
        blockButton = (Button)view.findViewById(R.id.blockButton);
        txButton = (Button)view.findViewById(R.id.txButton);
        accountButton = (Button)view.findViewById(R.id.accountButton);
        txLoopButton = (Button)view.findViewById(R.id.txloop);
        createChain = (Button)view.findViewById(R.id.createChain);

        keyButton.setOnClickListener(this);
        syncButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);
        miningButton.setOnClickListener(this);
        blockButton.setOnClickListener(this);
        txButton.setOnClickListener(this);
        accountButton.setOnClickListener(this);
        txLoopButton.setOnClickListener(this);
        createChain.setOnClickListener(this);

        changeButtonState(false);
        return view;
    }

    private void changeButtonState(boolean isImportKey) {
//        syncButton.setEnabled(isImportKey);
//        if(!isImportKey){
//            sendButton.setEnabled(false);
//            miningButton.setEnabled(false);
//            blockButton.setEnabled(false);
//            txButton.setEnabled(false);
//
//            changeMiningState();
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_KEY && resultCode == Activity.RESULT_OK){
            changeButtonState(true);
        }else if(requestCode == REQUEST_START_MINING && resultCode == Activity.RESULT_OK){
            targetAmount = data.getIntExtra("targetAmount", -1);
            Toast.makeText(getActivity(), " " +targetAmount, Toast.LENGTH_SHORT).show();
            TaucoinApplication.getRemoteConnector().startBlockForging(targetAmount);
            changeMiningState();
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
                    TaucoinApplication.getRemoteConnector().stopBlockForging(targetAmount);
                    changeMiningState();
                }else{
                    intent = new Intent(getActivity(), MiningActivity.class);
                    startActivityForResult(intent, REQUEST_START_MINING);
                }
                break;
            case R.id.blockButton:
                intent = new Intent(getActivity(), BlockHashActivity.class);
                startActivity(intent);
                break;
            case R.id.txButton:
                intent = new Intent(getActivity(), PoolTxsActivity.class);
                startActivity(intent);
                break;

            case R.id.accountButton:
                intent = new Intent(getActivity(), AccountStateActivity.class);
                startActivity(intent);
                break;

            case R.id.txloop:
                intent = new Intent(getActivity(), LoopSendActivity.class);
                startActivity(intent);
                break;
            case R.id.createChain:
                intent = new Intent(getActivity(),activity_create_chain.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
}