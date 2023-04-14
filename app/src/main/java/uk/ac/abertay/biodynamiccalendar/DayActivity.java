package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DayActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    public FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // get variables from extras
        String[] extras = this.getIntent().getStringArrayExtra("extras");
        String date = extras[2] + extras[1] + extras[0]; // day id

        // get layout elements
        EditText input = findViewById(R.id.notesInputEdit);
        Button submit = findViewById(R.id.submit);

        formatDay(extras); // format date TextView and toolbar color
        getNote(date); // get note from database if there is one

        // add note on button click
        submit.setOnClickListener(view -> {
           String note = input.getText().toString();
            if (TextUtils.isEmpty(note)) {
                input.setError("Field is empty!");
            } else {
                addNote(date, note);
            }
        });
    }

    // retrieve note
    private void getNote(String dateDoc){
        // FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid(); // ?
        DocumentReference dbNotes = db.document("/users/" + uid + "/notes/" + dateDoc);

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

    // add note
    private void addNote(String dateDoc, String note) {
        // FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        DocumentReference dbNotes = db.document("/users/" + uid + "/notes/" + dateDoc);
        Map<String, Object> docData = new HashMap<>();
        docData.put("note", note);

        dbNotes.set(docData).addOnSuccessListener(unused ->
                Toast.makeText(DayActivity.this, "Note Added!", Toast.LENGTH_SHORT).show()).addOnFailureListener(e ->
                    Toast.makeText(DayActivity.this, "Something went wrong, please try again: \n" + e, Toast.LENGTH_SHORT).show());
    }

    private void formatDay(String[] extras) {
        // will be passing all the day type stuff with intents
        // String iso8601 = extras[0] + extras[1] + extras[2] + "T100000";

        TextView dateView = findViewById(R.id.date);
        String date = extras[2] + "/" + extras[1] + "/" + extras[0];
        dateView.setText(date);

        final TextView constellation = findViewById(R.id.dayType);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);
        int dayType = sharedPref.getInt(extras[2] + extras[1] + extras[0], -1);
        switch (dayType) {
            case 1:
                constellation.setText("Root day");
                colorAppbar(dayType);
                break;
            case 2:
                constellation.setText("Flower day");
                colorAppbar(dayType);
                break;
            case 3:
                constellation.setText("Leaf day");
                colorAppbar(dayType);
                break;
            case 4:
                constellation.setText("Fruit day");
                colorAppbar(dayType);
                break;
            default:
                DocumentReference moonPhase = db.document("/moonPhases/fullNewMoons");
                moonPhase.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            constellation.setText(document.getString(extras[2] + extras[1] + extras[0]));
                        }
                    }
                });
        }

        // will be removed
        /* String latitude = "56.462002";
        String longitude = "-2.970700";
        String url = "https://api.visibleplanets.dev/v3?latitude=" + latitude + "&longitude=" + longitude + "&aboveHorizon=false&time=" + iso8601;
        StringRequest stringRequest = new StringRequest(url, response -> {
            try {
                JSONObject responseObject = new JSONObject(response);
                JSONArray responseArray = responseObject.getJSONArray("data");

                JSONObject moonArray = responseArray.getJSONObject(1);
                String constValue = moonArray.getString("constellation");
                constellation.append(constValue);

                colorAppbar(constValue);

            }  catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest); */
    }

    // toolbar color based on day type
    private void colorAppbar(int dayType) {
        // String[] constArray = {"Capricornus","Taurus", "Virgo", "Gemini", "Libra", "Aquarius", "Pisces", "Scorpius", "Cancer", "Ophiuchus", "Aries", "Sagittarius", "Leo"};
        LinearLayout toolbar = findViewById(R.id.appbar_sub);
        int color;

        /* for (int i = 0; i < constArray.length; i++) {
            if (constArray[i].equals(constValue)) {
                if (i <= 2) {
                    color = ContextCompat.getColor(this, R.color.root);
                } else if (i <= 5) {
                    color = ContextCompat.getColor(this, R.color.flower);
                } else if (i <= 9) {
                    color = ContextCompat.getColor(this, R.color.leaf);
                } else {
                    color = ContextCompat.getColor(this, R.color.fruit);
                }
                toolbar.setBackgroundColor(color);
                break;
            }
        } */
        // cases?
        if (dayType == 1) {
            color = ContextCompat.getColor(this, R.color.root);
        } else if (dayType == 2) {
            color = ContextCompat.getColor(this, R.color.flower);
        } else if (dayType == 3) {
            color = ContextCompat.getColor(this, R.color.leaf);
        } else {
            color = ContextCompat.getColor(this, R.color.fruit);
        }
        toolbar.setBackgroundColor(color);
    }

    // click on the home icon
    public void clickReturn(View view) {
        finish();
    }
}