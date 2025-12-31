package com.example.numidiapath;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class AuthEmailActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editPassword, editFirstName, editLastName, editUsername, editBirthDate;
    private TextInputLayout layoutFirstName, layoutLastName, layoutUsername, layoutBirthDate;
    private MaterialButton btnMainAction;
    private TextView txtSwitchMode, txtForgotPassword, subTitle;
    private boolean isLoginMode = true;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_email);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Liaison des vues existantes et nouvelles
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);

        // Nouveaux champs pros
        editFirstName = findViewById(R.id.editFirstName);
        editLastName = findViewById(R.id.editLastName);
        editUsername = findViewById(R.id.editUsername);
        editBirthDate = findViewById(R.id.editBirthDate);

        layoutFirstName = findViewById(R.id.layoutFirstName);
        layoutLastName = findViewById(R.id.layoutLastName);
        layoutUsername = findViewById(R.id.layoutUsername);
        layoutBirthDate = findViewById(R.id.layoutBirthDate);

        btnMainAction = findViewById(R.id.btnMainAction);
        txtSwitchMode = findViewById(R.id.txtSwitchMode);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);
        subTitle = findViewById(R.id.subTitle);

        // Gestion du calendrier pour la date de naissance
        editBirthDate.setOnClickListener(v -> showDatePicker());
        editBirthDate.setFocusable(false); // Empêche le clavier de s'ouvrir

        txtSwitchMode.setOnClickListener(v -> toggleMode());
        btnMainAction.setOnClickListener(v -> handleAuth());
        txtForgotPassword.setOnClickListener(v -> handleResetPassword());
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR) - 20; // Par défaut sur 20 ans en arrière
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1;
                    editBirthDate.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        int visibility = isLoginMode ? View.GONE : View.VISIBLE;

        subTitle.setText(isLoginMode ? "CONNECTEZ-VOUS" : "CRÉER UN COMPTE");
        btnMainAction.setText(isLoginMode ? "SE CONNECTER" : "S'INSCRIRE MAINTENANT");
        txtSwitchMode.setText(isLoginMode ? "Pas de compte ? Inscrivez-vous ici" : "Déjà un compte ? Connectez-vous");

        // Basculer la visibilité de TOUS les champs pros
        layoutFirstName.setVisibility(visibility);
        layoutLastName.setVisibility(visibility);
        layoutUsername.setVisibility(visibility);
        layoutBirthDate.setVisibility(visibility);

        txtForgotPassword.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
    }

    private void handleAuth() {
        String email = editEmail.getText().toString().trim();
        String pass = editPassword.getText().toString().trim();

        if (email.isEmpty() || pass.length() < 6) {
            Toast.makeText(this, "Email valide et 6 caractères min requis", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLoginMode) {
            mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) goToMain();
                else Toast.makeText(this, "Erreur: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            });
        } else {
            // INSCRIPTION PROFESSIONNELLE
            String fname = editFirstName.getText().toString().trim();
            String lname = editLastName.getText().toString().trim();
            String uname = editUsername.getText().toString().trim();
            String bDate = editBirthDate.getText().toString().trim();

            if (fname.isEmpty() || lname.isEmpty() || uname.isEmpty() || bDate.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        saveUserToFirestore(firebaseUser.getUid(), fname, lname, uname, email, bDate);
                    }
                } else {
                    Toast.makeText(this, "Erreur: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void saveUserToFirestore(String uid, String fname, String lname, String uname, String email, String bDate) {
        // Création de l'objet User (celui que nous avons mis à jour ensemble)
        User newUser = new User(uid, fname, lname, uname, email, bDate, "", "Passionné de voyages.");

        db.collection("users").document(uid).set(newUser)
                .addOnSuccessListener(aVoid -> {
                    // Mise à jour du DisplayName pour Firebase Auth
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(uname).build();

                    mAuth.getCurrentUser().updateProfile(profileUpdates).addOnCompleteListener(task -> goToMain());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // handleResetPassword et goToMain restent identiques...
    private void handleResetPassword() {
        String email = editEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Entrez votre email", Toast.LENGTH_SHORT).show();
            return;
        }
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) Toast.makeText(this, "Lien envoyé !", Toast.LENGTH_SHORT).show();
        });
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}