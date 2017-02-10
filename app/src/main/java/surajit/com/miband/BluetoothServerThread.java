package surajit.com.miband;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Surajit Sarkar on 7/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class BluetoothServerThread extends Thread {

    private final BluetoothServerSocket mmServerSocket;
    private BluetoothAdapter mBluetoothAdapter;
    public static UUID APP_BLUETOOTH_UUID = UUID.fromString("a672880a-ecfc-11e6-c016-6f6e6c696e65");
    private static String TAG = MainActivity.TAG;
    private boolean bRunning;

    public interface BluetoothServerConnectionListener {
        void onAcceptBluetoothConnection(BluetoothSocket socket);
    }

    private BluetoothServerConnectionListener listener;

    public BluetoothServerThread(BluetoothAdapter mBluetoothAdapter,BluetoothServerConnectionListener listener) {
        this.mBluetoothAdapter = mBluetoothAdapter;
        this.listener = listener;
        bRunning = false;
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("SRS_MI_BAND", APP_BLUETOOTH_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
    }

    public boolean isRunning(){
        return bRunning;
    }

    public void run() {
        Log.i(TAG, "BluetoothServerThread started");
        bRunning = true;
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                break;
            } catch (Exception e) {
                Log.e(TAG, "Socket's interrupted", e);
                break;
            }

            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                if(listener!=null){
                    listener.onAcceptBluetoothConnection(socket);
                }
                //cancel();
                break;
            }
        }
        bRunning = false;
        Log.i(TAG, "BluetoothServerThread stopped");
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}