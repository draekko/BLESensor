package com.sensirion.libsmartgadget.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

public interface BleConnector {

    boolean connect(SmartGadget gadget);

    void disconnect(SmartGadget gadget);

    @NonNull
    List<BluetoothGattService> getServices(SmartGadget gadget);

    @NonNull
    Map<String, BluetoothGattCharacteristic> getCharacteristics(@NonNull final String deviceAddress,
                                                                final List<String> uuids);

    void readCharacteristic(@NonNull final String deviceAddress, final String characteristicUuid);

    void writeCharacteristic(@NonNull final String deviceAddress,
                             final BluetoothGattCharacteristic characteristic);

    void setCharacteristicNotification(@NonNull final String deviceAddress,
                                       final BluetoothGattCharacteristic characteristic,
                                       final BluetoothGattDescriptor descriptor,
                                       final boolean enabled);
}
