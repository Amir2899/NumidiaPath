package com.example.numidiapath;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ChangePasswordSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_change_password, container, false);

        Button btnConfirm = view.findViewById(R.id.btnConfirmPassword);
        btnConfirm.setOnClickListener(v -> {
            // Ici tu ajouteras ta logique Firebase ou Base de données plus tard
            Toast.makeText(getContext(), "Mot de passe modifié avec succès", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        return view;
    }
}