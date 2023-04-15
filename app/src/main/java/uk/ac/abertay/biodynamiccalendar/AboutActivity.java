package uk.ac.abertay.biodynamiccalendar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // get TextView and set text to HTML formatted about string
        TextView aboutTextView = findViewById(R.id.about);
        aboutTextView.setText(Html.fromHtml(getString(R.string.about)));
    }

    // return home if home button is clicked
    public void clickReturn(View view) {
        finish();
    }
}