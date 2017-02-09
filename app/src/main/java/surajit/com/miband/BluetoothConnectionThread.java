package surajit.com.miband;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Surajit Sarkar on 9/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class BluetoothConnectionThread extends Thread {

    BluetoothSocket socket;
    InputStream inputStream;
    OutputStream outputStream;

    public BluetoothConnectionThread(BluetoothSocket socket){
        this.socket  = socket;
    }

    @Override
    public void run() {
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
