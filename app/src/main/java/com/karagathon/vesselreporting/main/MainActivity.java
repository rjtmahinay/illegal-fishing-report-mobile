package com.karagathon.vesselreporting.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.login.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private Handler handler;
    private ImageView splashLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        splashLogo = findViewById(R.id.splash_logo);
        splashLogo.animate().alpha(3000).setDuration(0);

        handler = new Handler();
        handler.postDelayed(() -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, 3000);
    }
}