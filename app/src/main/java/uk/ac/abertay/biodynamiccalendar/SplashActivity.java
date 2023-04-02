package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.applandeo.materialcalendarview.EventDay;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

@SuppressLint("CustomSplashScreen") // development API lower than 30 (?) needed to use android splash screen API
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // hide action bar for the splash screen
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // should ask for login and perms here now? since i need the latitude etc

        /* Calendar currCal = Calendar.getInstance(); // get current date
        List<EventDay> events = Collections.synchronizedList(new ArrayList<>());

        // if not in shared preferences
        parseMonth(currCal, events); // parse the current month
        // add if in shared preferences

        // if not in shared preferences
        // parse the next month (only the nearest months are needed for the app to serve its purpose)
        currCal.add(Calendar.MONTH, 1);
        parseMonth(currCal, events);
        // add if in shared


        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("EVENTS_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();
        String json = gson.toJson(events);

        editor.putString("parsed_data", json);
        editor.apply(); */


        Handler handler = new Handler();
        handler.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 2000);

        // startActivity(new Intent(SplashActivity.this, MainActivity.class));
        // finish();
    }


    private void parseMonth(Calendar currCal, List <EventDay> events) {
        String[] dates = formatDate(currCal);
        // List<EventDay> events = Collections.synchronizedList(new ArrayList<>());
        YearMonth yearMonthObject = YearMonth.of(Integer.parseInt(dates[0]), Integer.parseInt(dates[1]));

        // will be pulled from db
        String latitude = "56.462002";
        String longitude = "-2.970700";

        // ExecutorService executor= Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 1; i <= yearMonthObject.lengthOfMonth(); i++) {
            String iso8601 = dates[0] + dates[1] + i + "T000000"; // maybe also change time stuff. currently 12am
            String url = "https://api.visibleplanets.dev/v3?latitude=" + latitude + "&longitude=" + longitude + "&aboveHorizon=false&time=" + iso8601;

            try {
                test request = new test(i, url, dates, events, this.getApplicationContext());
                new Thread(request).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // executor.shutdown();
        // CalendarView calendarView = findViewById(R.id.calendarView);
        // calendarView.setEvents(events);
        // Log.d("Main Activity", "Drew stuff.");
    }

    // make utils and put this there
    private String[] formatDate(Calendar calendar) {
        // get values and format to strings (for displaying easier and API query formatting)
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month;
        if (calendar.get(Calendar.MONTH) < 9) {
            //  prefix with 0 if month is a single digit (for API query formatting)
            month = "0" + (calendar.get(Calendar.MONTH) + 1);
        } else {
            month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        }
        String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

        return new String[] {year, month, day};
    }
}

// go over later and check for any improvements
class test implements Runnable {
    int i;
    String url;
    String[] dates;
    List<EventDay> events;
    Context context;
    test (int i, String url, String[] dates, List<EventDay> events, Context context) {
        this.i = i;
        this.url = url;
        this.dates = dates;
        this.events = events;
        this.context = context;
    }
    @Override
    public void run() {
        Log.d("Request", "Thread started - " + i);
        try{
            // Make API request
            StringRequest stringRequest = new StringRequest(url, response -> {
                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray responseArray = responseObject.getJSONArray("data");

                    JSONObject moonArray = responseArray.getJSONObject(1);
                    String constValue = moonArray.getString("constellation");

                    labelDay(i, dates, constValue, events);

                }  catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {
                // add error stuff
            });

            // Add the request to the RequestQueue
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(stringRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void labelDay(int i, String[] dates, String constValue, List<EventDay> events){
        // array is for testing, will put elsewhere
        // make a 2d array with the day types corresponding to constellations?
        String[] constArray = {"Capricornus","Taurus", "Virgo", "Gemini", "Libra", "Aquarius", "Pisces", "Scorpius", "Cancer", "Ophiuchus", "Aries", "Sagittarius", "Leo"};

        // also make into function
        for (int a = 0; a < constArray.length; a++) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Integer.parseInt(dates[0]), Integer.parseInt(dates[1]) - 1, i);

            if (constArray[a].equals(constValue)) {
                if (a <= 2) {
                    events.add(new EventDay(calendar, R.drawable.event_root));
                } else if (a <= 5) {
                    events.add(new EventDay(calendar, R.drawable.event_flower));
                } else if (a <= 9) {
                    events.add(new EventDay(calendar, R.drawable.event_leaf));
                } else {
                    events.add(new EventDay(calendar, R.drawable.event_fruit));
                }
                return;
            }
        }
    }
}

