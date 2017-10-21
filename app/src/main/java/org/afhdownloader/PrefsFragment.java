package org.afhdownloader;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;


/**
 * Created by daktak on 2/19/16.
 */
public class PrefsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);


        Preference prefDir = findPreference("prefDirectory");

        //prefDir.setIntent(i);
        prefDir.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(getActivity(), DirectoryPicker.class);
                /* If you set default dir , you can't navigate up!
                SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String dir = mySharedPreferences.getString("prefDirectory",null);
                i.putExtra(DirectoryPicker.START_DIR, dir);*/

                startActivity(i);
                return true;
            }
        });



    }

}


