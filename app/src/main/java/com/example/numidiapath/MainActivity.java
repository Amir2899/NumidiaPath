package com.example.numidiapath;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseReference notificationRef;
    private ChildEventListener notificationListener;
    private ValueEventListener badgeListener;
    private View notificationBadge;

    private static final String DB_URL = "https://numidiapath-e8320-default-rtdb.europe-west1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        checkAndRequestPermissions();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            FirebaseUser currentUser = mAuth.getCurrentUser();
            boolean isRestricted = (currentUser == null || currentUser.isAnonymous());

            if (id == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (id == R.id.nav_explore) selectedFragment = new ExploreFragment();
            else if (id == R.id.nav_publish) {
                if (isRestricted) { showLoginRequiredDialog("publier vos photos"); return false; }
                selectedFragment = new PublishFragment();
            } else if (id == R.id.nav_favorites) {
                if (isRestricted) { showLoginRequiredDialog("gérer vos favoris"); return false; }
                selectedFragment = new FavoritesFragment();
            } else if (id == R.id.nav_profile) {
                if (isRestricted) { showLoginRequiredDialog("accéder à votre profil"); return false; }
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        startNotificationListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);

        MenuItem notifItem = menu.findItem(R.id.action_notifications);
        if (notifItem != null) {
            View actionView = notifItem.getActionView();

            notificationBadge = actionView.findViewById(R.id.notification_badge);

            actionView.setOnClickListener(v -> {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new NotificationFragment())
                        .addToBackStack(null)
                        .commit();
            });

            setupBadgeRealtimeUpdate();
        }
        return true;
    }

    private void setupBadgeRealtimeUpdate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || notificationBadge == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Notifications")
                .child(currentUser.getUid());

        badgeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (notificationBadge == null) return;

                boolean hasUnreadNotifications = false;
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Notification notification = snapshot.getValue(Notification.class);
                    if (notification != null && !notification.isRead()) {
                        hasUnreadNotifications = true;
                        break; 
                    }
                }

                if (hasUnreadNotifications) {
                    notificationBadge.setVisibility(View.VISIBLE);
                } else {
                    notificationBadge.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        ref.addValueEventListener(badgeListener);
    }

    private void startNotificationListener() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.isAnonymous()) return;

        notificationRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("Notifications").child(currentUser.getUid());

        notificationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                Notification notification = snapshot.getValue(Notification.class);
                if (notification != null && notification.getText() != null && !notification.isRead()) {
                    NotificationHelper.showNotification(MainActivity.this, "NumidiaPath", notification.getText());
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        notificationRef.addChildEventListener(notificationListener);
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!permissionsNeeded.isEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toArray(new String[0]));
        }
    }

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean denied = false;
                for (Boolean granted : result.values()) { if (!granted) denied = true; }
                if (denied) Toast.makeText(this, "Fonctionnalités limitées sans permissions.", Toast.LENGTH_LONG).show();
            });

    private void showLoginRequiredDialog(String action) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Connexion requise")
                .setMessage("Vous devez être connecté pour " + action + ".")
                .setPositiveButton("Se connecter", (dialog, which) -> startActivity(new Intent(MainActivity.this, LoginActivity.class)))
                .setNegativeButton("Annuler", null).show();
    }

    @Override
    public void onBackPressed() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav.getSelectedItemId() != R.id.nav_home) bottomNav.setSelectedItemId(R.id.nav_home);
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationRef != null && notificationListener != null) notificationRef.removeEventListener(notificationListener);
        if (notificationRef != null && badgeListener != null) notificationRef.removeEventListener(badgeListener);
    }
}