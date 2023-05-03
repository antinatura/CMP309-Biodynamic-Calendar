package uk.ac.abertay.biodynamiccalendar;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

public class NotificationReceiver extends BroadcastReceiver {

    @SuppressLint("MissingPermission") // permissions are checked elsewhere
    @Override
    public void onReceive (Context context, Intent intent){
        AtomicReference<String> text = new AtomicReference<>("");

        // get current date stamp and reformat it to match the shared preferences date stamp format (no leading 0 for date)
        String currentDate = String.valueOf(LocalDate.now());
        currentDate = currentDate.replaceAll("(0)(\\d$)", "$2"); // matches the date value in two capture groups and only uses the second which does not contain the 0

        // get stored daytypes
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPrefs = context.getSharedPreferences("biodynamiccalendar_DAYTYPES", Context.MODE_PRIVATE);

        int dayType = sharedPrefs.getInt(currentDate, -1);

        // set notification text
        switch(dayType) {
            case 1:
                text.set(context.getString(R.string.today_is) + context.getString(R.string.root) + context.getString(R.string.root_emoji));
                break;
            case 2:
                text.set(context.getString(R.string.today_is) + context.getString(R.string.flower) + context.getString(R.string.flower_emoji));
                break;
            case 3:
                text.set(context.getString(R.string.today_is) + context.getString(R.string.leaf) + context.getString(R.string.leaf_emoji));
                break;
            case 4:
                text.set(context.getString(R.string.today_is) + context.getString(R.string.fruit) + context.getString(R.string.fruit_emoji));
                break;
            default:
                DocumentReference fullNewMoons = db.document("/moonPhases/fullNewMoons");
                String finalCurrentDate = currentDate;
                fullNewMoons.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String moonPhase = document.getString(finalCurrentDate);
                            if (moonPhase != null && moonPhase.equals("Full Moon")){
                                text.set(context.getString(R.string.full_moon_notif));
                            } else if (moonPhase != null && moonPhase.equals("New Moon")) {
                                text.set(context.getString(R.string.new_moon_notif));
                            }
                        }
                    }
                });
        }

        // notification intents
        Intent notifIntent = new Intent(context, SplashActivity.class);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notifIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        // build notification
        Notification.Builder builder = new Notification.Builder(context, "DAILY")
                .setContentTitle(context.getString(R.string.notif_title))
                .setContentText(text.get())
                .setSmallIcon(R.drawable.icon_daily)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(111, builder.build()); // notify
    }
}
