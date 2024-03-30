package com.sensirion.libble.action;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattDescriptor;

public class ActionWriteDescriptor extends GattAction {
    private final BluetoothGattDescriptor mGattDescriptor;

    public ActionWriteDescriptor(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor) {
        super(gatt);
        mGattDescriptor = descriptor;
    }

    @Override
    boolean execute() {
        try {
            return mGatt.writeDescriptor(mGattDescriptor);
        } catch (SecurityException e) {
            return false;
        }    
    }

    public BluetoothGattDescriptor getGattDescriptor() {
        return mGattDescriptor;
    }
}
