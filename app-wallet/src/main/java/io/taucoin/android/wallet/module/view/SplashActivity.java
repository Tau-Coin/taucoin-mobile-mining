package io.taucoin.android.wallet.module.view;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.github.naturs.logger.Logger;

import io.reactivex.schedulers.Schedulers;
import io.taucoin.android.wallet.R;

import java.util.List;
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
import io.taucoin.android.wallet.util.PermissionUtils;
import io.taucoin.foundation.util.AppUtil;
import io.taucoin.foundation.util.permission.EasyPermissions;

public class SplashActivity extends BaseActivity {

    @BindView(R.id.app_title)
    TextView appTitle;

    private volatile boolean isAsk = false;

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

            requestWriteLogPermissions();

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

    private synchronized void splashJump() {
        if(!isAsk){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            Logger.i("Jump to MainActivity");
        }
        isAsk = false;
    }

    /**
     * Shielded return key
     * */
    @Override
    public void onBackPressed() {

    }

    private void requestWriteLogPermissions() {
        boolean isAndroidQ = Build.VERSION.SDK_INT > Build.VERSION_CODES.P;
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if(!isAndroidQ && !EasyPermissions.hasPermissions(this, permission)){
            isAsk = true;
            EasyPermissions.requestPermissions(this,
                    this.getString(R.string.permission_tip_upgrade_denied),
                    permissionCallbacks,
                    PermissionUtils.REQUEST_PERMISSIONS_STORAGE, permission);
        }
    }

    private synchronized void handlePermissionsCallBack() {
        if(!isAsk){
            splashJump();
        }
        isAsk = false;
    }

    private EasyPermissions.PermissionCallbacks permissionCallbacks = new EasyPermissions.PermissionCallbacks(){

        @Override
        public void onPermissionsGranted(int requestCode, List<String> granted) {

        }

        @Override
        public void onPermissionsDenied(int requestCode, List<String> denied) {
            handlePermissionsCallBack();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PermissionUtils.REQUEST_PERMISSIONS_STORAGE:
                handlePermissionsCallBack();
                break;
            default:
                break;
        }
    }
}