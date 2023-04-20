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
import java.util.Map;


@SuppressLint("CustomSplashScreen") // android splash screen API was introduced for android 12 and later, while development environment is lower
public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    boolean rewrite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // request perms (if needed). as of now those would be location
        // check connectivity
        // find a way to make locale permanent
        // comments
        // deprecated methods

        mAuth = FirebaseAuth.getInstance();
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE); // get day type shared preferences

        // gets date from which saved day types start
        // Main activity could handle this...
        String writeTime = sharedPrefs.getString("written", null);
        LocalDate currentFirst = LocalDate.now().withDayOfMonth(1);
        rewrite = false;
        if (writeTime == null) {
            // first launch, add new date to start saved day types from
            sharedPrefs.edit().putString("written", String.valueOf(currentFirst)).apply();
            rewrite = true;
        } else {
            Map<String, ?> allEntries = sharedPrefs.getAll();
            if (LocalDate.parse(writeTime).isEqual(currentFirst.minusMonths(1)) || allEntries.size() == 1 || true) {
                // delete preferences and add new start date if a new month has begun since last update or if last update was unsuccessful
                sharedPrefs.edit().clear().apply();
                sharedPrefs.edit().putString("written", String.valueOf(currentFirst)).apply();
                rewrite = true;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // check for and redirect non signed in users
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (currentUser == null) {
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
                finish();
            } else {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("rewrite", rewrite);
                startActivity(intent);
                finish();
            }
        }, 2000); // launch the main activity after 2 seconds, change this
    }
}


