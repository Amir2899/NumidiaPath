package com.example.numidiapath;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ReportBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_report_sheet, container, false);

        // On gère le clic pour chaque option
        View.OnClickListener listener = v -> {
            String raison = ((TextView)v).getText().toString();
            Toast.makeText(getContext(), "Merci. Signalé pour : " + raison, Toast.LENGTH_LONG).show();
            dismiss(); // Ferme la fenêtre
        };

        view.findViewById(R.id.reportSpam).setOnClickListener(listener);
        view.findViewById(R.id.reportInappropriate).setOnClickListener(listener);
        view.findViewById(R.id.reportHarassment).setOnClickListener(listener);
        view.findViewById(R.id.reportFalseInfo).setOnClickListener(listener);

        return view;
    }
}