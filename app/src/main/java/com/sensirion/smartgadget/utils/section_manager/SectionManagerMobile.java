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
package com.sensirion.smartgadget.utils.section_manager;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.sensirion.smartgadget.R;
import com.sensirion.smartgadget.view.comfort_zone.ComfortZoneFragment;
import com.sensirion.smartgadget.view.dashboard.DashboardFragment;
import com.sensirion.smartgadget.view.history.HistoryFragment;
import com.sensirion.smartgadget.view.preference.SmartgadgetPreferenceFragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SectionManagerMobile extends SectionManager {

    public static final int POSITION_DASHBOARD = 0;
    public static final int POSITION_COMFORT_ZONE = 1;
    public static final int POSITION_HISTORY = 2;
    public static final int POSITION_SETTINGS = 3;
    private static final int NUMBER_PAGES = 4;

    public SectionManagerMobile(@NonNull final FragmentManager fm) {
        super(fm);
    }

    /**
     * {@inheritDoc}
     */
    public int getPageId(@AvailableSections final int position) {
        switch (position) {
            case POSITION_DASHBOARD:
                return R.string.title_page_dashboard;
            case POSITION_COMFORT_ZONE:
                return R.string.title_page_comfort_zone;
            case POSITION_HISTORY:
                return R.string.title_page_history;
            case POSITION_SETTINGS:
                return R.string.title_page_settings;
            default:
                throw new IllegalArgumentException(String.format("getPageTitle -> Position %d it's not implemented yet.", position));
        }
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Fragment getItem(@AvailableSections final int position) {
        switch (position) {
            case POSITION_DASHBOARD:
                return new DashboardFragment();
            case POSITION_COMFORT_ZONE:
                return new ComfortZoneFragment();
            case POSITION_HISTORY:
                return new HistoryFragment();
            case POSITION_SETTINGS:
                return new SmartgadgetPreferenceFragment();
            default:
                throw new IllegalArgumentException(String.format("getItem -> Position %d it's not implemented yet.", position));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCount() {
        return NUMBER_PAGES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPosition(@NonNull final Context context, @NonNull final String name) {
        if (name.equals(context.getString(R.string.title_page_dashboard))) {
            return POSITION_DASHBOARD;
        }
        if (name.equals(context.getString(R.string.title_page_comfort_zone))) {
            return POSITION_COMFORT_ZONE;
        }
        if (name.equals(context.getString(R.string.title_page_history))) {
            return POSITION_HISTORY;
        }
        if (name.equals(context.getString(R.string.title_page_settings))) {
            return POSITION_SETTINGS;
        }
        throw new IllegalArgumentException(String.format("getPosition -> %s it's not implemented.", name));
    }

    @Nullable
    public Drawable getIcon(@NonNull final Context context, final int position) {
        switch (position) {
            case POSITION_DASHBOARD:
                return context.getResources().getDrawable(R.drawable.dashboard_tab_icon);
            case POSITION_COMFORT_ZONE:
                return context.getResources().getDrawable(R.drawable.comfort_zone_tab_icon);
            case POSITION_HISTORY:
                return context.getResources().getDrawable(R.drawable.history_tab_icon);
            case POSITION_SETTINGS:
                return context.getResources().getDrawable(R.drawable.settings_tab_icon);
            default:
                throw new IllegalArgumentException(String.format("getIcon -> Icon with position %d it's not implemented.", position));
        }
    }

    @IntDef(flag = false,
            value = {POSITION_DASHBOARD, POSITION_COMFORT_ZONE, POSITION_HISTORY, POSITION_SETTINGS})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AvailableSections {
    }
}
