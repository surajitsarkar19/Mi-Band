package surajit.com.miband;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import surajit.com.miband.bluetooth.BluetoothActivity;
import surajit.com.miband.bluetooth.BluetoothListAdapter;

public class MainActivity extends BluetoothActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private Button buttonScan;
    private ListView listViewBluetooth;
    private BluetoothListAdapter arrayAdapter;

    ObjectAnimator scanAnimation;

    public static String TAG = "Mi Band";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonScan = (Button) findViewById(R.id.buttonScan);
        listViewBluetooth = (ListView) findViewById(R.id.listViewBluetooth);

        setUpAnimation();

        arrayAdapter = new BluetoothListAdapter(this, bluetoothDeviceList);
        listViewBluetooth.setAdapter(arrayAdapter);
        buttonScan.setOnClickListener(this);
        listViewBluetooth.setOnItemClickListener(this);

        startBluetooth();
    }

    private void setUpAnimation(){
        scanAnimation = ObjectAnimator.ofInt(buttonScan, "textColor", Color.RED, Color.TRANSPARENT);
        scanAnimation.setDuration(1000);
        scanAnimation.setEvaluator(new ArgbEvaluator());
        scanAnimation.setRepeatCount(ValueAnimator.INFINITE);
        scanAnimation.setRepeatMode(ValueAnimator.REVERSE);
    }

    private void startScanAnimation(){
        if(scanAnimation!=null) {
            buttonScan.setText("STOP");
            scanAnimation.start();
        }
    }

    private void stopScanAnimation(){
        if(scanAnimation!=null) {
            scanAnimation.cancel();
            buttonScan.setText("SCAN");
            buttonScan.setTextColor(Color.BLACK);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.buttonScan){
            if(buttonScan.getText().toString().equalsIgnoreCase("SCAN")) {
                scanDevices();
            } else{
                stopDiscovery();
                stopScanAnimation();
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String mac = bluetoothDeviceList.get(position).getAddress();
        BluetoothDevice device = getBluetoothDevice(mac);
        if(device!=null){
            if(isPaired(device)){
                //connecTo(device);
                Intent intent = new Intent(this,BluetoothConnectionActivity.class);
                intent.putExtra("mac",device.getAddress());
                startActivity(intent);
            } else{
                pairDevice(device);
            }
        }
    }

    @Override
    public void onMessage(String message) {
        if(listViewBluetooth!=null)
        Snackbar.make(listViewBluetooth,message,Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDiscoveryStarted() {
        startScanAnimation();
    }

    @Override
    public void onDiscoveryStopped() {
        stopScanAnimation();
    }

    @Override
    public void onDevicePaired(BluetoothDevice device) {
        if(arrayAdapter!=null) {
            arrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDeviceUnpaired(BluetoothDevice device) {
        if(arrayAdapter!=null) {
            arrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onFoundNewDevice(BluetoothDevice device) {
        if(arrayAdapter!=null) {
            arrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onAccept(BluetoothSocket socket) {

    }

    @Override
    public void onConnect(BluetoothSocket socket) {

    }
}
