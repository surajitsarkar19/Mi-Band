package surajit.com.miband;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static android.bluetooth.BluetoothDevice.EXTRA_BOND_STATE;

public class MainActivity extends PermissionActivity implements View.OnClickListener, BluetoothServerThread.BluetoothServerConnectionListener, AdapterView.OnItemClickListener, BluetoothClientConnectionThread.BluetoothClientConnectionListener {

    private static int REQUEST_ENABLE_BT = 345;
    private static int REQUEST_START_DISCOVERIBILITY = 267;
    private static int DISCOVERABLE_DURATION = 5*60;
    private Button buttonScan;
    private ListView listViewBluetooth;
    private BluetoothListAdapter arrayAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    List<BluetoothItem> bluetoothDeviceList;
    List<BluetoothItem> pairedList;
    List<BluetoothItem> unpairedList;
    ObjectAnimator scanAnimation;
    BluetoothServerThread bluetoothServerThread;
    BluetoothClientConnectionThread bluetoothClientConnectionThread;

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                bluetoothDeviceList.removeAll(unpairedList);
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BluetoothItem bluetoothItem = new BluetoothItem(deviceName,deviceHardwareAddress,BluetoothListAdapter.TYPE_ITEM);
                unpairedList.add(bluetoothItem);

                bluetoothDeviceList.addAll(unpairedList);
                arrayAdapter.notifyDataSetChanged();
            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)){
                scanAnimation.start();

            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                resetScanButton();
            } else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,0);
                if(bondState == BluetoothDevice.BOND_BONDED) {
                    removeDeviceFromList(unpairedList,device.getAddress());
                    updatePairedDevices();
                } else if(bondState == BluetoothDevice.BOND_NONE){
                    removeDeviceFromList(pairedList,device.getAddress());
                    unpairedList.add(new BluetoothItem(device.getAddress(),device.getName()));
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonScan = (Button) findViewById(R.id.buttonScan);
        listViewBluetooth = (ListView) findViewById(R.id.listViewBluetooth);
        bluetoothDeviceList = new ArrayList<>();
        pairedList = new ArrayList<>();
        unpairedList = new ArrayList<>();
        arrayAdapter = new BluetoothListAdapter(this, bluetoothDeviceList);
        listViewBluetooth.setAdapter(arrayAdapter);
        buttonScan.setOnClickListener(this);
        listViewBluetooth.setOnItemClickListener(this);

        setUpAnimation();
        registerReceiver();
        startBluetooth();

    }

    private void pairDevice(BluetoothDevice device) {
        //normally device will pair automatically is having same request UUID
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setUpAnimation(){
        scanAnimation = ObjectAnimator.ofInt(buttonScan, "textColor", Color.RED, Color.TRANSPARENT);
        scanAnimation.setDuration(1000);
        scanAnimation.setEvaluator(new ArgbEvaluator());
        scanAnimation.setRepeatCount(ValueAnimator.INFINITE);
        scanAnimation.setRepeatMode(ValueAnimator.REVERSE);
    }

    private void registerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver,intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        stopBluetooth();
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

    private void startBluetooth(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Snackbar.make(buttonScan,"Device does not support Bluetooth",Snackbar.LENGTH_SHORT).show();
        } else{
            if (!mBluetoothAdapter.isEnabled()) {
                askToEnableBluetooth();
            } else{
                startDiscoverability();
                listPairedDevices();
                scanDevices();
                startBluetoothServer();
            }
        }
    }

    private void stopBluetooth(){
        if(mBluetoothAdapter!=null){
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
            }
            stopBluetoothServer();
        }
    }

    private void startDiscoverability(){
        if(mBluetoothAdapter!=null) {
            if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
                startActivityForResult(discoverableIntent, REQUEST_START_DISCOVERIBILITY);
            }
        }
    }

    private void stopBluetoothServer(){
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

    private void startBluetoothServer(){
        stopBluetoothServer();
        bluetoothServerThread = new BluetoothServerThread(mBluetoothAdapter, this);
        bluetoothServerThread.start();
    }

    private void askToEnableBluetooth(){
        if (mBluetoothAdapter!=null && !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void enableBluetooth(){
        //this will need bluetooth admin permission
        if (mBluetoothAdapter!=null && !mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.enable();
        }
    }

    private void disableBluetooth(){
        if (mBluetoothAdapter!=null && mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT){
            if(resultCode == RESULT_OK){
                Snackbar.make(buttonScan,"Bluetooth Enabled",Snackbar.LENGTH_SHORT).show();
                startBluetooth();
            } else{
                Snackbar.make(buttonScan,"Bluetooth disabled",Snackbar.LENGTH_SHORT).show();
                finish();
            }
        } else if(requestCode == REQUEST_START_DISCOVERIBILITY){
            if(resultCode == DISCOVERABLE_DURATION){
                Snackbar.make(buttonScan,"Discoverability started for 5 min",Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void updatePairedDevices(){
        listPairedDevices();
        if(unpairedList.size()>0){
            bluetoothDeviceList.addAll(unpairedList);
        }
        arrayAdapter.notifyDataSetChanged();
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
        arrayAdapter.notifyDataSetChanged();
    }

    private void scanDevices(){
        if(mBluetoothAdapter == null)
            return;
        listPairedDevices();
        bluetoothDeviceList.removeAll(unpairedList);
        arrayAdapter.notifyDataSetChanged();
        unpairedList.clear();

        if(!isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)){
            requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION,"");// needed in marshmellow for bluetooth discoverability
            return;
        }
        buttonScan.setText("STOP");
        unpairedList.add(new BluetoothItem("Available Devices","",BluetoothListAdapter.TYPE_TITLE));
        mBluetoothAdapter.cancelDiscovery();
        if(mBluetoothAdapter.startDiscovery()){
            //Toast.makeText(this,"Discovery started", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetScanButton(){
        scanAnimation.cancel();
        buttonScan.setText("SCAN");
        buttonScan.setTextColor(Color.BLACK);
    }

    private boolean isPaired(BluetoothDevice device){
        if(device.getBondState() == BluetoothDevice.BOND_BONDED){
            return true;
        } else{
            return false;
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.buttonScan){
            if(buttonScan.getText().toString().equalsIgnoreCase("SCAN")) {
                scanDevices();
            } else{
                mBluetoothAdapter.cancelDiscovery();
                resetScanButton();
            }
        }
    }

    @Override
    public void onAcceptBluetoothConnection(BluetoothSocket socket) {

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        connecTo(bluetoothDeviceList.get(position).getAddress());
    }

    private void cancelBluetoothclientConnectionThread(BluetoothClientConnectionThread bluetoothClientConnectionThread){
        if(bluetoothClientConnectionThread!=null){
            if(bluetoothClientConnectionThread.isRunning()){
                bluetoothClientConnectionThread.cancel();
                try {
                    bluetoothClientConnectionThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                bluetoothClientConnectionThread = null;
            }
        }
    }

    private void connecTo(String mac){
        if(mBluetoothAdapter == null)
            return;
        mBluetoothAdapter.cancelDiscovery();

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac.toUpperCase());
        if(isPaired(device)){
            cancelBluetoothclientConnectionThread(bluetoothClientConnectionThread);
            bluetoothClientConnectionThread = null;

            bluetoothClientConnectionThread = new BluetoothClientConnectionThread(mBluetoothAdapter, device, MainActivity.this);
            bluetoothClientConnectionThread.start();
        } else{
            pairDevice(device);
        }

    }

    @Override
    public void onClientConnected(BluetoothSocket socket) {

    }
}
