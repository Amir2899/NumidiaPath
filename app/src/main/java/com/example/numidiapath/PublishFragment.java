package com.example.numidiapath;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PublishFragment extends Fragment {

    private ImageView imageToPublish;
    private TextView textTapToSelect;
    private TextInputEditText editCaption, editLocation;
    private ChipGroup chipGroupTags;
    private Uri imageUri;
    private Button btnShare;
    private ProgressBar progressBar;
    private FirebaseStorage storage;
    private FirebaseFirestore db;

    private Spinner spinnerGroups;
    private List<String> groupNames = new ArrayList<>();
    private List<String> groupIds = new ArrayList<>();
    private String selectedGroupId = "";

    private double finalLat = 0.0;
    private double finalLon = 0.0;
    private String finalFullAddress = "";
    private List<String> detectedTags = new ArrayList<>();

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    imageUri = uri;
                    imageToPublish.setImageURI(uri);
                    updateUIAfterSelection();
                    runImageLabeling(uri);
                }
            });

    private final ActivityResultLauncher<Intent> takePhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && imageUri != null) {
                    imageToPublish.setImageURI(imageUri);
                    updateUIAfterSelection();
                    runImageLabeling(imageUri);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) openCamera();
                else Toast.makeText(getContext(), "Accès caméra refusé", Toast.LENGTH_SHORT).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_publish, container, false);

        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();

        imageToPublish = view.findViewById(R.id.imageToPublish);
        textTapToSelect = view.findViewById(R.id.textTapToSelect);
        editCaption = view.findViewById(R.id.editCaption);
        editLocation = view.findViewById(R.id.editLocation);
        btnShare = view.findViewById(R.id.btnShare);
        progressBar = view.findViewById(R.id.progressBar);
        chipGroupTags = view.findViewById(R.id.chipGroupTags);
        spinnerGroups = view.findViewById(R.id.spinnerGroups);

        view.findViewById(R.id.cardSelectImage).setOnClickListener(v -> showImagePickerDialog());
        btnShare.setOnClickListener(v -> startPublishingProcess());

        loadUserGroups();

        return view;
    }

    private void loadUserGroups() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        groupNames.clear();
        groupIds.clear();
        groupNames.add("🌍 Mur Public (Tout le monde)");
        groupIds.add("");

        db.collection("groups")
                .whereArrayContains("members", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        groupNames.add("👥 " + doc.getString("groupName"));
                        groupIds.add(doc.getId());
                    }

                    if (isAdded() && getContext() != null) {
                        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_item, groupNames);
                        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerGroups.setAdapter(groupAdapter);
                    }
                });

        spinnerGroups.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedGroupId = groupIds.get(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void runImageLabeling(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(requireContext(), uri);
            ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

            labeler.process(image)
                    .addOnSuccessListener(labels -> {
                        detectedTags.clear();
                        chipGroupTags.removeAllViews();
                        for (ImageLabel label : labels) {
                            if (label.getConfidence() > 0.7f) {
                                String text = label.getText();
                                detectedTags.add(text);
                                addTagToChipGroup(text);
                            }
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "IA: Échec de l'analyse", Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addTagToChipGroup(String tag) {
        if (getContext() == null) return;
        Chip chip = new Chip(getContext());
        chip.setText(tag);
        chip.setCheckable(false);
        chipGroupTags.addView(chip);
    }

    private void showImagePickerDialog() {
        String[] options = {"Prendre une photo", "Choisir dans la galerie", "Annuler"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Sélectionner une image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) checkCameraPermission();
                    else if (which == 1) pickImageLauncher.launch("image/*");
                }).show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        imageUri = requireContext().getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        takePhotoLauncher.launch(intent);
    }

    private void startPublishingProcess() {
        String locationStr = Objects.requireNonNull(editLocation.getText()).toString().trim();
        if (imageUri == null || locationStr.isEmpty()) {
            Toast.makeText(getContext(), "Sélectionnez une image et un lieu", Toast.LENGTH_SHORT).show();
            return;
        }
        setLoading(true);
        convertAddressToCoordinates(locationStr);
    }

    private void convertAddressToCoordinates(String addressName) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(addressName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                finalLat = addr.getLatitude();
                finalLon = addr.getLongitude();
                finalFullAddress = addr.getAddressLine(0);
                uploadPostToFirebase();
            } else {
                Toast.makeText(getContext(), "Lieu introuvable", Toast.LENGTH_SHORT).show();
                setLoading(false);
            }
        } catch (IOException e) {
            setLoading(false);
        }
    }

    private void uploadPostToFirebase() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        StorageReference fileRef = storage.getReference().child("posts/" + System.currentTimeMillis() + ".jpg");

        fileRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            fileRef.getDownloadUrl().addOnSuccessListener(uri -> {

                String postId = db.collection("posts").document().getId();
                String caption = Objects.requireNonNull(editCaption.getText()).toString().trim();

                String displayName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                        ? user.getDisplayName() : user.getEmail().split("@")[0];

                Post newPost = new Post(displayName, finalFullAddress, caption, uri.toString());
                newPost.setPostId(postId);
                newPost.setPublisherId(user.getUid());
                newPost.setLatitude(finalLat);
                newPost.setLongitude(finalLon);
                newPost.setTimestamp(System.currentTimeMillis());
                newPost.setTags(new ArrayList<>(detectedTags));
                newPost.setGroupId(selectedGroupId);

                db.collection("posts").document(postId).set(newPost)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("NOTIF_DEBUG", "Post enregistré avec succès.");
                            FirestoreHelper.incrementPostCount(user.getUid());

                            // Lancer les notifications
                            handleMultipleNotifications(user.getUid(), displayName, selectedGroupId);

                            // Petit délai pour laisser les notifications partir avant de changer de Fragment
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (isAdded()) {
                                    NotificationHelper.showNotification(getContext(), "NumidiaPath", "Publication réussie !");
                                    Toast.makeText(getContext(), "Publié !", Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager().beginTransaction()
                                            .replace(R.id.fragment_container, new ProfileFragment())
                                            .commit();
                                }
                            }, 1000);

                        }).addOnFailureListener(e -> resetUI(e.getMessage()));
            });
        }).addOnFailureListener(e -> resetUI(e.getMessage()));
    }

    private void handleMultipleNotifications(String currentUserId, String senderName, String groupId) {
        Log.d("NOTIF_DEBUG", "Début handleMultipleNotifications pour : " + currentUserId);

        // 1. Notifier les abonnés (Followers)
        db.collection("users").document(currentUserId)
                .collection("followers").get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        Log.d("NOTIF_DEBUG", "Followers trouvés : " + snapshots.size());
                        for (DocumentSnapshot doc : snapshots) {
                            NotificationHelper.sendNotificationToUser(doc.getId(), senderName, "a publié une nouvelle photo");
                        }
                    } else {
                        Log.d("NOTIF_DEBUG", "Aucun follower trouvé.");
                    }
                }).addOnFailureListener(e -> Log.e("NOTIF_DEBUG", "Erreur Followers: " + e.getMessage()));

        // 2. Notifier les membres du groupe
        if (groupId != null && !groupId.isEmpty()) {
            db.collection("groups").document(groupId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    List<String> members = (List<String>) doc.get("members");
                    String groupName = doc.getString("groupName");
                    if (members != null) {
                        for (String memberId : members) {
                            if (!memberId.equals(currentUserId)) {
                                NotificationHelper.sendNotificationToUser(memberId, "Groupe " + groupName, senderName + " a partagé une photo");
                            }
                        }
                    }
                }
            });
        }

        // 3. Notifier pour le lieu
        if (finalFullAddress != null && !finalFullAddress.isEmpty()) {
            db.collection("users").whereArrayContains("followedLocations", finalFullAddress).get()
                    .addOnSuccessListener(snapshots -> {
                        if (snapshots != null && !snapshots.isEmpty()) {
                            Log.d("NOTIF_DEBUG", "Fans de lieu trouvés : " + snapshots.size());
                            for (DocumentSnapshot doc : snapshots) {
                                if (!doc.getId().equals(currentUserId)) {
                                    NotificationHelper.sendNotificationToUser(doc.getId(), "Lieu : " + finalFullAddress, senderName + " a posté ici");
                                }
                            }
                        }
                    });
        }

        // 4. Notifier pour les tags
        if (detectedTags != null && !detectedTags.isEmpty()) {
            for (String tag : detectedTags) {
                db.collection("users").whereArrayContains("followedTags", tag).get()
                        .addOnSuccessListener(snapshots -> {
                            if (snapshots != null && !snapshots.isEmpty()) {
                                Log.d("NOTIF_DEBUG", "Fans du tag #" + tag + " trouvés : " + snapshots.size());
                                for (DocumentSnapshot doc : snapshots) {
                                    if (!doc.getId().equals(currentUserId)) {
                                        NotificationHelper.sendNotificationToUser(doc.getId(), "Tag: #" + tag, senderName + " a posté une photo");
                                    }
                                }
                            }
                        });
            }
        }
    }

    private void updateUIAfterSelection() {
        if (textTapToSelect != null) textTapToSelect.setVisibility(View.GONE);
        imageToPublish.setPadding(0, 0, 0, 0);
    }

    private void setLoading(boolean isLoading) {
        btnShare.setEnabled(!isLoading);
        btnShare.setText(isLoading ? "Analyse & Envoi..." : "Partager");
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void resetUI(String error) {
        setLoading(false);
        if (getContext() != null) {
            Toast.makeText(getContext(), "Erreur : " + error, Toast.LENGTH_SHORT).show();
        }
    }
}