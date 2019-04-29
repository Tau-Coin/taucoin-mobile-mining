package io.taucoin.android_app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LoopSendActivity extends AppCompatActivity implements View.OnClickListener {

    private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, "LoopSendTXTimer-" + cnt.getAndIncrement());
        }
    });

    private static ScheduledFuture<?> timerTask = null;

    private static TextView statusTV;
    private static TextView countTV;
    private static EditText addressET;
    private static EditText amountET;
    private static EditText feeET;
    private static EditText internalET;
    private static Button   startStopBtn;

    private static boolean sending = false;
    private static long sendingCount = 0;

    private static String sAddress;
    private static String sAmount;
    private static String sFee;
    private static long   sInternal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loop_send);

        statusTV   = (TextView) findViewById(R.id.statusTV);
        countTV    = (TextView) findViewById(R.id.countTV);
        addressET  = (EditText) findViewById(R.id.addressET);
        amountET   = (EditText) findViewById(R.id.amountET);
        feeET      = (EditText) findViewById(R.id.feeET);
        internalET = (EditText) findViewById(R.id.internalET);
        startStopBtn = (Button) findViewById(R.id.startStopButton);
        startStopBtn.setOnClickListener(this);

        setStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setStatus();
    }

    private void setStatus() {
        if (sending) {
            statusTV.setText("Sending");
            startStopBtn.setText("Stop");
            updateSentAmout();
        } else {
            statusTV.setText("");
            countTV.setVisibility(View.GONE);
            startStopBtn.setText("Start");
        }
    }

    private static synchronized void updateSentAmout() {
        sendingCount++;
        countTV.setVisibility(View.VISIBLE);
        countTV.setText("Sent:" + String.valueOf(sendingCount));
    }

    private static synchronized void resetSentAmout() {
        sendingCount = 0;
    }

    @Override
    public void onClick(View v) {
        if (!sending) {
            sAddress = addressET.getText().toString();
            sAmount = amountET.getText().toString();
            sFee = feeET.getText().toString();
            String internal = internalET.getText().toString();

            if (sAddress.isEmpty() || sAmount.isEmpty() || sFee.isEmpty() || internal.isEmpty()) {
                Toast.makeText(this, "Can not be empty", Toast.LENGTH_SHORT).show();
            } else {
                sInternal = Long.valueOf(internal).longValue() * 1000;
                sending = true;
                resetSentAmout();
                startLoop();
            }
        } else {
            sending = false;
            stopLoop();
        }

        setStatus();
    }

    private static void startLoop() {
        timerTask = timer.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    sendTx();
                    updateSentAmout();
                } catch (Throwable t) {
                    Log.e("LoopSendActivity", "Unhandled exception " + t);
                }
            }
        }, 500, sInternal, TimeUnit.MILLISECONDS);
    }

    private static void stopLoop() {
        if (timerTask != null) {
            timerTask.cancel(true);
        }
        timerTask = null;
    }

    private static void sendTx() {
        String privateKey = Sp.getInstance().getString(Sp.PRIVATE_KEY, "");
        TaucoinApplication.getRemoteConnector()
                .submitTransaction(privateKey, sAddress, sAmount, sFee);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopLoop();
    }
}
