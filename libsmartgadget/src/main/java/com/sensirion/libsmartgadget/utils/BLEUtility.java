package com.sensirion.libsmartgadget.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

public final class BLEUtility {
    private static final String TAG = BLEUtility.class.getSimpleName();

    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private BLEUtility() {
    }

    /**
     * Checks if BLE connections are supported and if Bluetooth is enabled on the device.
     *
     * @return <code>true</code> if it's enabled - <code>false</code> otherwise.
     */
    public static boolean isBLEEnabled(@NonNull final Context context) {
        // Use this check to determine whether BLE is supported on the device.
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Bluetooth LE is not supported on this device");
            return false;
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return false;
        }

        return bluetoothAdapter.isEnabled();
    }

    /**
     * Runtime request for ACCESS_FINE_LOCATION. This is required on Android 6.0 and higher in order
     * to perform BLE scans.
     *
     * @param requestingActivity The activity requesting the permission.
     * @param requestCode        The request code used to deliver the user feedback to the calling
     *                           activity.
     */
    public static void requestScanningPermission(@NonNull final Activity requestingActivity,
                                                 final int requestCode) {
        if (!hasScanningPermission(requestingActivity)) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(requestingActivity, LOCATION_PERMISSION)) {
                ActivityCompat.requestPermissions(requestingActivity, new String[]{LOCATION_PERMISSION}, requestCode);
            }
        }
    }

    /**
     * Checks if a context has scanning permission
     *
     * @param context The context of which the user wants to know if it has scanning permission
     * @return        <code>true</code> if the context has scanning permission - <code>false</code> otherwise.
     */
    public static boolean hasScanningPermission(@NonNull final Context context) {
        return ContextCompat.checkSelfPermission(context, LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the user to enable bluetooth in case it's disabled.
     *
     * @param context {@link android.content.Context} of the requesting activity.
     */
    public static void requestEnableBluetooth(@NonNull final Context context) {
        if (!isBLEEnabled(context)) {
            final Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            try {
                context.startActivity(enableBluetoothIntent);
            } catch (SecurityException e) {
            }    
        }
    }
}
