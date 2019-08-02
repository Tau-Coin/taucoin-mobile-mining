package io.taucoin.android_app;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class activity_create_chain extends AppCompatActivity implements View.OnClickListener {

    private EditText chainnameET;
    private EditText totalamountET;
    private EditText feeET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_chain);
        chainnameET = (EditText) findViewById(R.id.chainnameET);
        totalamountET = (EditText) findViewById(R.id.totalamountET);
        feeET = (EditText) findViewById(R.id.createfeeET);
        Button createbt = (Button) findViewById(R.id.createBT);
        createbt.setOnClickListener(this);
    }

    @Override
    public void onClick(View v){
        String coinName = chainnameET.getText().toString();
        String coinTotalAmount = totalamountET.getText().toString();
        String createCoinFee = feeET.getText().toString();
        if(coinName.isEmpty() || coinTotalAmount.isEmpty() || createCoinFee.isEmpty()){
            Toast.makeText(this, "Can not be empty", Toast.LENGTH_SHORT).show();
        }else{
            String privateKey = Sp.getInstance().getString(Sp.PRIVATE_KEY, "");
            TaucoinApplication.getRemoteConnector().submitGenesisTransaction(privateKey,coinName, coinTotalAmount, createCoinFee);
        }
    }
}
