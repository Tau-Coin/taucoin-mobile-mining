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

public class AccountStateActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText addressET;
    private TextView stateTextView;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && intent.getAction() != null){
                switch (intent.getAction()){
                    case RemoteConnectorManager.ACTION_ACCOUNT_STATE:
                        if(intent.getExtras() != null){
                            Bundle bundle = intent.getExtras();
                            StringBuilder stringBuilder = new StringBuilder();

                            String address = bundle.getString("address");
                            String balance = bundle.getString("balance");
                            String power   = bundle.getString("power");
                            stringBuilder.append("Address:" + address + "\n\n");
                            stringBuilder.append("Balance:" + balance + "\n\n");
                            stringBuilder.append("Power:" + power);

                            stateTextView.setText(stringBuilder.toString());
                            addressET.getText().clear();
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
        setContentView(R.layout.activity_account);
        addressET = (EditText) findViewById(R.id.addressET);
        stateTextView = (TextView) findViewById(R.id.stateTextView);
        findViewById(R.id.getButton).setOnClickListener(this);


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RemoteConnectorManager.ACTION_ACCOUNT_STATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onClick(View v) {
        String addressStr = addressET.getText().toString();

        if (!addressStr.isEmpty()) {
            TaucoinApplication.getRemoteConnector().getAccountState(addressStr);
        }
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
}
