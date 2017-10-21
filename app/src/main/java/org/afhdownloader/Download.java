package org.afhdownloader;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.net.URLDecoder;


/**
 * Created by daktak on 4/26/16.
 */
public class Download extends Service {

    /** indicates how to behave if the service is killed */
    int mStartMode;
    /** interface for clients that bind */
    IBinder mBinder;
    /** indicates whether onRebind should be used */
    boolean mAllowRebind;
    private static final String LOGTAG = LogUtil
            .makeLogTag(Download.class);
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra("url");
        int action = intent.getIntExtra("action",1);
        if (action == 1) {
            new ParseURL().execute(new String[]{url});
        } else if (action ==2 ){
            new ParseURLDownload().execute(new String[]{url});
        } else if (action == 3) {
            new downloadFirstThread().execute(new String[]{url});
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    private class ParseURL extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            return parseUrl(strings[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String newS = s.substring(1,s.length()-1);
            List<String> array = Arrays.asList(newS.split(","));
            if (MainActivity.instance != null) {
                MainActivity.instance.setList(array);
            }
        }
    }

    public String parseUrl(String url) {
        Log.d(LOGTAG, "Fetch: "+url);
        ArrayList<String> urls = new ArrayList<String>();
        try {
	    String userAgent = getString(R.string.user_agent);
            Document doc = Jsoup.connect(url).timeout(10*1000).followRedirects(true).userAgent(userAgent).get();
            SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String selector = mySharedPreferences.getString("prefSelector",getString(R.string.selector_val)).trim();
            //String selector = getString(R.string.selector_val);
            Elements links = doc.select(selector);

            for (Element link : links) {
                urls.add(link.ownText());
                urls.add(link.attr("href"));
            }
            //set title
            String head = getString(R.string.app_name);
            try {
                Elements h1s = doc.select(getString(R.string.head_selector));
                head = h1s.get(0).ownText();
            } catch (Exception e) {
                Log.d(LOGTAG,"Unable to find heading");
            }
            urls.add(head);

        } catch (Throwable t) {
            Log.e(LOGTAG,t.getMessage());
        }

        return urls.toString();
    }

    public String getFirstUrl(List<String> array){
        String url = "";
        for (String i : array) {
            i = i.trim();
            /*
            String prefix = "";
            if (!(i.startsWith("http"))) {
                prefix = getBaseUrl();
            }*/
            url = i.substring(2,i.length());
        }
        return url;
    }

    public String getBaseUrl() {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return mySharedPreferences.getString("prefBase",getString(R.string.base_val)).trim()+"/";
    }

    public String getMD5(String url) {
        String md5S ="";
        try {
	    Log.d(LOGTAG, "md5 url: "+url);
	    String userAgent = getString(R.string.user_agent);
            Document doc = Jsoup.connect(url).timeout(10 * 1000).followRedirects(true).userAgent(userAgent).get();
	    String select_md5 = getString(R.string.md5_sel_val);
	    Log.d(LOGTAG, "md5 selector: "+select_md5);
            Elements md5s = doc.select(select_md5);
            for (Element md5 : md5s) {
                md5S = md5.ownText();
		Log.d(LOGTAG, "md5 value: "+md5S);
            }
        } catch (Throwable t) {
            Log.e(LOGTAG,t.getMessage());
        }
        return md5S;
    }

    public ArrayList<String> getDLUrl(String url){
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String data = mySharedPreferences.getString("prefMirrorData",getString(R.string.mirrordata_val)) + url;
        String mUrl = mySharedPreferences.getString("prefMirrorURL",getString(R.string.mirrorurl_val));
	String userAgent = getString(R.string.user_agent);
        Log.d(LOGTAG, "Download parse: " +url);

        ArrayList<String> urls = new ArrayList<String>();

        try {
            HttpURLConnection httpcon = (HttpURLConnection) ((new URL(mUrl).openConnection()));
            httpcon.setDoOutput(true);
            httpcon.setRequestMethod("POST");
	    httpcon.setRequestProperty("User-Agent",userAgent);
	    httpcon.setRequestProperty("X-MOD-SBB-CTYPE","xhr");
            httpcon.connect();

            //Write
            OutputStream os = httpcon.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(data);
            writer.close();
            os.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(),"UTF-8"));

            String line = null;
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            br.close();
            //result = sb.toString();
            JSONObject jo = new JSONObject(sb.toString());
            JSONArray mirrors = jo.getJSONArray("MIRRORS");
            for (int i=0; i < mirrors.length(); i++){
                JSONObject mirror = mirrors.getJSONObject(i);
                String urlJS = mirror.getString("url");
                urls.add(urlJS);
            }

        } catch (Throwable t) {
            Log.e(LOGTAG,t.getMessage());
        }

        //create md5 file
        new dlMd5().execute(new String[]{urls.get(0), url});

        return urls;
    }

    private class dlMd5 extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            try {
                String aUrl = strings[0];
		String md5_ext = getString(R.string.md5_ext);
                int slash = aUrl.lastIndexOf("/");
                String filename = aUrl.substring(slash+1);
		filename = URLDecoder.decode(filename) + md5_ext;
		Log.d(LOGTAG,"Saving File: "+filename);
                String body = getMD5(getBaseUrl()+"/?"+strings[1]);
		Log.d(LOGTAG,"Found MD5: "+body);
                FileOutputStream fileout=openFileOutput(filename, MODE_PRIVATE);
                OutputStreamWriter outputWriter=new OutputStreamWriter(fileout);
                outputWriter.write(body);
                outputWriter.close();

            } catch (java.io.IOException e) {
                Log.e(LOGTAG,e.getMessage());
            }
            return null;
        }

    }

    private class downloadFirstThread extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            return parseUrl(strings[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String newS = s.substring(1,s.length()-1);
            List<String> array = Arrays.asList(newS.split(","));

            String url = getFirstUrl(array);
            new ParseURLDownload().execute(new String[]{url.toString()});

        }
    }

    private class ParseURLDownload extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            return getDLUrl(strings[0]).toString();
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            String newS = s.substring(1,s.length()-1);
            List<String> array = Arrays.asList(newS.split(","));
            String url = array.get(0);

            Log.d(LOGTAG, url);
            if (!(url.isEmpty())){
                int slash = url.lastIndexOf("/");
                String filename = url.substring(slash+1);
                download(url, getString(R.string.app_name), filename, filename);
            }

        }
    }
    public void download(String url, String desc, String title, String filename) {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String exten = "/";

        if (!url.endsWith(exten)) {

            Log.d(LOGTAG, "Downloading: " + url);
            boolean external = mySharedPreferences.getBoolean("prefExternal", false);


            String directory = mySharedPreferences.getString("prefDirectory", Environment.DIRECTORY_DOWNLOADS).trim();
            if (!(directory.startsWith("/"))) {
                directory = "/" + directory;
            }
            File direct = new File(Environment.getExternalStorageDirectory() + directory);

            if (!direct.exists()) {
                direct.mkdirs();
            }
            boolean fileExists = false;

            //check to see if we already have the file
            //this will make scheduling better
            if (EasyPermissions.hasPermissions(this, MainActivity.perms2)) {
                //have to assume we want to download the file if we can't check the dir
                File f = new File(direct.getAbsolutePath());
                File file[] = f.listFiles();
                for (int i = 0; i < file.length; i++) {
                    if (filename.equals(file[i].getName())) {
                        fileExists = true;
                    }
                }
            }

            if (!fileExists) {
                if (external) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(intent);
                } else if (EasyPermissions.hasPermissions(this, MainActivity.perms)) {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setDescription(desc);
                    request.setTitle(title);

                    // in order for this if to run, you must use the android 3.2 to compile your app
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    }

                    boolean wifionly = mySharedPreferences.getBoolean("prefWIFI", true);
                    //Restrict the types of networks over which this download may proceed.
                    if (wifionly) {
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
                    } else {
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    }
                    //Set whether this download may proceed over a roaming connection.
                    request.setAllowedOverRoaming(false);
                    request.setDestinationInExternalPublicDir(directory, filename);

                    // get download service and enqueue file
                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    manager.enqueue(request);
                } else {
                    Log.d(LOGTAG, "fallout");
                }
            } else {
                Log.d(LOGTAG, "file-exists");
            }
        } else {
            Log.d(LOGTAG, "Not downloading: " + url);
        }
    }

}
