package surajit.com.miband.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import surajit.com.miband.MainActivity;

/**
 * Created by Surajit Sarkar on 13/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class BluetoothService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private Context context;
    private BluetoothUtility bluetoothUtility;
    private BluetoothServiceListener listener;
    private String TAG = MainActivity.TAG;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(listener!=null) {
                switch (msg.what) {

                    case Constants.MESSAGE_READ:
                        int readLength = msg.arg1;
                        byte[] data = (byte[])msg.obj;
                        listener.onRead(readLength,data);
                        break;

                    case Constants.MESSAGE_STATE_CHANGE:
                        int state = msg.arg1;
                        if(state == BluetoothUtility.STATE_CONNECTED){
                            Bundle bundle = msg.getData();
                            BluetoothDevice device = bundle.getParcelable(Constants.EXTRA_DEVICE);
                            listener.onConnect(device);
                        } else{
                            //listener.
                        }
                        break;

                    case Constants.MESSAGE_ERROR:
                        String errrorMessage = msg.getData().getString(Constants.EXTRA_MESSAGE);
                        listener.onError(errrorMessage);
                        break;

                }
            }
            return false;
        }
    });

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void registerCallback(BluetoothServiceListener listener){
        this.listener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"Bluetooth Service Started");
        context = getApplicationContext();
        bluetoothUtility = new BluetoothUtility(context,handler);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
        Log.i(TAG,"Bluetooth Service Destroyed");
    }

    public void start(){
        if(bluetoothUtility!=null){
            bluetoothUtility.start();
        }
    }

    public void stop(){
        if(bluetoothUtility!=null){
            bluetoothUtility.stop();
        }
    }

    public void write(byte[] buffer) {
        if(bluetoothUtility!=null){
            bluetoothUtility.write(buffer);
        }
    }

    public void connect(BluetoothDevice device){
        if(bluetoothUtility!=null){
            bluetoothUtility.connect(device,true);
        }
    }

    public BluetoothSocket getConnectedSocket(){
        if(bluetoothUtility!=null){
            return bluetoothUtility.getConnectedSocket();
        } else{
            return null;
        }
    }

}
