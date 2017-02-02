package surajit.com.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends PermissionActivity implements View.OnClickListener {

    private static int REQUEST_ENABLE_BT = 345;
    private Button buttonScan;
    private ListView listViewBluetooth;
    private BluetoothListAdapter arrayAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    List<BluetoothItem> bluetoothDeviceList;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonScan = (Button) findViewById(R.id.buttonScan);
        listViewBluetooth = (ListView) findViewById(R.id.listViewBluetooth);
        bluetoothDeviceList = new ArrayList<>();
        arrayAdapter = new BluetoothListAdapter(this,bluetoothDeviceList);
        listViewBluetooth.setAdapter(arrayAdapter);

        buttonScan.setOnClickListener(this);
        checkBluetooth();
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    @Override
    public void onPermissionGranted(String permission) {

    }

    @Override
    public void onPermissionDenied(String permission) {
        finish();
    }

    private void checkBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Snackbar.make(buttonScan,"Device does not support Bluetooth",Snackbar.LENGTH_SHORT).show();
        } else{
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void askToEnableBluetooth(){
        if (mBluetoothAdapter!=null && !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void enableBluetooth(){
        //this will need bluetooth admin permission
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }
    }

    private void disableBluetooth(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                Snackbar.make(buttonScan,"Bluetooth Enabled",Snackbar.LENGTH_SHORT).show();
            } else{
                Snackbar.make(buttonScan,"Bluetooth disabled",Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void listPairedDevices(){
        bluetoothDeviceList.clear();
        bluetoothDeviceList.add(new BluetoothItem("Paired Devices","",BluetoothListAdapter.TYPE_TITLE));
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BluetoothItem bluetoothItem = new BluetoothItem(deviceName,deviceHardwareAddress,BluetoothListAdapter.TYPE_ITEM);
                bluetoothDeviceList.add(bluetoothItem);
            }
        }
        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.buttonScan){
            listPairedDevices();
        }
    }
}
