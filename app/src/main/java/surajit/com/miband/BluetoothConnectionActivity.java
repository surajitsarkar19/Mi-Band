package surajit.com.miband;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import surajit.com.miband.bluetooth.BluetoothActivity;

public class BluetoothConnectionActivity extends BluetoothActivity {

    TextView textViewConnectionStatus;
    ProgressBar progressbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        textViewConnectionStatus = (TextView) findViewById(R.id.textViewConnectionStatus);
        progressbar = (ProgressBar) findViewById(R.id.progressbar);
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
    public void onDevicePaired(BluetoothDevice device) {

    }

    @Override
    public void onDeviceUnpaired(BluetoothDevice device) {

    }

    @Override
    public void onFoundNewDevice(BluetoothDevice device) {

    }

    @Override
    public void onAccept(BluetoothSocket socket) {

    }

    @Override
    public void onConnect(BluetoothSocket socket) {

    }
}
