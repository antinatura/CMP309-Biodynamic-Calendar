package uk.ac.abertay.biodynamiccalendar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day);

        // FirebaseFirestore db = FirebaseFirestore.getInstance();

        // get variables from extras
        String[] extras = this.getIntent().getStringArrayExtra("extras");
        String date = extras[2] + extras[1] + extras[0]; //

        EditText input = findViewById(R.id.notesInputEdit);
        Button submit = findViewById(R.id.submit);

        formatDay(extras);

        getNote(date);

        submit.setOnClickListener(view -> {
           String note = input.getText().toString();
            if (TextUtils.isEmpty(note)) {
                input.setError("Field is empty!");
            } else {
                addNote(date, note);
            }
        });
    }

    private void getNote(String dateDoc){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference dbNotes = db.document("..." + dateDoc);

        dbNotes.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    EditText input = findViewById(R.id.notesInputEdit);
                    input.setText(document.getString("note"));
                }
            }
        });
    }

    private void addNote(String dateDoc, String note) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference dbNotes = db.document("..." + dateDoc);
        Map<String, Object> docData = new HashMap<>();
        docData.put("note", note);

        dbNotes.set(docData).addOnSuccessListener(unused ->
                Toast.makeText(DayActivity.this, "Note Added!", Toast.LENGTH_SHORT).show()).addOnFailureListener(e ->
                    Toast.makeText(DayActivity.this, "Something went wrong, please try again: \n" + e, Toast.LENGTH_SHORT).show());
    }

    private void formatDay(String[] extras) {
        // will be passing all the day type stuff as with intents after as the main activity handles API too now
        String iso8601 = extras[0] + extras[1] + extras[2] + "T100000"; // iso8601 timestamp for the API call
        // locations will be used from db
        String latitude = "56.462002";
        String longitude = "-2.970700";

        TextView dateView = findViewById(R.id.date);
        String date = extras[2] + "/" + extras[1] + "/" + extras[0];
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
    }
}