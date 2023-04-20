package uk.ac.abertay.biodynamiccalendar;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

// makes an API request and parses response
class LabelDay implements Runnable {
    String url;
    int i;
    String[] dates;
    List<EventDay> events;
    Context context;
    Activity activity;
    LabelDay (String url, int i, String[] dates, List<EventDay> events, Context context, Activity activity) {
        this.url = url;
        this.i = i;
        this.dates = dates;
        this.events = events;
        this.context = context;
        this.activity = activity;
    }
    @Override
    public void run() {
        makeRequest();
    }

    private void makeRequest() {
        try{
            // make a request to visibleplanets API with volley
            StringRequest stringRequest = new StringRequest(url, response -> {
                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray responseArray = responseObject.getJSONArray("data");
                    JSONObject moonArray = responseArray.getJSONObject(1);
                    String constValue = moonArray.getString("constellation");
                    parseResponse(constValue);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }, Throwable::printStackTrace);

            // add the request to the RequestQueue
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(stringRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // labels days based on their type and writes result to shared preferences
    private void parseResponse(String constValue){
        // need to add Cetus to array
        String[] constArray = {"Capricornus","Taurus", "Virgo", "Gemini", "Libra", "Aquarius", "Pisces", "Scorpius", "Cancer", "Ophiuchus", "Aries", "Sagittarius", "Leo"};

        for (int a = 0; a < constArray.length; a++) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Integer.parseInt(dates[0]), Integer.parseInt(dates[1]) - 1, i);

            if (constArray[a].equals(constValue)) {
                if (a <= 2) {
                    MainActivity.labelCell(events, calendar, 1);
                    writeToPrefs(1);
                } else if (a <= 5) {
                    MainActivity.labelCell(events, calendar, 2);
                    writeToPrefs(2);
                } else if (a <= 9) {
                    MainActivity.labelCell(events, calendar, 3);
                    writeToPrefs(3);
                } else {
                    MainActivity.labelCell(events, calendar, 4);
                    writeToPrefs(4);
                }
                // start populating the fields
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> {
                    CalendarView calendarView = activity.findViewById(R.id.calendarView);
                    calendarView.setEvents(events);
                });
                return;
            }
        }
    }

    // writes day type to shared preferences
    private void writeToPrefs(int type) {
        SharedPreferences sharedPrefs = context.getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);
        if (!sharedPrefs.contains(dates[0] + "-" + dates[1] + "-" + i)) {
            sharedPrefs.edit().putInt(dates[0] + "-" + dates[1] + "-" + i, type).apply();
        }
    }
}
