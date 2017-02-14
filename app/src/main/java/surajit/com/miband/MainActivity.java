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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import surajit.com.miband.bluetooth.BluetoothActivityNew;
import surajit.com.miband.bluetooth.BluetoothListAdapter;

public class MainActivity extends BluetoothActivityNew implements View.OnClickListener, AdapterView.OnItemClickListener {

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
                stopScan();
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
    protected void onBluetoothServiceConnected() {
        Log.i(TAG,"Service Connected");
        mService.start();// start server
    }

    @Override
    protected void notifyDeviceListChanged() {
        if(arrayAdapter!=null) {
            arrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConnect(BluetoothDevice device) {
        //Snackbar.make(listViewBluetooth,"New connection accepted "+socket,Snackbar.LENGTH_SHORT).show();

        Intent intent = new Intent(this,BluetoothConnectionActivity.class);//get the socket connection from bluetoothservice
        intent.putExtra("device",device);
        startActivity(intent);
    }

    @Override
    public void onRead(int nRead, byte[] data) {

    }

    @Override
    public void onError(String message) {

    }
}
