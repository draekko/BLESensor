/*
 * Copyright (c) 2017, Sensirion AG
 * Copyright (c) 2024, Draekko RAND
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of Sensirion AG nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.sensirion.smartgadget.view.preference;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.sensirion.smartgadget.R;
import com.sensirion.smartgadget.peripheral.rht_sensor.RHTSensorFacade;
import com.sensirion.smartgadget.peripheral.rht_sensor.RHTSensorListener;
import com.sensirion.smartgadget.peripheral.rht_sensor.external.RHTHumigadgetSensorManager;
import com.sensirion.smartgadget.utils.Settings;
import com.sensirion.smartgadget.utils.view.AboutDialog;
import com.sensirion.smartgadget.utils.view.ParentListFragment;
import com.sensirion.smartgadget.utils.view.PrivacyPolicyDialog;
import com.sensirion.smartgadget.utils.view.SectionAdapter;
import com.sensirion.smartgadget.utils.view.SmartGadgetRequirementDialog;
import com.sensirion.smartgadget.view.MainActivity;
import com.sensirion.smartgadget.view.device_management.ScanDeviceFragment;
import com.sensirion.smartgadget.view.preference.adapter.PreferenceAdapter;

public class SmartgadgetPreferenceFragment extends ParentListFragment implements RHTSensorListener {

    public static final int REMOTE_INSTRUCTION_OPEN_SCAN_FRAGMENT = 1;

    // Class name
    @NonNull
    private static final String TAG = SmartgadgetPreferenceFragment.class.getSimpleName();

    Button mFindGadgetButton;

    boolean IS_TABLET;

    // XML resources
    String CONDENSED_TYPEFACE;
    String BOLD_TYPEFACE;
    String SEASON_PREFERENCE_LABEL;
    String DEVICES_PREFERENCE_LABEL;
    String GLOSSARY_PREFERENCE_LABEL;
    String CONNECTION_HEADER;
    String TEMPERATURE_PREFERENCE_LABEL;
    String USER_PREFERENCES_HEADER;
    String APPLICATION_REQUIREMENTS_LABEL;
    String PRIVACY_POLICY_LABEL;
    String ABOUT_PREFERENCE_LABEL;
    String APP_INFORMATION_HEADER;

    // Layout Adapters
    @Nullable
    private SectionAdapter mSectionAdapter;
    @Nullable
    private PreferenceAdapter mConnectionsAdapter;
    @Nullable
    private PreferenceAdapter mUserPreferencesAdapter;

    private Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IS_TABLET = mContext.getResources().getBoolean(R.bool.is_tablet);
        CONDENSED_TYPEFACE = mContext.getResources().getString(R.string.typeface_condensed);
        BOLD_TYPEFACE = mContext.getResources().getString(R.string.typeface_bold);
        SEASON_PREFERENCE_LABEL = mContext.getResources().getString(R.string.label_season);
        DEVICES_PREFERENCE_LABEL = mContext.getResources().getString(R.string.label_smart_gadgets);
        GLOSSARY_PREFERENCE_LABEL = mContext.getResources().getString(R.string.label_glossary);
        CONNECTION_HEADER = mContext.getResources().getString(R.string.header_connections);
        TEMPERATURE_PREFERENCE_LABEL = mContext.getResources().getString(R.string.label_temperature_unit);
        USER_PREFERENCES_HEADER = mContext.getResources().getString(R.string.header_user_prefs);
        APPLICATION_REQUIREMENTS_LABEL = mContext.getResources().getString(R.string.label_application_requirements);
        PRIVACY_POLICY_LABEL = mContext.getResources().getString(R.string.label_privacy_policy);
        ABOUT_PREFERENCE_LABEL = mContext.getResources().getString(R.string.label_about);
        APP_INFORMATION_HEADER = mContext.getResources().getString(R.string.header_app_information);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() -> Refreshing number of devices.");
        if (mConnectionsAdapter == null) {
            initPreferencesList();
        }
        refreshPreferenceAdapter();
        refreshUserPreferenceAdapter();
        setListAdapter(mSectionAdapter);
        RHTSensorFacade.getInstance().registerListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        RHTSensorFacade.getInstance().unregisterListener(this);
    }

    @Override
    @NonNull
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_smartgadget_list, container, false);
        final Typeface typefaceBold = Typeface.createFromAsset(mContext.getAssets(), BOLD_TYPEFACE);

        mFindGadgetButton = view.findViewById(R.id.button_find_gadget);

        initPreferencesList();

        mFindGadgetButton.setTypeface(typefaceBold);
        if (IS_TABLET) {
            mFindGadgetButton.setVisibility(View.GONE);
        }
        mFindGadgetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScanDeviceFragment();
            }
        });

        return view;
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        if (menuVisible) {
            MainActivity activity = (MainActivity) getParent();
            if (activity == null) {
                return;
            }
            final int instruction = activity.getRemoteInstruction();
            switch (instruction) {
                case REMOTE_INSTRUCTION_OPEN_SCAN_FRAGMENT:
                    openScanDeviceFragment();
                    break;
                default:
                    break;
            }
        }
    }

    private void initPreferencesList() {
        mSectionAdapter = new SectionAdapter() {
            @NonNull
            @Override
            protected View getHeaderView(@NonNull final String caption,
                                         final int itemIndex,
                                         @Nullable final View convertView,
                                         @Nullable final ViewGroup parent) {
                TextView listItemHeader = (TextView) convertView;
                if (listItemHeader == null) {
                    final AssetManager assets = mContext.getAssets();
                    final Typeface typefaceBold = Typeface.createFromAsset(assets, BOLD_TYPEFACE);
                    listItemHeader = (TextView) View.inflate(getParent(), R.layout.listitem_scan_header, null);
                    listItemHeader.setTypeface(typefaceBold);
                }
                listItemHeader.setText(caption);
                return listItemHeader;
            }
        };
        initConnectionPreferenceAdapter();
        initUserPreferenceAdapter();
        if (mConnectionsAdapter == null) {
            Log.e(TAG, "initPreferencesList -> Connection adapter can't be null");
            return;
        }
        if (mUserPreferencesAdapter == null) {
            Log.e(TAG, "initPreferencesList -> User preferences adapter can't be null");
            return;
        }
        mSectionAdapter.addSectionToAdapter(CONNECTION_HEADER, mConnectionsAdapter);
        mSectionAdapter.addSectionToAdapter(USER_PREFERENCES_HEADER, mUserPreferencesAdapter);
        mSectionAdapter.addSectionToAdapter(APP_INFORMATION_HEADER, getAppInformationAdapter());
    }

    /**
     * ************************************************************************
     * *********************** CONNECTION PREFERENCES *************************
     * ************************************************************************
     */

    private void initConnectionPreferenceAdapter() {
        final AssetManager assets = mContext.getAssets();
        final Typeface typefaceCondensed = Typeface.createFromAsset(assets, CONDENSED_TYPEFACE);
        mConnectionsAdapter = new PreferenceAdapter(typefaceCondensed);
        refreshPreferenceAdapter();
    }

    private void refreshPreferenceAdapter() {
        if (mConnectionsAdapter == null) {
            Log.e(TAG, "refreshPreferenceAdapter -> Connection adapter can't be null");
            return;
        }
        final String title = getConnectedDevicesTitle();
        final View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openScanDeviceFragment();
            }
        };
        mConnectionsAdapter.clear();
        mConnectionsAdapter.addPreference(title, null, clickListener);
        mConnectionsAdapter.notifyDataSetChanged();
    }

    private void openScanDeviceFragment() {
        getListView().setVisibility(View.GONE);
        final MainActivity mainActivity = (MainActivity) getParent();
        if (mainActivity == null) {
            Log.e(TAG, "refreshPreferenceAdapter.onClick -> getParent() returned null");
        } else {
            mainActivity.changeFragment(new ScanDeviceFragment());
        }
    }

    @NonNull
    private String getConnectedDevicesTitle() {
        final int numberConnectedGadgets = RHTHumigadgetSensorManager.getInstance().getConnectedDevicesCount();
        if (numberConnectedGadgets == 0) {
            return DEVICES_PREFERENCE_LABEL;
        }
        return String.format("%s (%d)", DEVICES_PREFERENCE_LABEL, numberConnectedGadgets);
    }


    /**
     * ************************************************************************
     * *************************** USER PREFERENCES ***************************
     * ************************************************************************
     */
    private void initUserPreferenceAdapter() {
        final AssetManager assets = mContext.getAssets();
        final Typeface typefaceCondensed = Typeface.createFromAsset(assets, CONDENSED_TYPEFACE);
        mUserPreferencesAdapter = new PreferenceAdapter(typefaceCondensed);
        refreshUserPreferenceAdapter();
    }

    private void refreshUserPreferenceAdapter() {
        if (mUserPreferencesAdapter == null) {
            Log.e(TAG, "refreshUserPreferenceAdapter -> mUserPreferenceAdapter can't be null");
            return;
        }
        mUserPreferencesAdapter.clear();
        addTemperatureUnitPreferenceAdapter();
        addSeasonPreferenceAdapter();
    }

    @SuppressLint("CommitPrefEdits")
    private void addTemperatureUnitPreferenceAdapter() {
        if (mUserPreferencesAdapter == null) {
            Log.e(TAG, "addTemperatureUnitPreferenceAdapter -> mUserPreferenceAdapter can't be null");
            return;
        }
        final String summary = Settings.getInstance().getSelectedTemperatureUnit();

        final View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(@NonNull final View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
                builder.setCancelable(true)
                        .setTitle(R.string.title_button_choice)
                        .setItems(R.array.array_temp_unit, new DialogInterface.OnClickListener() {
                            public void onClick(@NonNull final DialogInterface dialog,
                                                final int which) {
                                final String newSummary =
                                        getResources().getTextArray(R.array.array_temp_unit)[which].toString();
                                Settings.getInstance().setSelectedTemperatureUnit(newSummary);
                                final TextView summaryTextView = (TextView) v.findViewById(R.id.preference_summary);
                                summaryTextView.setText(newSummary);
                            }
                        });
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        };
        mUserPreferencesAdapter.addPreference(TEMPERATURE_PREFERENCE_LABEL, summary, clickListener);
    }

    @SuppressLint("CommitPrefEdits")
    private void addSeasonPreferenceAdapter() {
        if (mUserPreferencesAdapter == null) {
            Log.e(TAG, "addSeasonPreferenceAdapter -> mUserPreferenceAdapter can't be null");
            return;
        }
        final String summary = Settings.getInstance().getSelectedSeason();

        final View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(@NonNull final View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
                builder.setCancelable(true)
                        .setTitle(R.string.title_button_choice)
                        .setItems(R.array.array_season, new DialogInterface.OnClickListener() {
                            public void onClick(@NonNull final DialogInterface dialog,
                                                final int which) {
                                final String newSummary =
                                        getResources().
                                                getTextArray(R.array.array_season)[which].
                                                toString();
                                Settings.getInstance().setSelectedSeason(newSummary);
                                final TextView summaryView = ((TextView) v.findViewById(R.id.preference_summary));
                                summaryView.setText(newSummary);
                            }
                        });
                final AlertDialog dialog = builder.create();
                dialog.show();
            }
        };
        mUserPreferencesAdapter.addPreference(SEASON_PREFERENCE_LABEL, summary, clickListener);
    }

    /**
     * ************************************************************************
     * ***************************  APP INFORMATION ***************************
     * ************************************************************************
     */
    @NonNull
    private PreferenceAdapter getAppInformationAdapter() {
        final AssetManager assets = mContext.getAssets();
        final Typeface typefaceCondensed = Typeface.createFromAsset(assets, CONDENSED_TYPEFACE);
        final PreferenceAdapter appInformationAdapter = new PreferenceAdapter(typefaceCondensed);
        //  addGlossaryAdapter(appInformationAdapter);
        if (!RHTSensorFacade.getInstance().hasInternalRHTSensor()) {
            addApplicationRequirementsAdapter(appInformationAdapter);
        }
        addPrivacyPolicyAdapter(appInformationAdapter);
        addShowAboutAdapter(appInformationAdapter);
        return appInformationAdapter;
    }

    private void addApplicationRequirementsAdapter(@NonNull final PreferenceAdapter adapter) {
        final View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(@NonNull final View v) {
                (new SmartGadgetRequirementDialog(getActivity())).show();
            }
        };
        adapter.addPreference(APPLICATION_REQUIREMENTS_LABEL, null, clickListener);
    }

    private void addPrivacyPolicyAdapter(@NonNull final PreferenceAdapter adapter) {
        final View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(@NonNull final View v) {
                (new PrivacyPolicyDialog(getActivity())).show();
            }
        };
        adapter.addPreference(PRIVACY_POLICY_LABEL, null, clickListener);
    }

    // TODO: Check with PM if still needed. Otherwise remove entirely
//    private void addGlossaryAdapter(@NonNull final PreferenceAdapter adapter) {
//        final View.OnClickListener clickListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getListView().setVisibility(View.GONE);
//                final MainActivity mainActivity = (MainActivity) getParent();
//                if (mainActivity == null) {
//                    Log.e(TAG, "addGlossaryAdapter -> Cannot obtain the MainActivity.");
//                } else {
//                    mainActivity.changeFragment(new GlossaryFragment());
//                }
//            }
//        };
//        adapter.addPreference(GLOSSARY_PREFERENCE_LABEL, null, clickListener);
//    }

    private void addShowAboutAdapter(@NonNull final PreferenceAdapter adapter) {
        final View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(@NonNull final View v) {
                (new AboutDialog(getActivity())).show();
            }
        };
        adapter.addPreference(ABOUT_PREFERENCE_LABEL, null, clickListener);
    }

    @Override
    public void onNewRHTSensorData(float temperature, float relativeHumidity, @Nullable String deviceAddress) {
        // do nothing
    }

    @Override
    public void onGadgetConnectionChanged(@NonNull String deviceAddress, boolean deviceIsConnected) {
        refreshPreferenceAdapter();
    }
}
