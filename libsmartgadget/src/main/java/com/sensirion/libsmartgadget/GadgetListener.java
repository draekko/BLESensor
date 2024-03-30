package com.sensirion.libsmartgadget;

import androidx.annotation.NonNull;

public interface GadgetListener {

    /**
     * Callback reporting that the gadget connection and service discovery initiated by the
     * {@link Gadget#connect()} function was successful.
     *
     * @param gadget The gadget the connection was established to.
     */
    void onGadgetConnected(@NonNull Gadget gadget);

    /**
     * Callback reporting that the gadget's connection got lost. This can happen even if
     * {@link Gadget#disconnect()} was not called.
     *
     * @param gadget The gadget to which the connection got lost.
     */
    void onGadgetDisconnected(@NonNull Gadget gadget);

    /**
     * Callback reporting that there were new values received for the given {@link GadgetService}.
     *
     * @param gadget  The gadget the values were sent from.
     * @param service The dedicated service.
     * @param values  the received values.
     */
    void onGadgetValuesReceived(@NonNull Gadget gadget, @NonNull GadgetService service,
                                @NonNull GadgetValue[] values);

    /**
     * Callback reporting that there were new values downloaded from the given
     * {@link GadgetDownloadService}.
     *
     * @param gadget   The gadgets from which the values are coming from.
     * @param service  The dedicated download service.
     * @param values   the received values.
     * @param progress the delivery progress in percent
     */
    void onGadgetDownloadDataReceived(@NonNull Gadget gadget,
                                      @NonNull GadgetDownloadService service,
                                      @NonNull GadgetValue[] values, int progress);

    /**
     * Callback reporting that there were new values downloaded from the given
     *
     * @param gadget   The gadgets from which the values are coming from.
     * @param gadgetDownloadService  The dedicated download service.
     * @param gadgetDataPointArr   the received values.
     */
    void onGadgetDownloadNewDataPoints(@NonNull Gadget gadget,
                                       @NonNull GadgetDownloadService gadgetDownloadService,
                                       @NonNull GadgetDataPoint[] gadgetDataPointArr);

    /**
     * Callback reporting that there was progress
     *
     * @param gadget   The gadgets from which the values are coming from.
     * @param gadgetDownloadService  The dedicated download service.
     * @param progress the delivery progress in percent
     */
    void onGadgetDownloadProgress(@NonNull Gadget gadget,
                                  @NonNull GadgetDownloadService gadgetDownloadService,
                                  int progress);


    /**
     * Callback reporting that there was a new value downloaded from the given
     *
     * @param gadget   The gadgets from which the values are coming from.
     * @param gadgetService  The dedicated service.
     * @param gadgetDataPoint   the newly received value.
     */
    void onGadgetNewDataPoint(@NonNull Gadget gadget,
                              @NonNull GadgetService gadgetService,
                              GadgetDataPoint gadgetDataPoint);


    /**
     * Callback when the logging state change has failed.
     *
     * @param gadget  The gadget on which the service is running.
     * @param service The service used to change the logging feature state.
     */
    void onSetGadgetLoggingEnabledFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

    /**
     * Called when the setting of the logger interval has failed.
     *
     * @param gadget  The gadget on which the download service is running.
     * @param service The service providing the interval change feature.
     */
    void onSetLoggerIntervalFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

    /**
     * Called when the logger interval was updated.
     *
     * @param gadget   The gadget on which the logger interval was changed.
     */
    void onSetLoggerIntervalSuccess(@NonNull Gadget gadget);

    /**
     * Callback when the download has failed.
     *
     * @param gadget  The gadget from which data was downloaded.
     * @param service The service used to download the data.
     */
    void onDownloadFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

    /**
     * Callback when the download is completed.
     *
     * @param gadget  The gadget from which data was downloaded.
     * @param service The service used to download the data.
     */
    void onDownloadCompleted(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

    /**
     * Callback when there is no data available for download.
     *
     * @param gadget  The gadget from which data download was tried.
     * @param service The service trying to download the data.
     */
    void onDownloadNoData(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

}
