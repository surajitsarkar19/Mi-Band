package surajit.com.miband;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
    private String imagePath;
    private ByteBuffer headerBuffer;
    private int lengthBytesRead;
    private long imageBytesRead;
    private long imageSize;
    int headerSize;
    private File tempFile;
    private FileOutputStream fileOutputStream;

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

        headerSize = 8;
        headerBuffer = ByteBuffer.allocate(headerSize);
        lengthBytesRead = 0;
        imageBytesRead = 0;
        imageSize = 0;

        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,"");

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
    public synchronized void onRead(int nRead, byte[] data) {

        try {
            int offset = 0;
            if (lengthBytesRead < headerSize) {
                Log.i(TAG,"header data is not received yet");
                int remainingBytes = headerSize - lengthBytesRead;
                int headerOffset = lengthBytesRead;
                if (nRead > remainingBytes) {
                    lengthBytesRead += remainingBytes;
                    offset = remainingBytes;
                } else {
                    lengthBytesRead += nRead;
                    offset = nRead;
                }

                headerBuffer.put(data, headerOffset, remainingBytes);


                if(lengthBytesRead < headerSize){
                    return;
                }
                else if (lengthBytesRead == headerSize) {
                    imageSize = headerBuffer.getLong(0);
                    //imageBuffer = ByteBuffer.allocate(imageSize*2);
                    tempFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+File.separator+"srs_img.png");
                    tempFile.mkdirs();
                    if(!tempFile.exists()){
                        tempFile.createNewFile();
                    }
                    fileOutputStream = new FileOutputStream(tempFile,true);
                    setStatusMessage("Receiving img size "+imageSize,false);

                    Log.i(TAG,"Image Size  = "+imageSize);

                    if(nRead - offset <= 0){
                        return; // no data to read
                    }
                } else{
                    return;
                }

            } else{
                Log.i(TAG,"header data found...");
            }

            //Log.i(TAG,"Read size "+nRead+" Total Received length "+imageBytesRead);


            int dataLength = nRead;
            if(offset>0 && lengthBytesRead == headerSize){
                dataLength = nRead - offset;
                Log.i(TAG,"Read size "+nRead+" Data size "+dataLength+" Total Received length "+imageBytesRead);
                fileOutputStream.write(data, (int)imageBytesRead, dataLength);
                fileOutputStream.flush();
            } else {
                Log.i(TAG,"Read size "+dataLength+" Total Received length "+imageBytesRead);
                fileOutputStream.write(data);
                fileOutputStream.flush();
            }
            imageBytesRead += dataLength;
            if (imageSize>0 && imageBytesRead == imageSize) {
                fileOutputStream.close();
                imagePath = tempFile.getAbsolutePath();
                lengthBytesRead = 0;
                imageBytesRead = 0;
                displayImage();
                imageSize = 0;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void displayImage(){
        try {
            Bitmap imageBitmap  = BitmapFactory.decodeFile(imagePath);
            if(imageBitmap!=null){
                imageViewIcon.setImageBitmap(imageBitmap);
            }
            headerBuffer.clear();
        } catch (Exception e) {
            e.printStackTrace();
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
            sendPicture();
        } else if(v.getId() == R.id.buttonBrowse){
            Intent i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
        }
    }

    private void sendPicture(){
        if(mService!=null){
            File file = new File(imagePath);
            if(file.isFile()){
                try {
                    byte[] length = ByteBuffer.allocate(8).putLong(file.length()).array();
                    mService.write(length);
                    FileInputStream inputStream = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    while(inputStream.read(buffer)!=-1){
                        mService.write(buffer);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            setStatusMessage("Image length = "+file.length(),false);

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
            imagePath = cursor.getString(columnIndex);
            cursor.close();
            displayImage();
        }
    }
}
