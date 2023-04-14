package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import java.time.LocalDate;
import java.util.Map;

@SuppressLint("CustomSplashScreen") // android splash screen API was introduced for android 12 and later, while development environment is lower
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // request perms (if needed) here. as of now those would be location and message

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);
        String lastUpdate = sharedPref.getString("updated", null);
        LocalDate currentDate = LocalDate.now();
        if(lastUpdate == null) {
            //First time
            // LocalDate currentDateMinus6Months = currentDate.minusMonths(6);
            sharedPref.edit().putString("updated", String.valueOf(currentDate)).apply();
            Log.d("NEWUPDATE", "added " + currentDate);
        } else {
            LocalDate lastUpdateDateVal = LocalDate.parse(lastUpdate);
            LocalDate monthAgo = currentDate.minusMonths(1);
            if(lastUpdateDateVal.isBefore(monthAgo)) {
                // add editor
                sharedPref.edit().clear().apply();
                sharedPref.edit().putString("updated", String.valueOf(currentDate)).apply();
                Log.d("UPDATEOLD", "rewrote " + currentDate);
            }
        }

        /* Map<String, ?> allEntries = sharedPref.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
        } */

        Handler handler = new Handler();
        // launch the main activity after 2 seconds
        handler.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2000);
    }
}

