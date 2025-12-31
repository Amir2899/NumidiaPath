package com.example.numidiapath;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        view.findViewById(R.id.btnBackSettings).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Modifier mot de passe
        view.findViewById(R.id.settingChangePassword).setOnClickListener(v -> sendPasswordReset(user));

        // Déconnexion
        view.findViewById(R.id.settingLogout).setOnClickListener(v -> showLogoutDialog());

        // Suppression compte
        view.findViewById(R.id.settingDeleteAccount).setOnClickListener(v -> confirmAccountDeletion(user));
    }

    private void sendPasswordReset(FirebaseUser user) {
        if (user != null) {
            mAuth.sendPasswordResetEmail(user.getEmail())
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "E-mail de réinitialisation envoyé !", Toast.LENGTH_SHORT).show());
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(getActivity(), LoginActivity.class));
                    getActivity().finish();
                }).setNegativeButton("Non", null).show();
    }

    private void confirmAccountDeletion(FirebaseUser user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Action irréversible")
                .setMessage("Supprimer définitivement votre compte et vos données ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    String uid = user.getUid();
                    // Suppression Firestore (Posts + User) puis Auth
                    db.collection("posts").whereEqualTo("publisherId", uid).get().addOnSuccessListener(snapshots -> {
                        for (DocumentSnapshot ds : snapshots) ds.getReference().delete();
                        db.collection("users").document(uid).delete().addOnSuccessListener(aVoid -> {
                            user.delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    startActivity(new Intent(getActivity(), LoginActivity.class));
                                    getActivity().finish();
                                }
                            });
                        });
                    });
                }).setNegativeButton("Annuler", null).show();
    }
}