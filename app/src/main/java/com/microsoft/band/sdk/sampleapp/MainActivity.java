package com.microsoft.band.sdk.sampleapp;

import android.os.Bundle;
import android.content.Intent;
import android.preference.PreferenceActivity;
import com.aware.Aware;




/**
 * Created by jennachoo on 7/2/15.
 */
public class MainActivity extends PreferenceActivity {

    public static final String STATUS_PLUGIN_TEMPLATE = "status_plugin_template";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Aware.startPlugin(this, getPackageName());
        //Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
        Intent apply = new Intent(getApplicationContext(), BandStreamingAppActivity.class);
        getApplicationContext().startService(apply);
        //sendBroadcast(apply);
    }

}