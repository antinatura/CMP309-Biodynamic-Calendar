package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.applandeo.materialcalendarview.listeners.OnCalendarPageChangeListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static DrawerLayout drawerLayout;

    private FirebaseAuth mAuth;

    private GoogleSignInClient gsc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // need to check for permissions
        // need to get location

        // app to be portrait only?

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestIdToken(getString(R.string.default_web_client_id)).requestProfile().build();
        gsc = GoogleSignIn.getClient(this, gso);

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        drawerLayout = findViewById(R.id.main_layout);
        CalendarView calendarView = findViewById(R.id.calendarView); // get calendar view object

        ImageView icon = findViewById(R.id.icon);
        TextView email = findViewById(R.id.email);

        String imgUrl = String.valueOf(acct.getPhotoUrl());
        imgUrl = imgUrl.replace("s96-c", "s192-c");
        Picasso.get().load(imgUrl).into(icon);

        String personEmail = acct.getEmail();
        email.setText(personEmail);

        setLimits(calendarView); // set minimum and maximum date for calendarView

        Calendar currCal = Calendar.getInstance(); // get current date

        List<EventDay> events = Collections.synchronizedList(new ArrayList<>());

        // if not in shared resources?
        parseMonth(currCal, events); // parse the current month
        // add if in shared resources

        // if not in shared resources?
        // parse the next month (only the nearest months are needed for the app to serve its purpose)
        currCal.add(Calendar.MONTH, 1);
        parseMonth(currCal, events);
        // add if in shared resources

        calendarView.setEvents(events); // run in bg somehow?
        drawerLayout.invalidate(); // call this on month change?
        // Log.d("Main Activity", "Drew stuff.");

        // when a specific day is tapped, open a page for it
        calendarView.setOnDayClickListener(this::launchDay);

        // gotta also do something about the lifecycle
        // add menu with app info, user info + maybe language and theme change buttons
        // moon phase api bc full and new moon should be gray and not any of the day types

        // any way to save the view and not have it pull stuff from db (if its there) every time?
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
        YearMonth yearMonthObject = YearMonth.of(Integer.parseInt(dates[0]), Integer.parseInt(dates[1]));

        // will be pulled from db
        String latitude = "56.462002";
        String longitude = "-2.970700";

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
    }

   // find a way to pass day type as intent
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

    public void openMenu(View view) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    public void closeMenu(View view) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public void clickHelp(View view) {
        startActivity(new Intent(MainActivity.this, AboutActivity.class));
    }

    public void clickSignout(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out");
        builder.setMessage("Are You sure You want to sign out?");
        builder.setPositiveButton("YES", (dialogInterface, i) -> {
            mAuth.signOut();
            gsc.signOut();
            startActivity(new Intent(MainActivity.this, AuthActivity.class));
            finish();
        });
        builder.setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.show();
    }
}

// makes parallel API requests
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

        String date = i + dates[1] + dates[0];
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference moonPhase = db.document("/moonPhases/fullNewMoons");

        moonPhase.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    if(document.getString(date) == null){
                        makeReq();
                    }
                }
            }
        });

        /* try{
            // Make an API request with volley
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
        } */
    }

    private void makeReq () {
        try{
            // Make an API request with volley
            StringRequest stringRequest = new StringRequest(url, response -> {
                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray responseArray = responseObject.getJSONArray("data");

                    JSONObject moonArray = responseArray.getJSONObject(1);
                    String constValue = moonArray.getString("constellation");

                    labelDay(constValue);

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

    // labels days based on their type
    // add each day in shared prefs? then pass ass intents to day activity
    private void labelDay(String constValue){
        // array needs improvements
        String[] constArray = {"Capricornus","Taurus", "Virgo", "Gemini", "Libra", "Aquarius", "Pisces", "Scorpius", "Cancer", "Ophiuchus", "Aries", "Sagittarius", "Leo"};

        for (int a = 0; a < constArray.length; a++) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Integer.parseInt(dates[0]), Integer.parseInt(dates[1]) - 1, i);

            if (constArray[a].equals(constValue)) {
                if (a <= 2) {
                    events.add(new EventDay(calendar, R.drawable.event_root)); // this icon will get changed
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
