package surajit.com.miband.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import surajit.com.miband.MainActivity;

/**
 * Created by Surajit Sarkar on 9/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class BluetoothConnectionThread extends Thread {

    BluetoothSocket mmSocket;
    InputStream mmInStream;
    OutputStream mmOutStream;

    private boolean bRunning;

    private static String TAG = MainActivity.TAG;

    public BluetoothConnectionThread(BluetoothSocket socket){
        this.mmSocket  = socket;

        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        bRunning = false;

        // Get the BluetoothSocket input and output streams
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    @Override
    public void run() {
        Log.i(TAG, "BEGIN BluetoothConnectionThread");
        bRunning = true;
        byte[] buffer = new byte[1024];
        int bytes;

        // Keep listening to the InputStream while connected
        while (true) {
            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);


            } catch (IOException e) {
                Log.e(TAG, "disconnected", e);
                break;
            }
        }
        bRunning = false;
        Log.i(TAG, "END BluetoothConnectionThread");
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
