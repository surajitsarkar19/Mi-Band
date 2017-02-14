package surajit.com.miband;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import surajit.com.miband.bluetooth.BluetoothActivityNew;

public class BluetoothConnectionActivity extends BluetoothActivityNew {

    TextView textViewConnectionStatus;
    ProgressBar progressbar;
    private static String TAG = MainActivity.TAG;
    BluetoothDevice device;
    boolean bConnectionMade = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        textViewConnectionStatus = (TextView) findViewById(R.id.textViewConnectionStatus);
        progressbar = (ProgressBar) findViewById(R.id.progressbar);

        String statusMsg;
        String mac = getIntent().getStringExtra("mac");
        if(mac!=null) {
            device = getBluetoothDevice(mac);
            connectRemoteDevice();

            statusMsg = "Connecting to " + device.getName();
            setStatusMessage(statusMsg, true);
        } else{
            BluetoothDevice device1 = getIntent().getParcelableExtra("device");
            statusMsg = "Connected to " + device1.getName();
            setStatusMessage(statusMsg, false);
        }
    }

    private void setStatusMessage(String msg, boolean bShowProgress){
        if(msg!=null){
            textViewConnectionStatus.setText(msg);
        }
        if(bShowProgress){
            progressbar.setVisibility(View.VISIBLE);
        } else{
            progressbar.setVisibility(View.GONE);
        }
    }

    private synchronized void connectRemoteDevice(){
        if(!bConnectionMade){
            if(device!=null && mService!=null) {
                bConnectionMade = true;
                mService.connect(device);
            }
        }
    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onDiscoveryStarted() {

    }

    @Override
    public void onDiscoveryStopped() {

    }

    @Override
    protected void onBluetoothServiceConnected() {
        Log.i(TAG,"Service Connected");
        connectRemoteDevice();
    }

    @Override
    protected void notifyDeviceListChanged() {

    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Log.i(TAG,"Connected .. "+device);
        setStatusMessage("Connected .. "+device,false);
    }

    @Override
    public void onRead(int nRead, byte[] data) {

    }

    @Override
    public void onError(String message) {
        setStatusMessage(message, false);
    }
}
