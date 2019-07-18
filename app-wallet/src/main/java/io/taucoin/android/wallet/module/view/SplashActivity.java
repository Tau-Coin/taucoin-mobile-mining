package io.taucoin.android.wallet.module.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.github.naturs.logger.Logger;

import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.R;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.taucoin.android.wallet.base.BaseActivity;
import io.taucoin.android.wallet.module.view.main.MainActivity;
import io.taucoin.android.wallet.net.callback.CommonObserver;
import io.taucoin.android.wallet.util.ActivityUtil;
import io.taucoin.android.wallet.util.MiningUtil;
import io.taucoin.foundation.util.AppUtil;

public class SplashActivity extends BaseActivity {

    @BindView(R.id.app_title)
    TextView appTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Is it the root of the task stack?
        Logger.d("SplashActivity.onCreate");
        if (!this.isTaskRoot()) {
            Intent intent = getIntent();
            if (intent != null) {
                String action = intent.getAction();
                if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(action)) {
                    Logger.d("SplashActivity immediate finish");
                    finish();
                    ActivityUtil.moveTaskToFront();
                }
            }
        } else {
            Logger.d("SplashActivity show");
            // Open for the first time
            setContentView(R.layout.activity_splash);
            ButterKnife.bind(this);
            initView();

            Logger.i("SplashActivity onCreate");

            MiningUtil.handleUpgradeCompatibility();

            // delay 3 seconds jump
            Observable.timer(3, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(mDisposableObserver);
        }
    }

    private void initView() {
        String title = getText(R.string.splash_title).toString();
        title = String.format(title, AppUtil.getVersionName(this));
        appTitle.setText(title);
    }

    private CommonObserver<Long> mDisposableObserver = new CommonObserver<Long>() {

        @Override
        public void onComplete() {
            splashJump();
        }
    };

    private void splashJump() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        Logger.i("Jump to MainActivity");
    }

    /**
     * Shielded return key
     * */
    @Override
    public void onBackPressed() {

    }
}