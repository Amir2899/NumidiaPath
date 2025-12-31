package com.example.numidiapath;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {

    private static final String CHANNEL_ID = "numidia_notifications";
    private static final String CHANNEL_NAME = "NumidiaPath Alerts";

    // URL OBLIGATOIRE pour le serveur Europe (Belgique)
    private static final String DB_URL = "https://numidiapath-e8320-default-rtdb.europe-west1.firebasedatabase.app";

    public static final String TYPE_LIKE = "a aimé votre photo";
    public static final String TYPE_COMMENT = "a commenté : ";
    public static final String TYPE_FOLLOW = "vient de s'abonner";

    /**
     * Affiche l'alerte système (le bandeau blanc en haut de l'écran)
     */
    public static void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Vérifie que cette icône existe
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    /**
     * Enregistre la notification dans la REALTIME DATABASE avec l'heure réelle du serveur.
     */
    public static void sendNotificationToUser(String receiverId, String senderName, String actionText) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();

        // Sécurité : Ne pas s'envoyer de notification à soi-même
        if (receiverId.equals(currentUserId)) return;

        // Connexion à la base de données Européenne
        DatabaseReference reference = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Notifications")
                .child(receiverId);

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userid", currentUserId);
        notificationData.put("text", senderName + " " + actionText);
        notificationData.put("ispost", true);

        // C'est ici qu'on envoie l'heure réelle (TIMESTAMP Firebase)
        notificationData.put("timestamp", ServerValue.TIMESTAMP);

        reference.push().setValue(notificationData)
                .addOnSuccessListener(aVoid -> Log.d("NotificationHelper", "Succès : Notification enregistrée avec heure réelle"))
                .addOnFailureListener(e -> Log.e("NotificationHelper", "Erreur d'envoi : " + e.getMessage()));
    }
}