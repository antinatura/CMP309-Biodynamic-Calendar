package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day);

        // get firebase auth and storage instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        String[] dateVals = this.getIntent().getStringArrayExtra("extras"); // get variables from extras

        // get layout elements
        EditText input = findViewById(R.id.notesInputEdit);
        Button submit = findViewById(R.id.submit);

        formatDay(dateVals); // format date TextView and toolbar color
        getNote(dateVals[0] + "-" + dateVals[1] + "-" + dateVals[2]); // get note from database if there is one

        // add note on button click
        submit.setOnClickListener(view -> {
           String note = input.getText().toString();
            if (TextUtils.isEmpty(note)) {
                input.setError(getString(R.string.notes_err));
            } else {
                addNote(dateVals[0] + "-" + dateVals[1] + "-" + dateVals[2], note);
            }
        });
    }

    // retrieve note
    private void getNote(String date){
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        DocumentReference dbNotes = db.document("/users/" + uid + "/notes/" + date);

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
    private void addNote(String date, String note) {
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        DocumentReference dbNotes = db.document("/users/" + uid + "/notes/" + date);
        Map<String, Object> docData = new HashMap<>();
        docData.put("note", note);

        dbNotes.set(docData).addOnSuccessListener(unused ->
                Toast.makeText(this, R.string.note_success, Toast.LENGTH_SHORT).show()).addOnFailureListener(e ->
                    Toast.makeText(this, R.string.note_failure, Toast.LENGTH_SHORT).show());
    }

    // displays date and day type, changed appbar color
    private void formatDay(String[] dateVals) {
        TextView dateView = findViewById(R.id.date);
        String date = dateVals[2] + "/" + dateVals[1] + "/" + dateVals[0];
        dateView.setText(date);

        final TextView dayDesc = findViewById(R.id.dayDesc);
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);
        int dayType = sharedPref.getInt(dateVals[0] + "-" + dateVals[1] + "-" + dateVals[2], -1);
        switch (dayType) {
            case 1:
                dayDesc.setText(R.string.root);
                colorAppbar(dayType);
                break;
            case 2:
                dayDesc.setText(R.string.flower);
                colorAppbar(dayType);
                break;
            case 3:
                dayDesc.setText(R.string.leaf);
                colorAppbar(dayType);
                break;
            case 4:
                dayDesc.setText(R.string.fruit);
                colorAppbar(dayType);
                break;
            default:
                DocumentReference fullNewMoons = db.document("/moonPhases/fullNewMoons");
                fullNewMoons.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String moonPhase = document.getString(dateVals[0] + "-" + dateVals[1] + "-" + dateVals[2]);
                            if (moonPhase != null && moonPhase.equals("Full Moon")){
                                dayDesc.setText(R.string.full_moon);
                            } else if (moonPhase != null && moonPhase.equals("New Moon")) {
                                dayDesc.setText(R.string.new_moon);
                            }
                        }
                    }
                });
        }
    }

    // appbar color based on day type
    private void colorAppbar(int dayType) {
        LinearLayout toolbar = findViewById(R.id.appbar_sub);
        int color = ContextCompat.getColor(this, R.color.green_1);

        switch (dayType) {
            case 1:
                color = ContextCompat.getColor(this, R.color.root);
                break;
            case 2:
                color = ContextCompat.getColor(this, R.color.flower);
                break;
            case 3:
                color = ContextCompat.getColor(this, R.color.leaf);
                break;
            case 4:
                color = ContextCompat.getColor(this, R.color.fruit);
                break;
        }
        toolbar.setBackgroundColor(color);
    }

    // click on the home icon
    public void clickReturn(View view) {
        finish();
    }
}