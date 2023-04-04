package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

@SuppressLint("CustomSplashScreen") // android splash screen API was introduced for API 30, development API is lower
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /* // hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        } */

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2000); // make this different in the future?
    }
}

