//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.sampleapp;

import com.aware.utils.Https;
import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
//import com.microsoft.band.sdk.sampleapp.streaming.R;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.SampleRate;
import com.microsoft.band.sensors.BandPedometerEvent;
import com.microsoft.band.sensors.BandPedometerEventListener;
import com.microsoft.band.sensors.BandCaloriesEvent;
import com.microsoft.band.sensors.BandCaloriesEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.BandDistanceEvent;
import com.microsoft.band.sensors.BandDistanceEventListener;
import com.microsoft.band.sensors.BandContactEvent;
import com.microsoft.band.sensors.BandContactEventListener;
import com.microsoft.band.sensors.BandContactState;
import com.microsoft.band.sensors.BandUVEvent;
import com.microsoft.band.sensors.BandUVEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.UVIndexLevel;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringBufferInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.io.File;
import android.os.Environment;
import java.util.Date;
import java.sql.Timestamp;
import java.util.TimeZone;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class BandStreamingAppActivity extends Aware_Plugin {

    private BandClient client = null;
    private Button btnStart;
    private TextView txtStatus;


    private File sdCard;
    private File dir;
    private File file;


    private int heartRate;
    private float x;
    private float y;
    private float z;
    private long steps;
    private float skinTemp;
    private long calories;
    private float distance;
    private BandContactState contact;
    private UVIndexLevel UV;
    private int gyroscope;

    private String heartRateL = "Unix Timestamp,Local Timestamp,Heart Rate\n";
    private String accelerometerL = "Unix Timestamp,Local Timestamp,x,y,z\n";
    private String stepsL = "Unix Timestamp,Local Timestamp,steps\n";
    private String skinTempL  ="Unix Timestamp,Local Timestamp,Skin Temperature\n";
    private String caloriesL = "Unix Timestamp,Local Timestamp,Calories\n";
    private String distanceL = "Unix Timestamp,Local Timestamp,Distance\n";
    private String UVL = "Unix Timestamp,Local Timestamp,UV\n";

    private static class Params {
        String filename;
        String update;
        String firstLine;

        Params(String filename, String update, String firstLine) {
            this.filename = filename;
            this.update = update;
            this.firstLine = firstLine;
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

     //   ArrayList<NameValuePair> request = new ArrayList<NameValuePair>();
     //   request.add(new BasicNameValuePair("Time", "3333"));
     //   request.add(new BasicNameValuePair("Value", "23"));

      //  new Https(getApplicationContext()).dataPOST( "https://api.awareframework.com/index.php/webservice/index/421/31wEGFcTZDnY" + "/" + "band" + "/insert", request, true);

        new appTask().execute(); //runs receivers on background thread
        sdCard = Environment.getExternalStorageDirectory();
        dir = new File (sdCard.getAbsolutePath() + "/band");
        dir.mkdirs();
    }

    @Override
    public int onStartCommand(Intent intent, int x, int y){
        super.onStartCommand(intent,x,y);
        new appTask().execute(); //runs receivers on background thread
        sdCard = Environment.getExternalStorageDirectory();
        dir = new File (sdCard.getAbsolutePath() + "/band");
        dir.mkdirs();
        return 1;
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        //TODO:  unregister everything
        try {
            client.getSensorManager().unregisterAccelerometerEventListeners();
        } catch (BandIOException e) {
            appendToUI(e.getMessage());
        }
    }


    private class appTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    //register receivers
                    client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, SampleRate.MS16);
                    client.getSensorManager().registerPedometerEventListener(mPedometerEventListener);
                    client.getSensorManager().registerSkinTemperatureEventListener(mSkinTemperatureEventListener);
                    client.getSensorManager().registerCaloriesEventListener(mCaloriesEventListener);
                    client.getSensorManager().registerUVEventListener(mUVEventListener);
                    client.getSensorManager().registerDistanceEventListener(mDistanceEventListener);
                    if(client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    } else {
                        //TODO: fill in ???
                        //client.getSensorManager().requestHeartRateConsent(MainActivity.this, mHeartRateConsentListener);
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage();
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }


    private void appendToUI(final String string) {
		/*this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				txtStatus.setText(string);
			}
		});*/
    }


    private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {
                long time = event.getTimestamp();
                Date date= new java.util.Date();

                x = event.getAccelerationX();
                y = event.getAccelerationY();
                z = event.getAccelerationZ();
                String update = Long.toString(time);
                update += "," + new Timestamp(date.getTime());
                update += ("," + x + "," + y + "," + z + "\n");
                Params params = new Params("accelerometer.csv",update,accelerometerL);
                //new writeOnFile().execute(params);
                //writeOnDB(time, 21);
                appendToUI(update);

            }
        }
    };


    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                long time = event.getTimestamp();
                Date date= new java.util.Date();
                heartRate = event.getHeartRate();
                String update = Long.toString(time);
                update += "," + new Timestamp(date.getTime());
                update += ("," + heartRate + "\n");
                Params params = new Params("heartRate.csv",update,heartRateL);
                //new writeOnFile().execute(params);
                //writeOnDB(time,heartRate);
                appendToUI(update);
            }
        }
    };


    private HeartRateConsentListener mHeartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean b) {
            // handle user's heart rate consent decision
            if (!b){
                // Consent hasn't been given
                appendToUI(String.valueOf(b));
            }
        }
    };


    private BandSkinTemperatureEventListener mSkinTemperatureEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent event) {
            if (event != null) {
                long time = event.getTimestamp();
                Date date= new java.util.Date();
                skinTemp = event.getTemperature();
                String update = Long.toString(time);
                update += "," + new Timestamp(date.getTime());
                update += ("," + skinTemp + "\n");
                Params params = new Params("skinTemperature.csv",update,skinTempL);
                //new writeOnFile().execute(params);
                writeOnCalendar(time, skinTemp);
                //writeOnDB(time, skinTemp);
                /**
                try {
                   // writeOnServer(time, skinTemp);
                   // uploadUrl("http://epiwork.hcii.cs.cmu.edu/~afsaneh/connection.php", time, skinTemp);
                }catch(IOException e){
                }**/
                appendToUI(update);
            }
        }
    };


    private BandPedometerEventListener mPedometerEventListener = new BandPedometerEventListener() {
        @Override
        public void onBandPedometerChanged(BandPedometerEvent event) {
            if (event != null) {
                long time = event.getTimestamp();
                Date date= new java.util.Date();
                steps = event.getTotalSteps();
                String update = Long.toString(time);
                update += "," + new Timestamp(date.getTime());
                update += ("," + steps + "\n");
                Params params = new Params("pedometer.csv",update,stepsL);
                //new writeOnFile().execute(params);
                appendToUI(update);
            }
        }
    };


    private BandCaloriesEventListener mCaloriesEventListener = new BandCaloriesEventListener() {
        @Override
        public void onBandCaloriesChanged(BandCaloriesEvent event) {
            if (event != null) {
                long time = event.getTimestamp();
                Date date= new java.util.Date();
                calories = event.getCalories();
                String update = Long.toString(time);
                update += "," + new Timestamp(date.getTime());
                update += ("," + calories + "\n");
                Params params = new Params("calories.csv",update,caloriesL);
                //new writeOnFile().execute(params);
                appendToUI(update);
            }
        }
    };

    private BandDistanceEventListener mDistanceEventListener = new BandDistanceEventListener() {
        @Override
        public void onBandDistanceChanged(BandDistanceEvent event) {
            if (event != null) {
                long time = event.getTimestamp();
                Date date= new java.util.Date();
                distance = event.getTotalDistance();
                //can have motiontype, pace and speed as well
                String update = Long.toString(time);
                update += "," + new Timestamp(date.getTime());
                update += ("," + distance + "\n");
                Params params = new Params("distance.csv",update,distanceL);
                //new writeOnFile().execute(params);
                appendToUI(update);
            }
        }
    };


    private BandUVEventListener mUVEventListener = new BandUVEventListener() {
        @Override
        public void onBandUVChanged(BandUVEvent event) {
            if (event != null){
                long time = event.getTimestamp();
                Date date= new java.util.Date();
                UV = event.getUVIndexLevel();
                String update = Long.toString(time);
                update += "," + new Timestamp(date.getTime());
                update += ("," + UV + "\n");
                Params params = new Params("UV.csv",update,UVL);
                //new writeOnFile().execute(params);
                appendToUI(update);
            }
        }
    };

/*
is this information needed?
	private BandGyroscopeEventListener mGyrosocopeEventListener = new BandGyroscopeEventListener() {
		@Override
		public void onBandGyroscopeChanged(BandGyroscopeEvent event) {
			if (event != null){
				time = event.getTimestamp();
			}
		}
	};
*/

    private BandContactEventListener mContactEventListener = new BandContactEventListener() {
        @Override
        public void onBandContactChanged(BandContactEvent event) {
            if (event != null) {
                long time = event.getTimestamp();
                contact = event.getContactState();
            }
        }
    };


    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }
        appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }


    private class writeOnFile extends AsyncTask<Params, Void, Void>{
        @Override
        protected Void doInBackground(Params...params) {
            String filename = params[0].filename;
            String update = params[0].update;
            String firstLine = params[0].firstLine;
            file = new File(dir, filename);
            if(!file.exists()) {
                try {
                    file.createNewFile();
                    FileOutputStream nfos = null;
                    try {
                        nfos = new FileOutputStream(file,true);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    OutputStreamWriter nosw = new OutputStreamWriter(nfos);
                    try {
                        nosw.write(firstLine);
                        nosw.flush();
                        nosw.close();

                    } catch (Exception ex) {
                        Log.e("DEBUG", "HERE");
                        ex.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file,true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            try {
                osw.write(update);
                osw.flush();
                osw.close();

            } catch (Exception ex) {
                Log.e("DEBUG", "HERE");
                ex.printStackTrace();
            }
            return null;
        }
    }
    private void writeOnDB (long timeStamp, float value){
        //Intent aware = new Intent(this, Aware.class);
       // startService(aware);
        ContentValues new_data = new ContentValues();
        new_data.put(bandProvider.Band_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
        new_data.put(bandProvider.Band_Data.TIMESTAMP, timeStamp);
        new_data.put(bandProvider.Band_Data._Value, value);
        getContentResolver().insert(bandProvider.Band_Data.CONTENT_URI, new_data);
    }
/**

    private class UploadWebpageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                final String result = uploadUrl(urls[0]);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        resultText.setText(result);
                    }
                });
                return uploadUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

**/
        private String uploadUrl(String myurl, long timestamp, float value) throws IOException {
            InputStream is = null;
            int len = 2500;
            URL url = new URL(myurl);
            String paramString = "time = " + Long.toString(timestamp) + "  " +
                    "value = " + Float.toString(value);
            String urlParameter = "param1="+paramString;
            byte[] postData = urlParameter.getBytes(StandardCharsets.UTF_8);
            String base64 = Base64.encodeToString(postData, Base64.DEFAULT);
            int postDataLength = postData.length;
            byte[] uploadData = myurl.getBytes(StandardCharsets.UTF_8);
            int uploadDataLength = uploadData.length;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("charset", "utf-8");
            //conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
            conn.setUseCaches(false);

            try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
                wr.write( postData );
            }


            String offsetAmount = base64Encoder(timeZoneBuilder());
            //DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            //wr.write(uploadData);
            conn.getInputStream();
            String android_id = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            //String contentAsString = readIt(is, len);
            return "uploading data";
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
        // onPostExecute displays the results of the AsyncTask.

        //Given a URL, establishes an HTTPUrlConnection and uploads
    // the data given to it via HTTPPost
    public void writeOnServer(long timestamp, float value) throws IOException {
        String myURL = "http://epiwork.hcii.cs.cmu.edu/~afsaneh/connection.php";
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

        //String offsetAmount = base64Encoder(timeZoneBuilder());
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.write(uploadData);
        //conn.getInputStream();
        //String android_id = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        //String contentAsString = readIt(is, len);
        //  return base64Encoder(android_id) + "    " + offsetAmount + "      " + base64 + "    " + uploadData;
    }

    private void writeOnCalendar (long timeStamp, float value){
        ContentValues event = new ContentValues();

        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, timeStamp);
        values.put(CalendarContract.Events.DTEND, timeStamp + 60*60*1000);
        values.put(CalendarContract.Events.TITLE, Float.toString(value));
        values.put(CalendarContract.Events.CALENDAR_ID, 1);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, "New_York");

        Uri eventsUri = Uri.parse("content://com.android.calendar/events");
        Uri insertedUri = getContentResolver().insert(eventsUri, values);
    }

}

