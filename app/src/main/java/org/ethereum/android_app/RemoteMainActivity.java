package org.ethereum.android_app;

import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.ethereum.android.service.ConnectorHandler;
import org.ethereum.android.service.EthereumConnector;
import org.ethereum.android.service.EthereumClientMessage;
import org.ethereum.android.service.events.BlockEventData;
import org.ethereum.android.service.events.EventData;
import org.ethereum.android.service.events.EventFlag;
import org.ethereum.android.service.events.MessageEventData;
import org.ethereum.android.service.events.PeerDisconnectEventData;
import org.ethereum.android.service.events.PendingTransactionsEventData;
import org.ethereum.android.service.events.TraceEventData;
import org.ethereum.android.service.events.VMTraceCreatedEventData;
import org.ethereum.config.SystemProperties;
import org.ethereum.net.p2p.HelloMessage;
import org.ethereum.net.rlpx.Node;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class RemoteMainActivity extends ActionBarActivity implements ActivityInterface {

    private Toolbar toolbar;
    private ViewPager viewPager;
    private SlidingTabLayout tabs;
    private TabsPagerAdapter adapter;
    protected ArrayList<FragmentInterface> fragments = new ArrayList<>();

    private ScheduledExecutorService initializer = Executors.newSingleThreadScheduledExecutor();
    public static final int BOOTUP_DELAY_INIT_SECONDS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        adapter = new TabsPagerAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(adapter);;

        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
        tabs.setDistributeEvenly(true);
        tabs.setViewPager(viewPager);

        initializer.schedule(
                new InitTask(EthereumApplication.ethereumConnector, EthereumApplication.handlerIdentifier),
                BOOTUP_DELAY_INIT_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void registerFragment(FragmentInterface fragment) {

        if (!fragments.contains(fragment)) {
            fragments.add(fragment);
        }
    }

    private static class InitTask implements Runnable {
        private EthereumConnector ethereumConnector;
        private String handlerIdentifier;

        public InitTask(EthereumConnector ethereumConnector, String handlerIdentifier) {
            this.ethereumConnector = ethereumConnector;
            this.handlerIdentifier = handlerIdentifier;
        }

        @Override
        public void run() {
            ethereumConnector.init(handlerIdentifier, null);
        }
    }
}
