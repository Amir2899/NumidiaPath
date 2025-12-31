package com.example.numidiapath;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateGroupFragment extends Fragment {

    private TextInputEditText editGroupName, editGroupDesc;
    private Button btnCreate;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create_group, container, false);

        db = FirebaseFirestore.getInstance();
        editGroupName = view.findViewById(R.id.editGroupName);
        editGroupDesc = view.findViewById(R.id.editGroupDesc);
        btnCreate = view.findViewById(R.id.btnConfirmCreateGroup);

        btnCreate.setOnClickListener(v -> createGroupLogic());

        return view;
    }

    private void createGroupLogic() {
        String name = editGroupName.getText().toString().trim();
        String desc = editGroupDesc.getText().toString().trim();
        String adminId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Le nom est obligatoire", Toast.LENGTH_SHORT).show();
            return;
        }

        String groupId = db.collection("groups").document().getId();
        Group newGroup = new Group(name, desc, adminId);
        newGroup.setGroupId(groupId);

        db.collection("groups").document(groupId).set(newGroup)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Groupe créé avec succès !", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}