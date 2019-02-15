package io.taucoin.android_app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class PoolTxsActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView countTextView;
    private TextView listTextView;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.getAction() != null){
                switch (intent.getAction()){
                    case RemoteConnectorManager.ACTION_POOL_TXS:
                        if(intent.getExtras() != null){
                            Bundle bundle = intent.getExtras();
                            List<String> blockHash = bundle.getStringArrayList("txs");
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
        setContentView(R.layout.activity_pool_txs);
        countTextView = (TextView) findViewById(R.id.countTextView);
        listTextView = (TextView) findViewById(R.id.listTextView);
        findViewById(R.id.getButton).setOnClickListener(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RemoteConnectorManager.ACTION_POOL_TXS);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onClick(View v) {
        TaucoinApplication.getRemoteConnector().getPendingTxs();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
}