package surajit.com.miband.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;

/**
 * Created by Surajit Sarkar on 13/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public abstract class BluetoothService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private Context context;
    private BluetoothUtility bluetoothUtility;
    private BluetoothServiceListener listener;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if(listener!=null) {
                switch (msg.what) {
                    case Constants.MESSAGE_NEW_DEVICE_FOUND:
                        BluetoothDevice device = msg.getData().getParcelable(Constants.EXTRA_DEVICE);
                        listener.onFoundNewDevice(device);
                        break;
                    case Constants.MESSAGE_SCAN_STARTED:
                        listener.onDiscoveryStarted();
                        break;
                    case Constants.MESSAGE_SCAN_STOPPED:
                        listener.onDiscoveryStopped();
                        break;
                    case Constants.MESSAGE_DEVICE_PAIRED:
                        BluetoothDevice device1 = msg.getData().getParcelable(Constants.EXTRA_DEVICE);
                        listener.onDevicePaired(device1);
                        break;
                    case Constants.MESSAGE_DEVICE_UNPAIRED:
                        BluetoothDevice device2 = msg.getData().getParcelable(Constants.EXTRA_DEVICE);
                        listener.onDeviceUnpaired(device2);
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
    }
}
