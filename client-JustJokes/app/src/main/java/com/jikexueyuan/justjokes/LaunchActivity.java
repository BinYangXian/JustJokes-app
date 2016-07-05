package com.jikexueyuan.justjokes;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;

public class LaunchActivity extends AppCompatActivity {

    private BroadcastReceiver netWorkConnectedBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launch_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()  //此句呈现了PlaceholderFragment
                    .add(R.id.container, new MainFragment())
                    .commit();
        }
        netWorkConnectedBroadcastReceiver = new NetWorkChangeBroadcastReceiver();
        registerReceiver();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();

        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        getApplicationContext().registerReceiver(netWorkConnectedBroadcastReceiver, filter);

    }

    @Override
    public void onDestroy() {
        getApplicationContext().unregisterReceiver(netWorkConnectedBroadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

}
