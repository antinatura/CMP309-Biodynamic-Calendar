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

        TextView aboutTextView = findViewById(R.id.about_text);

        aboutTextView.setText(Html.fromHtml(getString(R.string.about)));
    }

    public void clickReturn(View view) {
        finish();
    }
}