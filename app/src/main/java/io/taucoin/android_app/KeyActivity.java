package io.taucoin.android_app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class KeyActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText privateKeyET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key);
        privateKeyET = (EditText)findViewById(R.id.privateKeyET);
        Button clearButton = (Button) findViewById(R.id.clearButton);
        Button importButton = (Button) findViewById(R.id.importButton);
        Button initButton = (Button) findViewById(R.id.initButton);

        clearButton.setOnClickListener(this);
        importButton.setOnClickListener(this);
        initButton.setOnClickListener(this);

        String privateKey = Sp.getInstance().getString(Sp.PRIVATE_KEY, "");
        privateKeyET.setText(privateKey);
        privateKeyET.setSelection(privateKey.length());
    }

    @Override
    public void onClick(final View v) {
        switch(v.getId()){
            case R.id.clearButton:
                privateKeyET.getText().clear();
                break;
            case R.id.importButton:
                String privateKey = privateKeyET.getText().toString();
                if(!privateKey.isEmpty()){
                    Sp.getInstance().putString(Sp.PRIVATE_KEY, privateKey);
                    TaucoinApplication.getRemoteConnector().importForgerPrivkey(privateKey);
                    setResult(RESULT_OK);
                    this.finish();
                }else{
                    Toast.makeText(this, "private key is empty!", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.initButton:
                privateKey = privateKeyET.getText().toString();
                if(!privateKey.isEmpty()){
                    Sp.getInstance().putString(Sp.PRIVATE_KEY, privateKey);
                    TaucoinApplication.getRemoteConnector().importPrivkeyAndInit(privateKey);
                    setResult(RESULT_OK);
                    this.finish();
                }else{
                    Toast.makeText(this, "private key is empty!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
