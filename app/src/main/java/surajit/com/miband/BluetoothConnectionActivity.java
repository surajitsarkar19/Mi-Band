package surajit.com.miband;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import surajit.com.miband.bluetooth.BluetoothActivity;

public class BluetoothConnectionActivity extends BluetoothActivity implements View.OnClickListener {

    TextView textViewConnectionStatus;
    ProgressBar progressbar;
    private static String TAG = MainActivity.TAG;
    BluetoothDevice device;
    boolean bConnectionMade = false;
    private Button buttonBrowse,buttonSend;
    private ImageView imageViewIcon;
    private static int RESULT_LOAD_IMAGE = 593;
    private Bitmap imageBitmap;
    private ByteBuffer readBuffer,imageBuffer;
    private int lengthBytesRead;
    private int imageBytesRead;
    private int imageSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        textViewConnectionStatus = (TextView) findViewById(R.id.textViewConnectionStatus);
        progressbar = (ProgressBar) findViewById(R.id.progressbar);
        imageViewIcon = (ImageView) findViewById(R.id.imageViewIcon);
        buttonBrowse = (Button) findViewById(R.id.buttonBrowse);
        buttonSend = (Button) findViewById(R.id.buttonSend);

        buttonBrowse.setOnClickListener(this);
        buttonSend.setOnClickListener(this);

        readBuffer = ByteBuffer.allocate(1024);
        lengthBytesRead = 0;
        imageBytesRead = 0;
        imageSize = 0;

        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,"");

        String statusMsg;
        String mac = getIntent().getStringExtra("mac");
        if(mac!=null) {
            device = getBluetoothDevice(mac);
            connectRemoteDevice();

            statusMsg = "Connecting to " + device.getName();
            setStatusMessage(statusMsg, true);
        } else{
            BluetoothDevice device1 = getIntent().getParcelableExtra("device");
            statusMsg = "Connected to " + device1.getName();
            setStatusMessage(statusMsg, false);
        }
    }

    private void setStatusMessage(String msg, boolean bShowProgress){
        if(msg!=null){
            textViewConnectionStatus.setText(msg);
        }
        if(bShowProgress){
            progressbar.setVisibility(View.VISIBLE);
        } else{
            progressbar.setVisibility(View.GONE);
        }
    }

    private synchronized void connectRemoteDevice(){
        if(!bConnectionMade){
            if(device!=null && mService!=null) {
                bConnectionMade = true;
                mService.connect(device);
            }
        }
    }

    @Override
    public void onMessage(String message) {

    }

    @Override
    public void onDiscoveryStarted() {

    }

    @Override
    public void onDiscoveryStopped() {

    }

    @Override
    protected void onBluetoothServiceConnected() {
        Log.i(TAG,"Service Connected");
        connectRemoteDevice();
    }

    @Override
    protected void notifyDeviceListChanged() {

    }

    @Override
    public void onConnect(BluetoothDevice device) {
        Log.i(TAG,"Connected .. "+device);
        setStatusMessage("Connected .. "+device,false);
    }

    @Override
    public void onRead(int nRead, byte[] data) {
        int offset = 0;
        if(lengthBytesRead<8){

            int remainingBytes = 8 - lengthBytesRead;
            if(nRead>remainingBytes){
                lengthBytesRead += remainingBytes;
                offset = remainingBytes;
            } else{
                lengthBytesRead+=nRead;
                offset = nRead;
            }

            if(lengthBytesRead == 8){
                imageSize = readBuffer.getInt();
                imageBuffer = ByteBuffer.allocate(imageSize);
            }

        }
        if(offset != nRead){
            imageBuffer.put(data,offset,nRead-offset);
            if(imageBuffer.array().length == imageSize){
                displayImage();
            }
        }
    }

    private void displayImage(){
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBuffer.array(),0,imageSize);
        if(bitmap!=null){
            imageViewIcon.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onError(String message) {
        //setStatusMessage(message, false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(mBound && mService!=null){
            mService.stop();
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.buttonSend){
            sendPicture(imageBitmap);
        } else if(v.getId() == R.id.buttonBrowse){
            Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    }

    private void sendPicture(Bitmap bitmap){
        if(mService!=null){
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100,baos); //bm is the bitmap object
            byte[] buffer = baos.toByteArray();
            ByteBuffer imageLength = ByteBuffer.allocate(8);
            imageLength.putInt(buffer.length);
            mService.write(imageLength.array());
            mService.write(buffer);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            ImageView imageView = (ImageView) findViewById(R.id.imageViewIcon);
            imageBitmap  = BitmapFactory.decodeFile(picturePath);
            imageView.setImageBitmap(imageBitmap);
        }
    }
}
