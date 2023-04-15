package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
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
    FirebaseFirestore db;
    DocumentReference fullNewMoons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // same as auth activity
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();
        gsc = GoogleSignIn.getClient(this, gso);
        // get google profile information
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        // moon phase document from firestore
        db = FirebaseFirestore.getInstance();
        fullNewMoons = db.document("/moonPhases/fullNewMoons");

        // get layout elements
        drawerLayout = findViewById(R.id.main_layout);
        CalendarView calendarView = findViewById(R.id.calendarView);
        ImageView icon = findViewById(R.id.userIcon);
        TextView email = findViewById(R.id.userEmail);

        // populate profile picture and email with current user information
        if (acct != null) {
            String imgUrl = String.valueOf(acct.getPhotoUrl());
            imgUrl = imgUrl.replace("s96-c", "s192-c");
            Picasso.get().load(imgUrl).into(icon);
            String userEmail = acct.getEmail();
            email.setText(userEmail);
        }

        setLimits(calendarView); // set minimum and maximum date for calendarView

        Calendar currCal = Calendar.getInstance(); // get current date
        List<EventDay> events = Collections.synchronizedList(new ArrayList<>()); // initialise array list to store cell labels

        parseMonth(currCal, events); // parse the current month

        // parse the next month (current and next month are needed for the app)
        currCal.add(Calendar.MONTH, 1);
        parseMonth(currCal, events);

        // events wont log for some reason
        calendarView.setEvents(events); // add cell labels to calendar
        drawerLayout.invalidate();

        // when a specific day is tapped, open a page for it
        calendarView.setOnDayClickListener(this::launchDay);
    }

    // set minimum and maximum date for calendar
    // this size is very small but improves performance and a large timespan is not needed for the app
    private void setLimits(CalendarView calendarView) {
        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();
        // limits are half a year before and after the current date
        min.add(Calendar.MONTH, - 6);
        max.add(Calendar.MONTH, 6);
        calendarView.setMinimumDate(min);
        calendarView.setMaximumDate(max);
    }

    // starts runnables that will get day types for all days of a month
    private void parseMonth(Calendar calendar, List<EventDay> events) {
        // getting Location makes the app slightly more accurate however it is not detrimental to the apps functionality
        // in the future getting a rough user location would be good
        String latitude = "56.462002";
        String longitude = "-2.970700";

        String[] dates = formatDate(calendar);
        YearMonth yearMonthObject = YearMonth.of(Integer.parseInt(dates[0]), Integer.parseInt(dates[1])); // month object to determine how many days in it
        for (int i = 1; i <= yearMonthObject.lengthOfMonth(); i++) {
            int finalI = i;
            String iso8601 = dates[0] + dates[1] + i + "T100000"; // iso8601 timestamp for API request. set to 10am
            String url = "https://api.visibleplanets.dev/v3?latitude=" + latitude + "&longitude=" + longitude + "&aboveHorizon=false&time=" + iso8601;

            // check if day id exists in full/new moon document. if not, make an API request
            fullNewMoons.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        if(document.getString(dates[0] + "-" + dates[1] + "-" + finalI) == null){
                            try {
                                Request request = new Request(url, finalI, dates, events, this.getApplicationContext());
                                new Thread(request).start();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    }

   // launch clicked day
    private void launchDay(EventDay eventDay) {
        // get clicked day's calendar values
        Date clickedDay = eventDay.getCalendar().getTime();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(clickedDay);

        Intent intent = new Intent(MainActivity.this, DayActivity.class);
        // passes day, month, year to DayActivity
        intent.putExtra("extras",  formatDate(calendar));
        startActivity(intent);
    }

    // open navigation drawer
    public void openMenu(View view) {
        drawerLayout.openDrawer(GravityCompat.START);
    }
    // close navigation drawer
    public void closeMenu(View view) {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    // launch about activity
    public void clickHelp(View view) {
        startActivity(new Intent(MainActivity.this, AboutActivity.class));
    }

    // sign out current user
    public void clickSignout(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out");
        builder.setMessage("Are You sure You want to sign out?");
        builder.setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setPositiveButton("YES", (dialogInterface, i) -> {
            mAuth.signOut();
            gsc.signOut();
            startActivity(new Intent(MainActivity.this, AuthActivity.class));
            finish();
        });
        builder.show();
    }

    // date format function
    // mostly used to make life easier and prefix months before october with 0 and start them from 1
    // this is not possible with using the returned calendar values so complicates making API requests (among other things)
    private String[] formatDate(Calendar calendar) {
        // get values and format to strings (for displaying easier and API query formatting)
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month;
        if (calendar.get(Calendar.MONTH) < 9) {
            //  prefix with 0 if month is a single digit (also for API query formatting)
            month = "0" + (calendar.get(Calendar.MONTH) + 1);
        } else {
            month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        }
        String day = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));

        return new String[] {year, month, day};
    }
}

// makes an API request and parses response
class Request implements Runnable {
    String url;
    int i;
    String[] dates;
    List<EventDay> events;
    Context context;
    Request (String url, int i, String[] dates, List<EventDay> events, Context context) {
        this.url = url;
        this.i = i;
        this.dates = dates;
        this.events = events;
        this.context = context;
    }
    @Override
    public void run() {
        try{
            // make a request to visibleplanets API with volley
            StringRequest stringRequest = new StringRequest(url, response -> {
                try {
                    JSONObject responseObject = new JSONObject(response);
                    JSONArray responseArray = responseObject.getJSONArray("data");
                    JSONObject moonArray = responseArray.getJSONObject(1);
                    String constValue = moonArray.getString("constellation");
                    parseResponse(constValue);

                }  catch (JSONException e) {
                    e.printStackTrace();
                }
            }, error -> {
            });

            // add the request to the RequestQueue
            RequestQueue requestQueue = Volley.newRequestQueue(context);
            requestQueue.add(stringRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
        // add error stuff
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
                    events.add(new EventDay(calendar, R.drawable.event_root));
                    writeToPrefs(1);
                } else if (a <= 5) {
                    events.add(new EventDay(calendar, R.drawable.event_flower));
                    writeToPrefs(2);
                } else if (a <= 9) {
                    events.add(new EventDay(calendar, R.drawable.event_leaf));
                    writeToPrefs(3);
                } else {
                    events.add(new EventDay(calendar, R.drawable.event_fruit));
                    writeToPrefs(4);
                }
                return;
            }
        }
    }

    // writes day type to shared preferences
    private void writeToPrefs(int type) {
        SharedPreferences sharedPrefs = context.getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);
        // maybe make it check the timestamp too
        if (!sharedPrefs.contains(dates[0] + "-" + dates[1] + "-" + i)) {
            sharedPrefs.edit().putInt(dates[0] + "-" + dates[1] + "-" + i, type).commit();
        }
    }
}
