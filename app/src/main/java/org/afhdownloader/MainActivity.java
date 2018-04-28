package org.afhdownloader;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

/**
 * daktak
 */

public class MainActivity extends AppCompatActivity
        implements EasyPermissions.PermissionCallbacks {
    private static final String LOGTAG = LogUtil
            .makeLogTag(MainActivity.class);
    public static String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static String[] perms2 = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int REQUEST_PREFS = 99;
    private static final int RC_EXT_WRITE =1;
    private static final int RC_EXT_READ=2;
    public static MainActivity instance = null;
    public ArrayList<String> md5check = new ArrayList<String>();
    public ArrayList<String> names = new ArrayList<String>();
    public ArrayList<String> urls = new ArrayList<String>();
    public String directory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        String[] names = new String[] {getString(R.string.loading)};/*
        TabHost myTabHost = (TabHost)this.findViewById(R.id.th_set_menu_tabhost);
        myTabHost.setup();

        for(int i=0; i<5; i++ )
{
    final TabSpec x=tabHost2.newTabSpec("x");
    View row = inflater.inflate(R.layout.indicator1,null);
    final TextView indicator1 =(TextView) row.findViewById(R.id.textView_indicator1);
    indicator1.setText(indicator_list[i]);
    // indicator1.setShadowLayer(1, 0, 1, 0xFF013201);

    x.setIndicator(row);

    x.setContent(new TabContentFactory() {
        public View createTabContent(String arg) {
        return gallery2;
        }
    });

    tabHost2.addTab(x);

            ls1 = new ListView(Main_screen.this);
        TabSpec ts1 = myTabHost.newTabSpec("TAB_TAG_1");
        ts1.setIndicator("Tab1");
        ts1.setContent(new TabHost.TabContentFactory(){
            public View createTabContent(String tag)
            {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(Main_screen.this,android.R.layout.simple_list_item_1,new String[]{"item1","item2","item3"});
                ls1.setAdapter(adapter);
                return ls1;
            }
        });
        myTabHost.addTab(ts1);

}*/
        ListView mainListView = (ListView) findViewById( R.id.listView );
        ListAdapter listAdapter =  new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);
        // Set the ArrayAdapter as the ListView's adapter.
        mainListView.setAdapter( listAdapter );

        if (!(EasyPermissions.hasPermissions(this, perms))) {
            // Ask for both permissio  ns
            EasyPermissions.requestPermissions(this, getString(R.string.extWritePerm), RC_EXT_WRITE, perms);
            //otherwise use app
        }

        if (!(EasyPermissions.hasPermissions(this, perms2))) {
            // Ask for both permissions
            EasyPermissions.requestPermissions(this, getString(R.string.extReadPerm), RC_EXT_READ, perms2);
            //otherwise use app
        }

        setAlarm(this);
        run(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setAlarm(this);
        run(this);
    }

    public void setAlarm(Context context){
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean daily = mySharedPreferences.getBoolean("prefDailyDownload",false);
        if (daily) {
            Log.d(LOGTAG, "Setting daily alarm");
            setRecurringAlarm(context);
        } else {
            CancelAlarm(context);
        }
    }

    public SharedPreferences getPref() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    public void setRecurringAlarm(Context context) {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int hour =  Integer.parseInt(mySharedPreferences.getString("prefHour", getString(R.string.hour_val)));
        int minute = Integer.parseInt(mySharedPreferences.getString("prefMinute", getString(R.string.minute_val)));
        Calendar updateTime = Calendar.getInstance();
        //updateTime.setTimeZone(TimeZone.getTimeZone("GMT"));
        updateTime.set(Calendar.HOUR_OF_DAY, hour);
        updateTime.set(Calendar.MINUTE, minute);

        Intent downloader = new Intent(context, AlarmReceiver.class);
        PendingIntent recurringDownload = PendingIntent.getBroadcast(context,
                0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(
                Context.ALARM_SERVICE);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                updateTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, recurringDownload);

    }

    public void CancelAlarm(Context context)
    {
        Intent downloader = new Intent(context, AlarmReceiver.class);
        PendingIntent recurringDownload = PendingIntent.getBroadcast(context,
                0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) getSystemService(
                Context.ALARM_SERVICE);
        alarms.cancel(recurringDownload);
    }

    public void run(Context context) {
        //new ParseURL().execute(new String[]{buildPath(context)});
        Intent service = new Intent(context, Download.class);
        service.putExtra("url",buildPath(context));
        service.putExtra("action",1);
        context.startService(service);
    }

    //get for a specific int
    public String buildPath(Context context) {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String base = mySharedPreferences.getString("prefBase",getString(R.string.base_val)).trim();
        String flid = mySharedPreferences.getString("prefFlid",getString(R.string.flid_val)).trim();
        //if instring ,
        //List<String> flida = Arrays.asList(flid.split(","));
        String url_ext = "?w=files&flid=";

        return base+"/"+url_ext+flid;
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
            Intent prefs = new Intent(getBaseContext(), SetPreferenceActivity.class);
            startActivityForResult(prefs, REQUEST_PREFS);
            run(this);
            setAlarm(this);
            return true;
        }
	if (id == R.id.action_refresh) {
		run(this);
	}
        if (id == R.id.action_reboot) {
            ExecuteAsRootBase e = new ExecuteAsRootBase() {
                    @Override
                    protected ArrayList<String> getCommandsToExecute() {
                        ArrayList<String> a = new ArrayList<String>();
                        a.add("reboot recovery");
                        return a;
                    }
                };
            e.execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Some permissions have been granted
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Some permissions have been denied

    }

    public String getBaseUrl() {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return mySharedPreferences.getString("prefBase",getString(R.string.base_val)).trim();
    }

    public String readFile(String name) {

        StringBuilder out = new StringBuilder();
        try {
            FileInputStream filein = openFileInput(name);
            InputStreamReader inputreader = new InputStreamReader(filein);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;

            while (( line = buffreader.readLine()) != null) {
                out.append(line);
            }

            filein.close();
        } catch (Exception e) {
            Log.d(LOGTAG,"Unable to open: "+name);
        }
        return out.toString();
    }

    public void writeFile(String name, String body){
        try {
            FileOutputStream fileout = openFileOutput(name, MODE_PRIVATE);
            OutputStreamWriter outputWriter = new OutputStreamWriter(fileout);
            outputWriter.write(body);
            outputWriter.close();
        } catch (Exception e) {
            Log.w(LOGTAG, "Unable to write: "+name);
        }
    }

    public void setList(List<String> values)  {
        SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        directory = mySharedPreferences.getString("prefDirectory",Environment.DIRECTORY_DOWNLOADS).trim();
        boolean external = mySharedPreferences.getBoolean("prefExternal",false);
        md5check.clear();
        urls.clear();
        names.clear();
        String md5_ext = getString(R.string.md5_ext);
        final String md5_calc_ext = getString(R.string.md5calc_ext);
        if (external){
            directory = Environment.DIRECTORY_DOWNLOADS;
        }

        File direct = new File(Environment.getExternalStorageDirectory() + "/" + directory);
        if (!direct.exists()) {
            direct.mkdirs();
        }
        Log.w(LOGTAG, directory);
        File file[] = new File[0];
        if (EasyPermissions.hasPermissions(this, perms2)) {
            try {
                file = direct.listFiles();
            } catch (Exception e) {
                Log.w(LOGTAG, "Cant "+e.getMessage());
            }
        }

        getSupportActionBar().setTitle(values.get(values.size()-1));

        //for each returned value - filename and url
        for (int j = 0; j < values.size()-1; j+=2) {
            String md5val = "";
            String url = values.get(j+1).trim();
            String name = values.get(j).trim();
            int slash = name.lastIndexOf("/")+1;
            try {
                name = name.substring(slash);
            } catch (Exception e){
                Log.w(LOGTAG, "Cant find slash in "+name);
            }
            names.add(name);

            //for every result - check if file exists
            // then check if downloaded md5 exists
            // then check if calc exists

            for (int k = 0; k < file.length; k++) {

                if (name.equals(file[k].getName())) {
                    String md5 = readFile(name + md5_ext);
                    if (!md5.isEmpty()) {
                        String md5calc = readFile(name+md5_calc_ext);
                        if (md5calc.isEmpty()) {
                            md5calc = MD5.calculateMD5(file[k]);
                        }
                        if (md5calc.equalsIgnoreCase(md5)) {
                            md5val = "Y";
                            //cache this result
                            writeFile(name+md5_calc_ext, md5calc);
                        } else {
                            md5val = "N";
                            //don't cache, in the event the file is still downloading
                        }

                    } else {
                        md5val = "U";
                    }
                }
            }
            md5check.add(md5val);
            urls.add(url.substring(2,url.length()));

        }
        //newest on top
        Collections.reverse(urls);
        Collections.reverse(names);
        Collections.reverse(md5check);
        String[] namesS = new String[names.size()];
        namesS = names.toArray(namesS);
        // Find the ListView resource.
        ListView mainListView = (ListView) findViewById( R.id.listView );
        String [] md5checkS = new String[md5check.size()];
        md5checkS = md5check.toArray(md5checkS);

        MyCustomAdapter listAdapter = new MyCustomAdapter(this, namesS, file, md5checkS);
        //ListAdapter listAdapter =  new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, names);

        // Set the ArrayAdapter as the ListView's adapter.
        mainListView.setAdapter( listAdapter );
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        mainListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                boolean dis = true;
                                if (md5check.get(position).isEmpty()) { dis = false; };
                                return dis;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    final int pos = position;
                                    DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            File direct = new File(Environment.getExternalStorageDirectory() + "/" + directory+"/"+names.get(pos));
                                            Log.d(LOGTAG, "Delete " + direct.getName());
                                            if (direct.exists()&&direct.isFile()) { direct.delete(); }
                                            File md5file = new File(getFilesDir(), names.get(pos)+md5_calc_ext );
                                            if (md5file.exists()&&md5file.isFile()) { md5file.delete(); }
                                            if (MainActivity.instance != null) {
                                                run(MainActivity.instance);
                                            }
                                        }
                                    };
                                    message_dialog_yes_no(getString(R.string.delete) + " " + names.get(pos)+"?" , yesListener);
                                }
                            }
                        });
        mainListView.setOnTouchListener(touchListener);

        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
            if (view.isEnabled()) {
                String url = urls.get(position);
                Context context = getBaseContext();
                Intent service = new Intent(context, Download.class);
                service.putExtra("url", url.toString());
                //service.putExtra("name",names.get(position).toString());
                service.putExtra("action", 2);
                context.startService(service);

                //new ParseURLDownload().execute(new String[]{url.toString()});

            } else {
                Log.d(LOGTAG, "Entry disabled");
            }
          }
        });
    }

    public void message_dialog_yes_no (String msg, DialogInterface.OnClickListener yesListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        builder.setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(getString(R.string.yes), yesListener)
                .setNegativeButton(getString(R.string.no),  new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }})
                .show();
    }

    /**
	 * Executes commands as root user
	 * @author http://muzikant-android.blogspot.com/2011/02/how-to-get-root-access-and-execute.html
	 */
	public abstract class ExecuteAsRootBase {
	  public final boolean execute() {
	    boolean retval = false;
	    try {
	      ArrayList<String> commands = getCommandsToExecute();
	      if (null != commands && commands.size() > 0) {
	        Process suProcess = Runtime.getRuntime().exec("su");

	        DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());

	        // Execute commands that require root access
	        for (String currCommand : commands) {
	          os.writeBytes(currCommand + "\n");
	          os.flush();
	        }

	        os.writeBytes("exit\n");
	        os.flush();

	        try {
	          int suProcessRetval = suProcess.waitFor();
	          if (255 != suProcessRetval) {
	            // Root access granted
	            retval = true;
	          } else {
	            // Root access denied
	            retval = false;
	          }
	        } catch (Exception ex) {
	          Log.e(LOGTAG, "Error executing root action\n"+ ex.toString());
	        }
	      }
	    } catch (IOException ex) {
	      Log.w(LOGTAG, "Can't get root access", ex);
	    } catch (SecurityException ex) {
	      Log.w(LOGTAG, "Can't get root access", ex);
	    } catch (Exception ex) {
	      Log.w(LOGTAG, "Error executing internal operation", ex);
	    }

	    return retval;
	  }

	  protected abstract ArrayList<String> getCommandsToExecute();
	}


    @Override
    protected void onResume() {
        super.onResume();
        instance = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        instance = null;
    }

}
