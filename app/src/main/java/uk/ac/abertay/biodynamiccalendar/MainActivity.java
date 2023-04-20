package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        // google and firebase authentication
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();
        gsc = GoogleSignIn.getClient(this, gso);
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

        // moon phase document from firestore
        db = FirebaseFirestore.getInstance();
        fullNewMoons = db.document("/moonPhases/fullNewMoons");

        // get layout elements
        drawerLayout = findViewById(R.id.main_layout);
        CalendarView calendarView = findViewById(R.id.calendarView);
        ImageView icon = findViewById(R.id.userIcon);
        TextView email = findViewById(R.id.userEmail);
        SwitchCompat notifSwitch = findViewById(R.id.notifSwitch);
        Spinner spinner = findViewById(R.id.language_spinner);

        // populate profile picture and email with current user information
        if (acct != null) {
            String imgUrl = String.valueOf(acct.getPhotoUrl());
            imgUrl = imgUrl.replace("s96-c", "s192-c");
            Picasso.get().load(imgUrl).into(icon);
            String userEmail = acct.getEmail();
            email.setText(userEmail);
        }

        // apply the saved notification switch state
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences("biodynamiccalendar_APPSETTINGS", Context.MODE_PRIVATE);
        notifSwitch.setChecked(sharedPrefs.getBoolean("notifstate",false));

        // finalise the spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.languages_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);

        // called on spinner selection
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String lang = adapterView.getItemAtPosition(i).toString();

                if (lang.equals("English")) {
                    setLocale(MainActivity.this, "en");
                    startActivity(getIntent());
                    finish();
                } else if (lang.equals("Latviešu")) {
                    setLocale(MainActivity.this, "lv");
                    startActivity(getIntent());
                    finish();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        setLimits(calendarView); // set minimum and maximum date for calendarView

        boolean rewrite = this.getIntent().getBooleanExtra("rewrite", false);
        List<EventDay> events = Collections.synchronizedList(new ArrayList<>()); // initialise array list to store cell labels

        if (rewrite) {
            Calendar currCal = Calendar.getInstance(); // get current date
            parseMonth(currCal, events); // parse the current month
            // parse the next month
            currCal.add(Calendar.MONTH, 1);
            parseMonth(currCal, events);
        } else {
            getEvents(events);
        }

        calendarView.setEvents(events); // add cell labels to calendar

        calendarView.setOnDayClickListener(this::launchDay); // when a specific day is tapped, open a page for it
        notifSwitch.setOnCheckedChangeListener(this::onSwitchChange); // called when notification switch changes state
    }

    // set minimum and maximum date for calendar
    private void setLimits(CalendarView calendarView) {
        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();
        // limits are half a year before and after the current date
        min.add(Calendar.MONTH, - 6);
        max.add(Calendar.MONTH, 6);
        calendarView.setMinimumDate(min);
        calendarView.setMaximumDate(max);
    }

    // starts threads that will get day types for all days of a month
    private void parseMonth(Calendar calendar, List<EventDay> events) {
        // get location
        String latitude = "56.462002";
        String longitude = "-2.970700";

        String[] dates = formatDate(calendar);
        YearMonth yearMonthObject = YearMonth.of(Integer.parseInt(dates[0]), Integer.parseInt(dates[1])); // month object to determine how many days are in it

        for (int i = 1; i <= yearMonthObject.lengthOfMonth(); i++) {
            int finalI = i;
            String iso8601 = dates[0] + dates[1] + i + "T100000"; // iso8601 timestamp for API request. set to 10am
            String url = "https://api.visibleplanets.dev/v3?latitude=" + latitude + "&longitude=" + longitude + "&aboveHorizon=false&time=" + iso8601;

            // check if day id exists in full/new moon document. if not, make an API request
            fullNewMoons.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists() && document.getString(dates[0] + "-" + dates[1] + "-" + finalI) == null) {
                        try {
                            LabelDay newLabel = new LabelDay(url, finalI, dates, events, this.getApplicationContext(), this);
                            new Thread(newLabel).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    // gets events from shared preferences
    private void getEvents(List<EventDay> events) {
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPrefs.getAll();
        allEntries.remove("written");

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String[] date = entry.getKey().split("-");
            Calendar calendar = Calendar.getInstance();
            calendar.set(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]));
            int val =(int) entry.getValue();
            labelCell(events, calendar, val);
        }
    }

   // launch clicked day
    private void launchDay(EventDay eventDay) {
        Date clickedDay = eventDay.getCalendar().getTime(); // get clicked day's calendar values
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(clickedDay);
        Intent intent = new Intent(MainActivity.this, DayActivity.class);
        intent.putExtra("extras",  formatDate(calendar)); // passes day, month, year to DayActivity
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

    // notification switch
    public void onSwitchChange (CompoundButton buttonView, boolean isChecked) {
        // notification channel
        NotificationChannel channel = new NotificationChannel("DAILY", "Day types", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Channel for daily day type notifications");

        // create intents
        Intent intent = new Intent(MainActivity.this, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences("biodynamiccalendar_APPSETTINGS", Context.MODE_PRIVATE); // stores switch state

        if (isChecked) {
            // check if app can send notifications
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                Toast.makeText(this, R.string.notif_alert, Toast.LENGTH_SHORT).show();
                SwitchCompat notifSwitch = findViewById(R.id.notifSwitch);
                notifSwitch.setChecked(sharedPrefs.getBoolean("notifstate",false));
                return;
            }

            sharedPrefs.edit().putBoolean("notifstate", true).apply();
            Calendar calendar = Calendar.getInstance();
            // check if time has already passed
            if (calendar.get(Calendar.HOUR_OF_DAY) >= 10) {
                calendar.add(Calendar.DATE,1); // add a day to the calendar
            }

            // set to 10am
            calendar.set(Calendar.HOUR_OF_DAY, 10);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent); // set repeating notifications

        } else {
            sharedPrefs.edit().putBoolean("notifstate", false).apply();
            if(alarmManager != null)
                alarmManager.cancel(pendingIntent); // cancel notifications
        }
    }

    // sign out current user
    public void clickSignout(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sign_out);
        builder.setMessage(R.string.sign_out_body);
        builder.setNegativeButton(R.string.negative, (dialogInterface, i) -> dialogInterface.dismiss());
        builder.setPositiveButton(R.string.positive, (dialogInterface, i) -> {
            mAuth.signOut();
            gsc.signOut();
            startActivity(new Intent(MainActivity.this, AuthActivity.class));
            finish();
        });
        builder.show();
    }

    // change app language
    static void setLocale(Activity activity, String lang) {
        Locale locale = new Locale(lang);
        // save locale
        Locale.setDefault(locale);
        Resources resources = activity.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    // date from calendar format function
    // note: sometimes needs to be recast to int or subtract 1 from month, but generally makes it easier to work with dates
    static String[] formatDate(Calendar calendar) {
        String year = String.valueOf(calendar.get(Calendar.YEAR));
        String month;

        if (calendar.get(Calendar.MONTH) < 9) {
            //  prefix with 0 if month is a single digit
            month = "0" + (calendar.get(Calendar.MONTH) + 1);
        } else {
            month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
        }
        String day = String.valueOf(calendar.get(Calendar.DATE));

        return new String[] {year, month, day};
    }

    // label cells
    static void labelCell(List<EventDay> events, Calendar calendar, int daytype) {
        switch (daytype) {
            case 1:
                events.add(new EventDay(calendar, R.drawable.event_root));
                break;
            case 2:
                events.add(new EventDay(calendar, R.drawable.event_flower));
                break;
            case 3:
                events.add(new EventDay(calendar, R.drawable.event_leaf));
                break;
            case 4:
                events.add(new EventDay(calendar, R.drawable.event_fruit));
                break;
        }
    }
}
