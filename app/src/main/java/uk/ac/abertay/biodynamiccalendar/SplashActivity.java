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

@SuppressLint("CustomSplashScreen") // android splash screen API was introduced for android 12 and later, while development environment is lower
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // request perms (if needed) here. as of now those would be location and notifications

        // get day type shared preferences
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);
        // sharedPrefs.edit().clear().apply(); // deletes stuff for testing

        // gets date from which saved day types start
        String writeTime = sharedPrefs.getString("written", null);
        LocalDate currentFirst = LocalDate.now().withDayOfMonth(1);
        if (writeTime == null) {
            // first launch, add new date to start saved day types from
            sharedPrefs.edit().putString("written", String.valueOf(currentFirst)).apply();
        } else {
            if (LocalDate.parse(writeTime).isEqual(currentFirst.minusMonths(1))) {
                // delete preferences and add new start date if a new month has begun since last update
                sharedPrefs.edit().clear().apply();
                sharedPrefs.edit().putString("written", String.valueOf(currentFirst)).apply();
            }
        }

        /*
        Map<String, ?> allEntries = sharedPrefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("map values", entry.getKey() + ": " + entry.getValue().toString());
        }
        */

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2000); // launch the main activity after 2 seconds, change this
    }
}


