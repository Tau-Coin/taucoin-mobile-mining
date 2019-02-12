package io.taucoin.android_app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SendActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText addressET;
    private EditText amountET;
    private EditText feeET;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        addressET = (EditText) findViewById(R.id.addressET);
        amountET = (EditText) findViewById(R.id.amountET);
        feeET = (EditText) findViewById(R.id.feeET);
        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String address = addressET.getText().toString();
        String amount = amountET.getText().toString();
        String fee = feeET.getText().toString();
        if(address.isEmpty() || amount.isEmpty() || fee.isEmpty()){
            Toast.makeText(this, "Can not be empty", Toast.LENGTH_SHORT).show();
        }else{
            String privateKey = Sp.getInstance().getString(Sp.PRIVATE_KEY, "");
            TaucoinApplication.getRemoteConnector().submitTransaction(privateKey, address, amount, fee);
        }
    }
}