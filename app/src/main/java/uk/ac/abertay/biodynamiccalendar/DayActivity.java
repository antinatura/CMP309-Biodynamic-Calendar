package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day);

        // get variables from extras
        String[] extras = this.getIntent().getStringArrayExtra("extras");
        String date = extras[2] + "/" + extras[1] + "/" + extras[0];

        // will be passing all the day type stuff as with intents after as the main activity handles API too now
        String iso8601 = extras[0] + extras[1] + extras[2] + "T000000"; // iso8601 timestamp for the API call
        // locations will be used from db
        String latitude = "56.462002";
        String longitude = "-2.970700";

        // show selected date
        TextView dateView = findViewById(R.id.date);
        dateView.setText(date);

        final TextView constellation = findViewById(R.id.response);

        // API query
        String url = "https://api.visibleplanets.dev/v3?latitude=" + latitude + "&longitude=" + longitude + "&aboveHorizon=false&time=" + iso8601;

        // Make API request
        StringRequest stringRequest = new StringRequest(url, response -> {
            try {
                JSONObject responseObject = new JSONObject(response);
                JSONArray responseArray = responseObject.getJSONArray("data");

                JSONObject moonArray = responseArray.getJSONObject(1);
                String constValue = moonArray.getString("constellation");
                constellation.append(constValue);

                // basic testing code, this will move
                // make a 2d array with the day types corresponding to constellations?
                String[] constArray = {"Capricornus","Taurus", "Virgo", "Gemini", "Libra", "Aquarius", "Pisces", "Scorpius", "Cancer", "Ophiuchus", "Aries", "Sagittarius", "Leo"};
                ActionBar actionBar = getSupportActionBar();
                ColorDrawable colorDrawable;

                for (int i = 0; i < constArray.length; i++) {
                    if (constArray[i].equals(constValue)) {
                        // decide on colors and put them in colors.xml
                        if (i <= 2) {
                            colorDrawable = new ColorDrawable(this.getColor(R.color.root));
                        } else if (i <= 5) {
                            colorDrawable = new ColorDrawable(this.getColor(R.color.flower));
                        } else if (i <= 9) {
                            colorDrawable = new ColorDrawable(this.getColor(R.color.leaf));
                        } else {
                            colorDrawable = new ColorDrawable(this.getColor(R.color.fruit));
                        }
                        if (actionBar != null) {
                            actionBar.setBackgroundDrawable(colorDrawable);
                        }
                        break;
                    }
                }

            }  catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            // add error stuff
        });

        // Add the request to the RequestQueue
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);

        // FirebaseFirestore db = FirebaseFirestore.getInstance();
    }
}