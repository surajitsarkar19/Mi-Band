package surajit.com.miband;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Surajit Sarkar on 23/12/16.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public abstract class PermissionActivity extends AppCompatActivity {

    private static int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 355;
    private static int REQUEST_CODE_ASK_INDIVIDUAL_PERMISSION = 522;
    private Map<String,ManifestPermission> permissionMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionMap = new HashMap<>();
    }

    public void showMessageOKCancel(String message,
                                    DialogInterface.OnClickListener okListener,
                                    DialogInterface.OnClickListener cancelListener) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setPositiveButton("OK", okListener);
        builder.setNegativeButton("Cancel", cancelListener);
        builder.create();
        builder.setCancelable(false);
        builder.show();
    }

    public boolean isPermissionGranted(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean shouldShowRational(String permission) {
        if (!isPermissionGranted(permission)) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
            else{
                return false;
            }
        } else {
            return false;
        }
    }

    private List<String> getRationalList(String[] permissions){
        List<String> permissionList = new ArrayList<>();
        //check for each permission is granted or not
        for(String permission:permissions){
            if(shouldShowRational(permission)){
                permissionList.add(permission);
            }
        }
        return permissionList;
    }

    private List<String> getPermissionList(String[] permissions){
        List<String> permissionList = new ArrayList<>();
        //check for each permission is granted or not
        for(String permission:permissions){
            if(!shouldShowRational(permission)){
                permissionList.add(permission);
            }
        }
        return permissionList;
    }

    private void addPermission(ManifestPermission manifestPermission){
        if(!isPermissionGranted(manifestPermission.getPermissionName())) {
            if (shouldShowRational(manifestPermission.getPermissionName())) {
                manifestPermission.setRationalStatus(true);
            }
        } else {
            manifestPermission.setGranted(true);
        }
        permissionMap.put(manifestPermission.getPermissionName(),manifestPermission);
    }

    private void addPermission(String permission,String rational){
        ManifestPermission manifestPermission = new ManifestPermission(permission,rational);
        addPermission(manifestPermission);
    }

    private String[] getPermissionArray(){
        List<String> permissionList = new ArrayList<>();
        Set entrySet = permissionMap.entrySet();
        Iterator<Map.Entry> iterator = entrySet.iterator();
        while (iterator.hasNext()){
            Map.Entry<String,ManifestPermission> entry = iterator.next();
            ManifestPermission manifestPermission = entry.getValue();
            if(!manifestPermission.isGranted() && !manifestPermission.shouldShowRational()){
                permissionList.add(manifestPermission.getPermissionName());
            }
        }
        return permissionList.toArray(new String[permissionList.size()]);
    }

    private List<ManifestPermission> getRationalList(){
        List<ManifestPermission> permissionList = new ArrayList<>();
        Set entrySet = permissionMap.entrySet();
        Iterator<Map.Entry> iterator = entrySet.iterator();
        while (iterator.hasNext()){
            Map.Entry<String,ManifestPermission> entry = iterator.next();
            ManifestPermission manifestPermission = entry.getValue();
            if(!manifestPermission.isGranted() && manifestPermission.shouldShowRational()){
                permissionList.add(manifestPermission);
            }
        }
        return permissionList;
    }

    public void requestPermission(String[] permissions, String[] rationals) {
        if(permissions!=null && rationals!=null && permissions.length == rationals.length){
            for(int i=0;i<permissions.length;i++) {
                //requestPermission(permissions[i], rationals[i]);
                addPermission(permissions[i], rationals[i]);
            }
        }
        String[] permissionArray = getPermissionArray();
        List<ManifestPermission> rationalList = getRationalList();
        for(ManifestPermission manifestPermission:rationalList){
            requestPermission(manifestPermission);
        }
        requestPermission(permissionArray, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
    }

    public void requestPermission(String permission, String rational) {
        addPermission(permission, rational);
        ManifestPermission manifestPermission = permissionMap.get(permission);
        if(!manifestPermission.isGranted()) {
            requestPermission(manifestPermission);
        }
    }

    private void requestPermission(final ManifestPermission manifestPermission) {
        /*if(manifestPermission.shouldShowRational()){
            showMessageOKCancel(manifestPermission.getPermissionRational(),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission(
                                    new String[]{manifestPermission.getPermissionName()},
                                    REQUEST_CODE_ASK_INDIVIDUAL_PERMISSION);
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onPermissionDenied(manifestPermission.getPermissionName());
                        }
                    }
            );
        } else {
            requestPermission(
                    new String[]{manifestPermission.getPermissionName()},
                    REQUEST_CODE_ASK_INDIVIDUAL_PERMISSION);
        }*/
        requestPermission(
                new String[]{manifestPermission.getPermissionName()},
                REQUEST_CODE_ASK_INDIVIDUAL_PERMISSION);
    }

    private void requestPermission(String[] permissions, int requestCode){
        if(permissions!=null && permissions.length>0) {
            ActivityCompat.requestPermissions(PermissionActivity.this, permissions, requestCode);
        }
    }

    public abstract void onPermissionGranted(String permission);
    public abstract void onPermissionDenied(String permission);


    private void closeActivity(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //only accept my request codes
        if(permissions!=null && permissions.length>0) {
            if (requestCode == REQUEST_CODE_ASK_INDIVIDUAL_PERMISSION) {
                notifyPermissionResult(permissions[0], grantResults[0]);
            } else if (requestCode == REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS) {
                if (permissions.length == grantResults.length) {
                    for (int i = 0; i < permissions.length; i++) {
                        notifyPermissionResult(permissions[i], grantResults[i]);
                    }
                }
            }
        }
    }

    private void notifyPermissionResult(String permission, int result){
        if (result == PackageManager.PERMISSION_DENIED) {
            onPermissionDenied(permission);
        } else {
            onPermissionGranted(permission);
        }
    }

    public static class ManifestPermission{
        private String permissionName;
        private String permissionRational;
        private boolean bShowRational;
        private boolean bGranted;

        public ManifestPermission(String permissionName, String permissionRational) {
            this.permissionName = permissionName;
            this.permissionRational = permissionRational;
            this.bShowRational = false;
            this.bGranted = false;
        }

        public void setGranted(boolean status){
            this.bGranted = status;
        }

        public boolean isGranted(){
            return bGranted;
        }

        public boolean shouldShowRational(){
            return bShowRational;
        }

        public void setRationalStatus(boolean status){
            bShowRational = status;
        }

        public String getPermissionName() {
            return permissionName;
        }

        public void setPermissionName(String permissionName) {
            this.permissionName = permissionName;
        }

        public String getPermissionRational() {
            return permissionRational;
        }

        public void setPermissionRational(String permissionRational) {
            this.permissionRational = permissionRational;
        }
    }
}
