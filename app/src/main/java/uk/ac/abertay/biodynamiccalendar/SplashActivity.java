package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDate;


@SuppressLint("CustomSplashScreen") // android splash screen API was introduced for android 12 and later, while development environment is lower
public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // request perms (if needed) here. as of now those would be location
        // check connectivity

        mAuth = FirebaseAuth.getInstance();

        // get day type shared preferences
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);
        sharedPrefs.edit().clear().apply(); // deletes stuff for testing

        // gets date from which saved day types start
        // rn main can handle this
        String writeTime = sharedPrefs.getString("written", null);
        LocalDate currentFirst = LocalDate.now().withDayOfMonth(1);
        boolean rewrite = false;
        if (writeTime == null) {
            // first launch, add new date to start saved day types from
            sharedPrefs.edit().putString("written", String.valueOf(currentFirst)).apply();
            rewrite = true;
        } else {
            if (LocalDate.parse(writeTime).isEqual(currentFirst.minusMonths(1))) {
                // delete preferences and add new start date if a new month has begun since last update
                // check internet here
                sharedPrefs.edit().clear().apply();
                sharedPrefs.edit().putString("written", String.valueOf(currentFirst)).apply();
                rewrite = true;
            }
        }

        Handler handler = new Handler();
        boolean finalRewrite = rewrite;
        handler.postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.putExtra("rewrite", finalRewrite);
            startActivity(intent);
            finish();
        }, 2000); // launch the main activity after 2 seconds, change this
    }

    @Override
    protected void onStart() {
        super.onStart();
        // check for and redirect non signed in users
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(SplashActivity.this, AuthActivity.class));
            finish();
        }
    }
}


