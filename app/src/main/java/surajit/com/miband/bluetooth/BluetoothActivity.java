package surajit.com.miband.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import surajit.com.miband.MainActivity;
import surajit.com.miband.PermissionActivity;

/**
 * Created by Surajit Sarkar on 13/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public abstract class BluetoothActivity extends PermissionActivity implements BluetoothServiceListener {
    public static int REQUEST_ENABLE_BT = 345;
    public static int REQUEST_START_DISCOVERIBILITY = 267;
    public static int DISCOVERABLE_DURATION = 5*60;

    protected List<BluetoothItem> bluetoothDeviceList;
    protected List<BluetoothItem> pairedList;
    protected List<BluetoothItem> unpairedList;
    protected BluetoothAdapter mBluetoothAdapter;

    private static String TAG = MainActivity.TAG;

    protected BluetoothService mService;
    protected boolean mBound = false;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);//it will get all available devices
                onFoundNewDevice(device);
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                Log.i(TAG,"Scan Started");
                onDiscoveryStarted();

            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                Log.i(TAG,"Scan Finished");
                onDiscoveryStopped();

            } else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,0);

                if(bondState == BluetoothDevice.BOND_BONDED) {
                    onDevicePaired(device);
                } else if(bondState == BluetoothDevice.BOND_NONE){
                    onDeviceUnpaired(device);
                }
            }
        }
    };



    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            mService.registerCallback(BluetoothActivity.this);
            mBound = true;
            onBluetoothServiceConnected();
            Log.i(TAG,"Bluetooth Service Bounded");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
            Log.i(TAG,"Bluetooth Service Unbounded");
        }
    };

    private void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver,intentFilter);
    }

    protected abstract void onBluetoothServiceConnected();
    protected abstract void notifyDeviceListChanged();

    @Override
    public void onFoundNewDevice(BluetoothDevice device) {
        if(!isPaired(device)) {
            Log.i(TAG,"New Device found :"+device.getName());
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            BluetoothItem bluetoothItem = new BluetoothItem(deviceName, deviceHardwareAddress, BluetoothListAdapter.TYPE_ITEM);
            bluetoothDeviceList.removeAll(unpairedList);
            unpairedList.add(bluetoothItem);

            bluetoothDeviceList.addAll(unpairedList);

            notifyDeviceListChanged();
        }
    }

    @Override
    public void onDevicePaired(BluetoothDevice device) {
        Log.i(TAG,device.getName() + "Paired");
        removeDeviceFromList(unpairedList,device.getAddress());
        listPairedDevices();
        if(unpairedList.size()>0){
            bluetoothDeviceList.addAll(unpairedList);
        }
        notifyDeviceListChanged();
    }

    @Override
    public void onDeviceUnpaired(BluetoothDevice device) {
        Log.i(TAG,device.getName() + "Unpaired");
        removeDeviceFromList(pairedList,device.getAddress());
        unpairedList.add(new BluetoothItem(device.getAddress(),device.getName()));
        notifyDeviceListChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothDeviceList = new ArrayList<>();
        pairedList = new ArrayList<>();
        unpairedList = new ArrayList<>();

        registerReceiver();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        startBluetooth();

        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
        unregisterReceiver(mReceiver);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mService!=null){
            mService.registerCallback(BluetoothActivity.this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mService!=null){
            //mService.unregisterCallback(BluetoothActivity.this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                onMessage("Bluetooth Enabled");
                startBluetooth();
            } else{
                onMessage("Bluetooth Disabled");
                finish();
            }
        } else if(requestCode == REQUEST_START_DISCOVERIBILITY){
            if(resultCode == DISCOVERABLE_DURATION){
                onMessage("Discoverability started for 5 min");
            }
        }
    }

    private void removeDeviceFromList(List<BluetoothItem> deviceList, String mac){
        Iterator<BluetoothItem> iterator = deviceList.iterator();
        while (iterator.hasNext()){
            BluetoothItem item = iterator.next();
            if(item.getAddress().equals(mac)){
                deviceList.remove(item);
                break;
            }
        }
    }

    public void pairDevice(BluetoothDevice device) {
        //normally device will pair automatically is having same request UUID
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String msg){
        onMessage(msg);
    }

    public void startBluetooth(){
        if (mBluetoothAdapter == null) {
            //Snackbar.make(buttonScan,"Device does not support Bluetooth",Snackbar.LENGTH_SHORT).show();
            sendMessage("Device does not support Bluetooth");
        } else{
            if (!mBluetoothAdapter.isEnabled()) {
                askToEnableBluetooth();
            } else{
                startDiscoverability();
                listPairedDevices();
                //scanDevices();
                //bluetoothUtility.start();
            }
        }
    }

    public void startDiscoverability(){
        if(mBluetoothAdapter!=null) {
            if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
                startActivityForResult(discoverableIntent, REQUEST_START_DISCOVERIBILITY);
            }
        }
    }

    public void askToEnableBluetooth(){
        if (mBluetoothAdapter!=null && !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void enableBluetooth(){
        //this will need bluetooth admin permission
        if (mBluetoothAdapter!=null && !mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }
    }

    public void disableBluetooth(){
        if (mBluetoothAdapter!=null && mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
        }
    }

    private void listPairedDevices(){
        if(mBluetoothAdapter == null)
            return;
        bluetoothDeviceList.clear();
        pairedList.clear();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            pairedList.add(new BluetoothItem("Paired Devices","",BluetoothListAdapter.TYPE_TITLE));
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BluetoothItem bluetoothItem = new BluetoothItem(deviceName,deviceHardwareAddress,BluetoothListAdapter.TYPE_ITEM);
                pairedList.add(bluetoothItem);
            }
        }
        bluetoothDeviceList.addAll(pairedList);
        notifyDeviceListChanged();
    }

    public void scanDevices(){
        if(mBluetoothAdapter == null)
            return;
        listPairedDevices();
        bluetoothDeviceList.removeAll(unpairedList);
        notifyDeviceListChanged();
        unpairedList.clear();

        if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION,"");// needed in marshmellow for bluetooth discoverability
            return;
        }
        if(!isPermissionGranted(Manifest.permission.BLUETOOTH_ADMIN)){
            requestPermission(Manifest.permission.BLUETOOTH_ADMIN,"");// needed in marshmellow for bluetooth discoverability
            return;
        }
        unpairedList.add(new BluetoothItem("Available Devices","",BluetoothListAdapter.TYPE_TITLE));
        stopScan();
        if(startScan()){
            sendMessage("Scan started...");
        } else{
            sendMessage("Scan error...");
        }
    }

    public boolean isPaired(BluetoothDevice device){
        if(device.getBondState() == BluetoothDevice.BOND_BONDED){
            return true;
        } else{
            return false;
        }
    }

    public boolean startScan(){
        if(mBluetoothAdapter!= null){
            return mBluetoothAdapter.startDiscovery();
        } else{
            return false;
        }
    }

    public void stopScan(){
        if(mBluetoothAdapter!= null){
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    public BluetoothDevice getBluetoothDevice(String mac){
        if(mBluetoothAdapter == null) {
            return null;
        } else{
            return mBluetoothAdapter.getRemoteDevice(mac.toUpperCase());
        }
    }

    @Override
    public void onPermissionGranted(String permission) {
        if(unpairedList.size() == 0){
            scanDevices();
        }
    }

    @Override
    public void onPermissionDenied(String permission) {
        finish();
    }

    @Override
    public void onSuccess(String message) {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }
}
