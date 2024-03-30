package com.sensirion.libsmartgadget.smartgadget;

import androidx.annotation.NonNull;

import com.sensirion.libsmartgadget.GadgetDownloadService;
import com.sensirion.libsmartgadget.GadgetService;
import com.sensirion.libsmartgadget.GadgetValue;

public interface ServiceListener {
    void onGadgetValuesReceived(@NonNull GadgetService service, @NonNull GadgetValue[] values);

    void onGadgetDownloadDataReceived(@NonNull GadgetDownloadService service, @NonNull GadgetValue[] values, int progress);

    void onDownloadFailed(@NonNull GadgetDownloadService service);

    void onDownloadCompleted(@NonNull GadgetDownloadService service);

    void onDownloadNoData(@NonNull GadgetDownloadService service);

    void onSetGadgetLoggingEnabledFailed(@NonNull GadgetDownloadService service);

    void onSetLoggerIntervalFailed(@NonNull GadgetDownloadService service);

    void onSetLoggerIntervalSuccess();

}
