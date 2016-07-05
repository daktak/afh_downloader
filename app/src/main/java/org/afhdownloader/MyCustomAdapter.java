package org.afhdownloader;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;

public class MyCustomAdapter extends ArrayAdapter<String> {
    private final Context context;
    private final String[] values;
    private final File[] file;
    private final String[] md5check;
    private static final String LOGTAG = LogUtil
        .makeLogTag(MainActivity.class);

    public MyCustomAdapter(Context context, String[] values, File[] file, String[] md5check) {
        super(context, R.layout.rowlayout, values);
        this.context = context;
        this.values = values;
        this.file = file;
        this.md5check = md5check;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewHolder holder;
        String s = values[position];
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.rowlayout, parent, false);
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.label);
            convertView.setTag(holder);//Log.w("BasketBuild",s);
            try {
                for (int j = 0; j < file.length; j++) {

                    if (s.equals(file[j].getName())) {
                        Log.d(LOGTAG, file[j].getName() + " md5: " + md5check[j]);
                        if (md5check[j].equals("Y")) {
                            Log.d(LOGTAG,"Yes");
                            holder.text.setTextColor(R.color.md5_match);
                        } else if (md5check[j].equals("N")) {
                            holder.text.setTextColor(R.color.md5_nomatch);
                        } else {
                            holder.text.setTextColor(R.color.disabledText);
                        }

                        //Log.w("BasketBuild","have file: "+s+ ":"+file[j] + " : "+ j+"pos:" + position);
                        convertView.setEnabled(false);
                    }
                }
            } catch (Exception e) {
                Log.w(LOGTAG, "Cant "+e.getMessage());
            }
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.text.setText(s);

        return convertView;
    }

    static class ViewHolder {
        TextView text;
    }

}