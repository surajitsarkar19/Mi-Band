package surajit.com.miband.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by Surajit Sarkar on 10/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public interface BluetoothServiceListener {
    void onMessage(String message);
    void onDiscoveryStarted();
    void onDiscoveryStopped();
    void onDevicePaired(BluetoothDevice device);
    void onDeviceUnpaired(BluetoothDevice device);
    void onFoundNewDevice(BluetoothDevice device);//may contain null value for paired devices
    void onConnect(BluetoothDevice device);
    void onRead(int nRead, byte[] data);
    void onError(String message);
}
