package surajit.com.miband;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private String imagePath;
    private ByteBuffer headerBuffer;
    private int headerBytesRead;
    private long imageBytesRead;
    private long imageSize;
    int headerSize;
    private File tempFile;
    private BufferedOutputStream fileOutputStream;
    private ProgressBar progressbarData;
    private TextView textViewData;
    private Handler handler;
    private int headerOffset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);

        textViewConnectionStatus = (TextView) findViewById(R.id.textViewConnectionStatus);
        progressbar = (ProgressBar) findViewById(R.id.progressbar);
        imageViewIcon = (ImageView) findViewById(R.id.imageViewIcon);
        buttonBrowse = (Button) findViewById(R.id.buttonBrowse);
        buttonSend = (Button) findViewById(R.id.buttonSend);
        progressbarData = (ProgressBar) findViewById(R.id.progressbarData);
        textViewData = (TextView) findViewById(R.id.textViewData);

        buttonBrowse.setOnClickListener(this);
        buttonSend.setOnClickListener(this);
        handler = new Handler();

        resetData();

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

    private void showProgress(){
        showProgress(imageBytesRead,imageSize);
    }

    private void showProgress(final long progressVal, final long dataVal){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                textViewData.setText(""+progressVal);
                double data = (double)progressVal/dataVal;
                int progress = (int)(data*100);
                progressbarData.setProgress(progress);
            }
        },100);

    }


    private void resetData(){
        headerSize = 8;
        if(headerBuffer!=null){
            headerBuffer.clear();
            headerBuffer.put(new byte[]{0,0,0,0,0,0,0,0});
            headerBuffer.clear();
        } else {
            headerBuffer = ByteBuffer.allocate(headerSize);
        }
        headerBytesRead = 0;
        imageBytesRead = 0;
        imageSize = 0;
        //progressbar.setProgress(0);
        //textViewData.setText("");
    }

    private void setStatusMessage(final String msg, final boolean bShowProgress){
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(msg!=null){
                    textViewConnectionStatus.setText(msg);
                }
                if(bShowProgress){
                    progressbar.setVisibility(View.VISIBLE);
                } else{
                    progressbar.setVisibility(View.GONE);
                }
            }
        });
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

    private boolean readHeader(int nRead, byte[] data) throws IOException {
        boolean bHeaderReadCompleted = false;
        headerOffset = 0;
        if (headerBytesRead < headerSize) {
            Log.i(TAG,"header data is not received yet");
            int remainingBytes = headerSize - headerBytesRead;
            int headerOffset = headerBytesRead;
            if (nRead > remainingBytes) {
                headerBytesRead += remainingBytes;
                this.headerOffset = remainingBytes;
            } else {
                headerBytesRead += nRead;
                this.headerOffset = nRead;
            }

            headerBuffer.put(data, headerOffset, remainingBytes);


            if(headerBytesRead < headerSize){
                bHeaderReadCompleted = false;
            }
            else if (headerBytesRead == headerSize) {
                imageSize = headerBuffer.getLong(0);
                tempFile = new File(Environment.getExternalStorageDirectory(),"srs_img.png");
                boolean fileExists = tempFile.createNewFile();
                fileOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
                setStatusMessage("Receiving img size "+imageSize,false);

                Log.i(TAG,"Image Size  = "+imageSize);

                if(nRead - this.headerOffset <= 0){
                    bHeaderReadCompleted = false;
                }else{
                    bHeaderReadCompleted = true;
                }
            } else{
                bHeaderReadCompleted = false;
            }

        } else{
            Log.i(TAG,"header data received...");
            bHeaderReadCompleted = true;
        }
        return bHeaderReadCompleted;
    }

    @Override
    public void onRead(int nRead, byte[] data) {

        try {

            if(!readHeader(nRead,data)){
                return; //if header read is not complete return
            }

            //Log.i(TAG,"Read size "+nRead+" Total Received length "+imageBytesRead);


            int dataLength = nRead;
            if(headerOffset >0 && headerBytesRead == headerSize){
                dataLength = nRead - headerOffset;
                Log.i(TAG,"Read size "+nRead+" Data size "+dataLength+" Total Received length "+imageBytesRead);
                fileOutputStream.write(data, (int)imageBytesRead, dataLength);
                fileOutputStream.flush();
            } else {
                Log.i(TAG,"Read size "+dataLength+" Total Received length "+imageBytesRead);
                fileOutputStream.write(data,0,dataLength);
                fileOutputStream.flush();
            }
            imageBytesRead += dataLength;

            showProgress();

            if (imageSize>0 && imageBytesRead == imageSize) {
                fileOutputStream.close();
                imagePath = tempFile.getAbsolutePath();
                resetData();
                displayImage();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSent(int nSent, byte[] data) {
        //imageBytesRead+=nSent;
        //showProgress();
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
            new AsyncTask<Object,Object,Object>(){
                File file;
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    progressbar.setProgress(0);
                    textViewData.setText("");
                }

                @Override
                protected Object doInBackground(Object[] params) {
                    file = new File(imagePath);
                    if(file.isFile()){
                        try {
                            long fileLength = file.length();
                            long progress=0;
                            byte[] length = ByteBuffer.allocate(8).putLong(fileLength).array();
                            mService.write(length);
                            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                            byte[] buffer = new byte[1024];
                            int count = 0;
                            //BufferedOutputStream ff = new BufferedOutputStream(new FileOutputStream(new File(file.getParent(),"srs.png")));
                            while((count = inputStream.read(buffer))>0){
                                mService.write(buffer,0,count);
                                //ff.write(buffer,0,count);
                                progress+=count;
                                showProgress(progress,fileLength);
                            }
                            //ff.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Object o) {
                    super.onPostExecute(o);
                    setStatusMessage("Image length = "+file.length(),false);
                }
            }.execute();

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
