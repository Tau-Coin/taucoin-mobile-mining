package io.taucoin.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MiningActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText targetET;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mining);
        targetET = (EditText) findViewById(R.id.targetET);
        Button miningButton = (Button) findViewById(R.id.miningButton);
        miningButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String targetAmountStr = targetET.getText().toString();
        int targetAmount;
        try {
            targetAmount = Integer.valueOf(targetAmountStr);
        }catch (Exception e){
            targetAmount = -1;
        }
        Intent intent = new Intent();
        intent.putExtra("targetAmount", targetAmount);
        this.setResult(RESULT_OK, intent);
        this.finish();
    }
}