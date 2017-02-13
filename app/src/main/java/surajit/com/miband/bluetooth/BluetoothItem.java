package surajit.com.miband.bluetooth;

/**
 * Created by Surajit Sarkar on 2/2/17.
 * Company : Bitcanny Technologies Pvt. Ltd.
 * Email   : surajit@bitcanny.com
 */

public class BluetoothItem {
    String name,address;
    int type; //item type

    public BluetoothItem(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public BluetoothItem(String name, String address, int type) {
        this.name = name;
        this.address = address;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
