package com.microsoft.band.sdk.sampleapp;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;
import org.apache.http.client.HttpClient;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.TimeZone;
import java.util.Date;

import android.widget.TextView;
import android.widget.EditText;
import android.view.View;
import android.os.AsyncTask;
import android.util.Base64;

import android.provider.Settings.Secure;
import android.provider.Settings;

import org.json.JSONObject;

public class connectToServer {

    private static final String DEBUG_TAG = "HttpExample";
    private EditText downloadUrlText;
    private EditText uploadUrlText;
    private EditText paramText;
    private TextView resultText;



    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    //Given a URL, establishes an HTTPUrlConnection and uploads
    // the data given to it via HTTPPost
    public void uploadUrl(long timestamp, float value) throws IOException {
        InputStream is = null;
        int len = 2500;
        String myurl = "http://epiwork.hcii.cs.cmu.edu/~afsaneh/connection.php";
        URL url = new URL("http://epiwork.hcii.cs.cmu.edu/~afsaneh/connection.php");
        String paramString = "time = " + Long.toString(timestamp) + "  " +
                "value = " + Float.toString(value);
        String urlParameter = "param1="+paramString;
        byte[] postData = urlParameter.getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.encodeToString(postData, Base64.DEFAULT);
        //int postDataLength = postData.length;
        byte[] uploadData = myurl.getBytes(StandardCharsets.UTF_8);
        int uploadDataLength = uploadData.length;
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        //conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setRequestProperty("Content-Length", Integer.toString(uploadDataLength));
        conn.setUseCaches(false);
/*
    try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
        wr.write( postData );
    }
  */

        String offsetAmount = base64Encoder(timeZoneBuilder());
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.write(uploadData);
        //conn.getInputStream();
        //String android_id = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        //String contentAsString = readIt(is, len);
      //  return base64Encoder(android_id) + "    " + offsetAmount + "      " + base64 + "    " + uploadData;
    }

    private String timeZoneBuilder() {
        String timeZone = "";
        Date now = new Date();
        int offsetFromUTC = TimeZone.getDefault().getOffset(now.getTime())/3600000;
        String offsetAmount = Integer.toString(offsetFromUTC);
        if (offsetFromUTC>=0) {
            timeZone = timeZone + "+";
        } else if (offsetFromUTC<0) {
            timeZone = timeZone + "-";
        }
        if (Math.abs(offsetFromUTC)<10) {
            timeZone = timeZone + "0";
        }
        timeZone = timeZone + Integer.toString(Math.abs(offsetFromUTC))+ ":00" ;
        return timeZone;
    }
    private String base64Encoder (String stringToEncode) {
        byte[] stringToByte = stringToEncode.getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.encodeToString(stringToByte, Base64.DEFAULT);
        return base64;
    }
}

