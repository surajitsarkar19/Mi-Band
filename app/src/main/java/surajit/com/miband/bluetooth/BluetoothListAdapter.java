package surajit.com.miband.bluetooth;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import surajit.com.miband.R;

/**
 * Created by Surajit Sarkar on 1/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class BluetoothListAdapter extends ArrayAdapter<BluetoothItem> {

    public static int TYPE_TITLE = 0;
    public static int TYPE_ITEM = 1;

    public BluetoothListAdapter(Context context, List<BluetoothItem> objects) {
        super(context, R.layout.bluetooth_list_item, objects);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).getType();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder; // view lookup cache stored in tag
        BluetoothItem device = getItem(position);
        int itemType = getItemViewType(position);
        if (convertView == null) {
            viewHolder = new ViewHolder();
            if(itemType == TYPE_ITEM) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.bluetooth_list_item, parent, false);
                viewHolder.name = (TextView) convertView.findViewById(R.id.textViewName);
                viewHolder.address = (TextView) convertView.findViewById(R.id.textViewAddress);
            } else{
                ViewGroup.LayoutParams lparams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                TextView tv=new TextView(getContext());
                tv.setBackgroundColor(Color.LTGRAY);
                tv.setTextColor(Color.BLACK);
                tv.setPadding(10,10,10,10);
                tv.setLayoutParams(lparams);
                viewHolder.name = tv;
                viewHolder.address = null;
                convertView = tv;

            }
            convertView.setTag(viewHolder);
        } else {
            // View is being recycled, retrieve the viewHolder object from tag
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if(viewHolder.name!=null) {
            viewHolder.name.setText(device.getName());
        }
        if(viewHolder.address!=null) {
            viewHolder.address.setText(device.getAddress());
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView name;
        TextView address;
    }

}
