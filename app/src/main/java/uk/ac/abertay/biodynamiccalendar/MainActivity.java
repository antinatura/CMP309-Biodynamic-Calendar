package uk.ac.abertay.biodynamiccalendar;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
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

        // get calendar view object
        CalendarView calendarView = findViewById(R.id.calendarView);

        // get current date
        Calendar currCal = Calendar.getInstance();
        // set minimum and maximum date for calendarView
        setLimits(currCal, calendarView);

        // parse the current month, if not in db
        parseMonth(currCal);

        // when a specific day is tapped, open a page for it
        calendarView.setOnDayClickListener(this::launchDay);

        // on month change forward, parse that month, if not in db
        // month change backward for this seems unnecessary

        // gotta also do something about the lifecycle
        // add menu with app info, user info + maybe language and theme change buttons
    }

    // syntax can be changed? from the library: calendar.add(Calendar.DAY_OF_MONTH, 10);
    private void setLimits(Calendar currCal, CalendarView calendarView) {
        // set minimum and maximum date for calendarView
        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();
        // limits are two years before and after the current date
        min.set(currCal.get(Calendar.YEAR) - 2, currCal.get(Calendar.MONTH), currCal.get(Calendar.DAY_OF_MONTH));
        max.set(currCal.get(Calendar.YEAR) + 2, currCal.get(Calendar.MONTH), currCal.get(Calendar.DAY_OF_MONTH));
        calendarView.setMinimumDate(min);
        calendarView.setMaximumDate(max);
    }

    private void parseMonth(Calendar currCal) {
        String[] dates = formatDate(currCal);

        YearMonth yearMonthObject = YearMonth.of(Integer.parseInt(dates[0]), Integer.parseInt(dates[1]));
        // put elsewhere + will be pulled from db
        String latitude = "56.462002";
        String longitude = "-2.970700";

        List<EventDay> events = new ArrayList<>();

        // thread this and make into function maybe
        for (int i = 1; i <= yearMonthObject.lengthOfMonth(); i++) {
            String iso8601 = dates[0] + dates[1] + i + "T000000"; // maybe also change time stuff. currently 12am
            String url = "https://api.visibleplanets.dev/v3?latitude=" + latitude + "&longitude=" + longitude + "&aboveHorizon=false&time=" + iso8601;

            // Make API request
            int finalI = i;
            StringRequest stringRequest = new StringRequest(url, response -> {
                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray responseArray = responseObject.getJSONArray("data");

                    JSONObject moonArray = responseArray.getJSONObject(1);
                    String constValue = moonArray.getString("constellation");

                    labelDay(finalI, dates, constValue, events);

                }  catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {
                // add error stuff
            });

            // Add the request to the RequestQueue
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(stringRequest);
        }

        CalendarView calendarView = findViewById(R.id.calendarView);
        calendarView.setEvents(events);
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
                    // events.add(new EventDay(calendar, R.drawable.event_root));
                    events.add(new EventDay(calendar, getEventLabel(this, R.drawable.event_root)));
                } else if (a <= 5) {
                    // events.add(new EventDay(calendar, R.drawable.event_flower));
                    events.add(new EventDay(calendar, getEventLabel(this, R.drawable.event_flower)));
                } else if (a <= 9) {
                    // events.add(new EventDay(calendar, R.drawable.event_leaf));
                    events.add(new EventDay(calendar, getEventLabel(this, R.drawable.event_leaf)));
                } else {
                    // events.add(new EventDay(calendar, R.drawable.event_fruit));
                    events.add(new EventDay(calendar, getEventLabel(this, R.drawable.event_fruit)));
                }
               return;
            }
        }
    }

    //  from Material calendar, working on this later if needed
    private static Drawable getEventLabel(Context context, @DrawableRes int event) {
        Drawable drawable = ContextCompat.getDrawable(context, event);
        //Add padding to too large icon
        return new InsetDrawable(drawable, 100, 0, 100, 0);
    }

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

    // date format function, this will get tweaks bc its interfering with some stuff
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
