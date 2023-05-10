package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
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

        // fetch language and switch to it
        SharedPreferences fetch = getApplicationContext().getSharedPreferences("biodynamiccalendar_APPSETTINGS", Context.MODE_PRIVATE); // stores language selection
        String savedLang = fetch.getString("lang", null);

        if (savedLang != null) {
            String code;
            if (savedLang.equals("English")) {code = "en";}
            else {code = "lv";}
            MainActivity.setLocale(SplashActivity.this, code);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        // gets date from which saved days start
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE); // get day type shared preferences
        String writeTime = sharedPrefs.getString("written", null);
        LocalDate currentFirst = LocalDate.now().withDayOfMonth(1);
        rewrite = false;

        if (writeTime == null) {
            // first launch, add new date to start saved day types from
            sharedPrefs.edit().putString("written", String.valueOf(currentFirst)).apply();
            rewrite = true;
        } else {
            Map<String, ?> allEntries = sharedPrefs.getAll();
            if (LocalDate.parse(writeTime).isEqual(currentFirst.minusMonths(1)) || allEntries.size() == 1) {
                // check if there is a connection
                checkConnection(() -> {
                    // delete preferences and add new start date if a new month has begun since last update or if last update was unsuccessful
                    sharedPrefs.edit().clear().apply();
                    sharedPrefs.edit().putString("written", String.valueOf(currentFirst)).apply();
                    rewrite = true;
                });
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (currentUser == null) {
                // check for and redirect non signed in users
                startActivity(new Intent(SplashActivity.this, AuthActivity.class));
                finish();
            } else {
                // launch the main activity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                intent.putExtra("rewrite", rewrite);
                startActivity(intent);
                finish();
            }
        }, 2000); // after 2 seconds wait time
    }

    // callback for a successful volley request
    public interface VolleyCallback {
        void onResponse();
    }

    // to check connectivity, check if the API can be reached
    public void checkConnection(final VolleyCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(this);
        // API url, the parameters do not matter
        String url = "https://api.visibleplanets.dev/v3?latitude=39&longitude=34&showCoords=true&aboveHorizon=false&time=20231231T235959";

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> callback.onResponse(), Throwable::printStackTrace);
        queue.add(stringRequest);
    }
}


