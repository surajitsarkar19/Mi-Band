package surajit.com.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Surajit Sarkar on 8/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class BluetoothClientConnectionThread extends Thread {

    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private String TAG = "BluetoothClientThread";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothClientConnectionListener listener;
    private boolean bRunning;

    public interface BluetoothClientConnectionListener {
        void onClientConnected(BluetoothSocket socket);
    }

    public BluetoothClientConnectionThread(BluetoothAdapter mBluetoothAdapter, BluetoothDevice device, BluetoothClientConnectionListener listener) {
        // Use a temporary object that is later assigned to mmSocket
        // because mmSocket is final.
        BluetoothSocket tmp = null;
        bRunning = false;
        mmDevice = device;
        this.mBluetoothAdapter = mBluetoothAdapter;
        this.listener = listener;
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = device.createRfcommSocketToServiceRecord(BluetoothServerThread.APP_BLUETOOTH_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        mmSocket = tmp;
    }

    public void run() {
        bRunning = true;
        // Cancel discovery because it otherwise slows down the connection.
        mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket.connect();

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            if(listener!=null){
                listener.onClientConnected(mmSocket);
            }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
        }

        bRunning = false;
    }

    public boolean isRunning(){
        return bRunning;
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

}
