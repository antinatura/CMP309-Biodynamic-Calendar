package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // need to check for permissions
        // google login
        // need to get location

        /* List<EventDay> events;
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("EVENTS_PREFS", Context.MODE_PRIVATE);
        String serializedObject = sharedPreferences.getString("parsed_data", null);
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<EventDay>>(){}.getType();
        events = gson.fromJson(serializedObject, type); */

        CalendarView calendarView = findViewById(R.id.calendarView); // get calendar view object
        setLimits(calendarView); // set minimum and maximum date for calendarView

        Calendar currCal = Calendar.getInstance(); // get current date
        List<EventDay> events = Collections.synchronizedList(new ArrayList<>());

        // if not in shared resources
        parseMonth(currCal, events); // parse the current month
        // add if in shared resources

        // if not in shared resources
        // parse the next month (only the nearest months are needed for the app to serve its purpose)
        currCal.add(Calendar.MONTH, 1);
        parseMonth(currCal, events);
        // add if in shared resources

        calendarView.setEvents(events); // run in bg
        calendarView.invalidate(); // call this on month change?
        // Log.d("Main Activity", "Drew stuff.");

        // when a specific day is tapped, open a page for it
        calendarView.setOnDayClickListener(this::launchDay);

        // add db stuff as well
        // month change backward for this seems unnecessary

        // gotta also do something about the lifecycle
        // add menu with app info, user info + maybe language and theme change buttons
        // moon phase api bc full and new moon should be gray and not any of the day types

        // any way to save the view and not have it pull stuff from db (if its there) every time?
        // also shared resources or firestore for the day type stuff?
    }

    // set minimum and maximum date for calendarView
    private void setLimits(CalendarView calendarView) {
        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();
        // limits are a year before and after the current date
        min.add(Calendar.YEAR, - 1);
        max.add(Calendar.YEAR, 1);
        calendarView.setMinimumDate(min);
        calendarView.setMaximumDate(max);
    }

    private void parseMonth(Calendar currCal, List<EventDay> events) {
        String[] dates = formatDate(currCal);
        // List<EventDay> events = Collections.synchronizedList(new ArrayList<>());
        YearMonth yearMonthObject = YearMonth.of(Integer.parseInt(dates[0]), Integer.parseInt(dates[1]));

        // will be pulled from db
        String latitude = "56.462002";
        String longitude = "-2.970700";

        // ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 1; i <= yearMonthObject.lengthOfMonth(); i++) {
            String iso8601 = dates[0] + dates[1] + i + "T100000"; // maybe also change time stuff. currently 10am
            String url = "https://api.visibleplanets.dev/v3?latitude=" + latitude + "&longitude=" + longitude + "&aboveHorizon=false&time=" + iso8601;

            try {
                Request request = new Request(i, url, dates, events, this.getApplicationContext());
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

   // when storage is implemented, change the extras around
    private void launchDay(EventDay eventDay) {
        // get clicked day's calendar values
        Date clickedDay = eventDay.getCalendar().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(clickedDay);

        Intent intent = new Intent(MainActivity.this, DayActivity.class);
        // passes day, month, year as a string array to DayActivity as extras
        intent.putExtra("extras",  formatDate(calendar));
        startActivity(intent);
    }

    // date format function, this will get some tweaks bc its interfering with some stuff with how im formatting it.
    // maybe make it return int[] and only do stringvalueof() where needed.
    // the + 1 also interferes with some stuff where months are counted from 0.
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
class Request implements Runnable {
    int i;
    String url;
    String[] dates;
    List<EventDay> events;
    Context context;
    Request (int i, String url, String[] dates, List<EventDay> events, Context context) {
        this.i = i;
        this.url = url;
        this.dates = dates;
        this.events = events;
        this.context = context;
    }
    @Override
    public void run() {
        // Log.d("Request", "Thread started - " + i);
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
