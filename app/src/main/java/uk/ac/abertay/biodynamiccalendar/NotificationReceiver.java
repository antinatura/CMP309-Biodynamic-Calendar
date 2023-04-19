package uk.ac.abertay.biodynamiccalendar;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

public class NotificationReceiver extends BroadcastReceiver {

    @SuppressLint("MissingPermission") // checks for permissions elsewhere
    @Override
    public void onReceive (Context context, Intent intent){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AtomicReference<String> text = new AtomicReference<>("");
        LocalDate currentDate = LocalDate.now();
        SharedPreferences sharedPrefs = context.getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);
        int dayType = sharedPrefs.getInt(String.valueOf(currentDate), -1);
        switch(dayType) {
            case 1:
                text.set(context.getString(R.string.today_is) + context.getString(R.string.root) + ".");
                break;
            case 2:
                text.set(context.getString(R.string.today_is) + context.getString(R.string.flower) + ".");
                break;
            case 3:
                text.set(context.getString(R.string.today_is) + context.getString(R.string.leaf) + ".");
                break;
            case 4:
                text.set(context.getString(R.string.today_is) + context.getString(R.string.fruit) + ".");
                break;
            default:
                DocumentReference fullNewMoons = db.document("/moonPhases/fullNewMoons");
                fullNewMoons.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String moonPhase = document.getString(String.valueOf(currentDate));
                            if (moonPhase != null && moonPhase.equals("Full Moon")){
                                text.set(context.getString(R.string.full_moon_notif));
                            } else if (moonPhase != null && moonPhase.equals("New Moon")) {
                                text.set(context.getString(R.string.new_moon_notif));
                            }
                        }
                    }
                });
        }

        // add launcher intents?
        Intent notificationIntent = new Intent(context, SplashActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "DAILY")
                .setContentTitle(context.getString(R.string.notif_title))
                .setContentText(text.get())
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(111, builder.build());
    }
}
