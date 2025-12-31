package com.example.numidiapath;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // 1. Persistance : Si l'utilisateur est déjà reconnu par Firebase
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToMainActivity();
        }

        // Bouton SE CONNECTER / S'INSCRIRE
        findViewById(R.id.btnConnect).setOnClickListener(v -> {
            startActivity(new Intent(this, AuthEmailActivity.class));
        });

        // Bouton EXPLORER SANS COMPTE (Mode Anonyme)
        findViewById(R.id.btnAnonymous).setOnClickListener(v -> loginAnonymously());
    }

    private void loginAnonymously() {
        mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Mode Anonyme Activé", Toast.LENGTH_SHORT).show();
                goToMainActivity();
            } else {
                Toast.makeText(this, "Erreur : " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Empêche de revenir à l'écran de login avec le bouton retour
    }
}