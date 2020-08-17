package com.karagathon.vesselreporting.common;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.karagathon.vesselreporting.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (findViewById(R.id.settings_framgent_container) != null) {
            if (savedInstanceState != null) return;

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_framgent_container, new SettingsFragment())
                    .commit();
        }
    }
}