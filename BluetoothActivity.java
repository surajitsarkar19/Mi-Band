package surajit.com.miband.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import surajit.com.miband.MainActivity;
import surajit.com.miband.PermissionActivity;

/**
 * Created by Surajit Sarkar on 10/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public abstract class BluetoothActivity extends PermissionActivity implements BluetoothServiceListener, BluetoothServerThread.BluetoothServerConnectionListener, BluetoothClientConnectionThread.BluetoothClientConnectionListener {

    public static int REQUEST_ENABLE_BT = 345;
    public static int REQUEST_START_DISCOVERIBILITY = 267;
    public static int DISCOVERABLE_DURATION = 5*60;

    private Handler handler;
    protected List<BluetoothItem> bluetoothDeviceList;
    protected List<BluetoothItem> pairedList;
    protected List<BluetoothItem> unpairedList;
    protected BluetoothAdapter mBluetoothAdapter;

    private static String TAG = MainActivity.TAG;

    //private Map<String, BluetoothConnectionThread> activeConnectionThreads; //active open bluetooth sockets
    //private Map<String, BluetoothClientConnectionThread> clientConnectionThreads; //active client connection threads;
    //private BluetoothServerThread bluetoothServerThread;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);//it will get all available devices
                if(!isPaired(device)) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    BluetoothItem bluetoothItem = new BluetoothItem(deviceName, deviceHardwareAddress, BluetoothListAdapter.TYPE_ITEM);
                    bluetoothDeviceList.removeAll(unpairedList);
                    unpairedList.add(bluetoothItem);

                    bluetoothDeviceList.addAll(unpairedList);

                    onFoundNewDevice(device);
                }
                //arrayAdapter.notifyDataSetChanged();
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                //scanAnimation.start();
                onDiscoveryStarted();

            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                //resetScanButton();
                onDiscoveryStopped();

            } else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,0);
                if(bondState == BluetoothDevice.BOND_BONDED) {
                    removeDeviceFromList(unpairedList,device.getAddress());
                    listPairedDevices();
                    if(unpairedList.size()>0){
                        bluetoothDeviceList.addAll(unpairedList);
                    }
                    onDevicePaired(device);
                } else if(bondState == BluetoothDevice.BOND_NONE){
                    removeDeviceFromList(pairedList,device.getAddress());
                    unpairedList.add(new BluetoothItem(device.getAddress(),device.getName()));
                    //arrayAdapter.notifyDataSetChanged();
                    onDeviceUnpaired(device);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerReceiver();

        bluetoothDeviceList = new ArrayList<>();
        pairedList = new ArrayList<>();
        unpairedList = new ArrayList<>();
        handler = new Handler();

        activeConnectionThreads = new HashMap();
        clientConnectionThreads = new HashMap();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //startBluetooth();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        stopBluetooth();
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

    private void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver,intentFilter);
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
                //listPairedDevices();
                scanDevices();
                startBluetoothServer();
            }
        }
    }

    public void stopBluetooth(){
        if(mBluetoothAdapter!=null){
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
            }
            stopBluetoothServer();
            stopAllActiveConnectionThreads();
            stopAllClientConnectionThreads();
            mBluetoothAdapter = null;
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

    public void stopActiveConnectionThread(String id){
        BluetoothConnectionThread thread = activeConnectionThreads.get(id);
        thread.cancel();
        activeConnectionThreads.remove(id);
    }

    public void stopClientConnectionThread(String id){
        BluetoothClientConnectionThread thread = clientConnectionThreads.get(id);
        thread.cancel();
        clientConnectionThreads.remove(id);
    }

    public void stopAllActiveConnectionThreads(){
        Set<String> set = activeConnectionThreads.keySet();
        for (String id : set) {
            BluetoothConnectionThread thread = activeConnectionThreads.get(id);
            if(thread.isRunning()) {
                thread.cancel();
            }
        }
        activeConnectionThreads.clear();
    }

    public void stopAllClientConnectionThreads(){
        Set<String> set = clientConnectionThreads.keySet();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()){
            String id = iterator.next();
            BluetoothClientConnectionThread thread = clientConnectionThreads.get(id);
            if(thread.isRunning()) {
                thread.cancel();
            }
        }
        clientConnectionThreads.clear();
    }

    public void stopBluetoothServer(){
        if(bluetoothServerThread!=null){
            bluetoothServerThread.cancel();
            /*try {
                bluetoothServerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                bluetoothServerThread = null;
            }*/
        }
    }

    public void startBluetoothServer(){
        stopBluetoothServer();
        bluetoothServerThread = new BluetoothServerThread(mBluetoothAdapter, this);
        bluetoothServerThread.start();
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
        onFoundNewDevice(null);
    }

    public void scanDevices(){
        if(mBluetoothAdapter == null)
            return;
        listPairedDevices();
        bluetoothDeviceList.removeAll(unpairedList);
        onFoundNewDevice(null);
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
        stopDiscovery();
        if(startDiscovery()){
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

    public boolean isPaired(String mac){
        if(mBluetoothAdapter!=null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac.toUpperCase());
            return isPaired(device);
        } else{
            return false;
        }
    }

    public boolean startDiscovery(){
        if(mBluetoothAdapter!= null){
            return mBluetoothAdapter.startDiscovery();
        } else{
            return false;
        }
    }

    public void stopDiscovery(){
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

    public void connecTo(BluetoothDevice device){
        if(mBluetoothAdapter == null)
            return;
        mBluetoothAdapter.cancelDiscovery();

        BluetoothClientConnectionThread bluetoothClientConnectionThread = new BluetoothClientConnectionThread(mBluetoothAdapter, device, BluetoothActivity.this);
        clientConnectionThreads.put(device.getAddress(),bluetoothClientConnectionThread);
        bluetoothClientConnectionThread.start();

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
    public void onAcceptBluetoothConnection(BluetoothSocket socket) {
        onAccept(socket);
    }

    @Override
    public void onClientConnected(BluetoothSocket socket) {
        onConnect(socket);
    }
}
