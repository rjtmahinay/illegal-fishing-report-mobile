package com.karagathon.vesselreporting.common;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.karagathon.vesselreporting.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }


}