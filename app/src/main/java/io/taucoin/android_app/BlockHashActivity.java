package io.taucoin.android_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

public class BlockHashActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText fromET;
    private EditText countET;
    private TextView countTextView;
    private TextView listTextView;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.getAction() != null){
                switch (intent.getAction()){
                    case RemoteConnectorManager.ACTION_BLOCK_HASH:
                        if(intent.getExtras() != null){
                            Bundle bundle = intent.getExtras();
                            List<String> blockHash = bundle.getStringArrayList("hashList");
                            if(blockHash != null){
                                countTextView.setText("count:" + blockHash.size());
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < blockHash.size(); i++) {
                                    stringBuilder.append(blockHash.get(i) + "\n");
                                }
                                listTextView.setText(stringBuilder.toString());
                            }

                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_block);
        fromET = (EditText) findViewById(R.id.fromET);
        countET = (EditText) findViewById(R.id.countET);
        countTextView = (TextView) findViewById(R.id.countTextView);
        listTextView = (TextView) findViewById(R.id.listTextView);
        findViewById(R.id.getButton).setOnClickListener(this);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RemoteConnectorManager.ACTION_BLOCK_HASH);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onClick(View v) {
        String fromStr = fromET.getText().toString();
        String countStr = countET.getText().toString();
        long from = 0;
        long limit = 0;
        try {
            from = Long.valueOf(fromStr);
            limit = Long.valueOf(countStr);
        }catch (Exception e){
        }
        if(from >= 0 && limit > 0){
            TaucoinApplication.getRemoteConnector().getBlockHashList(from, limit);
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
}