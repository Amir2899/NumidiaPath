package com.example.numidiapath;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText editName, editBio;
    private ImageView imagePreview;
    private Uri selectedImageUri;
    private OnProfileUpdateListener listener;

    public EditProfileBottomSheet(OnProfileUpdateListener listener) {
        this.listener = listener;
    }

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(imagePreview);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_edit_profile, container, false);

        editName = view.findViewById(R.id.editProfileUsername);
        editBio = view.findViewById(R.id.editProfileBio);
        imagePreview = view.findViewById(R.id.editImagePreview);
        Button btnSave = view.findViewById(R.id.btnSaveProfile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            editName.setText(user.getDisplayName());
            if (user.getPhotoUrl() != null) Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(imagePreview);
        }

        view.findViewById(R.id.btnChangePhoto).setOnClickListener(v -> pickImage.launch("image/*"));
        btnSave.setOnClickListener(v -> saveChanges());

        return view;
    }

    private void saveChanges() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String name = editName.getText().toString();
        String bio = editBio.getText().toString();

        if (selectedImageUri != null) {
            // Upload photo puis mise à jour profil
            StorageReference ref = FirebaseStorage.getInstance().getReference().child("profiles/" + user.getUid() + ".jpg");
            ref.putFile(selectedImageUri).addOnSuccessListener(task -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                updateProfile(user, name, bio, uri);
            }));
        } else {
            updateProfile(user, name, bio, user.getPhotoUrl());
        }
    }

    private void updateProfile(FirebaseUser user, String name, String bio, Uri photoUri) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .setPhotoUri(photoUri)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .update("username", name, "bio", bio, "profileImageUrl", photoUri != null ? photoUri.toString() : "");

            listener.onUpdate(name, bio, photoUri);
            dismiss();
        });
    }
}