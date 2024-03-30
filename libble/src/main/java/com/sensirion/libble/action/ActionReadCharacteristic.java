package com.sensirion.libble.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public class ActionReadCharacteristic extends GattAction {
    private final BluetoothGattCharacteristic mCharacteristic;

    public ActionReadCharacteristic(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
        super(gatt);
        mCharacteristic = characteristic;
    }

    @Override
    boolean execute() {
        try {
            return mGatt.readCharacteristic(mCharacteristic);
        } catch (SecurityException e) {
            return false;
        }    
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;
    }
}
