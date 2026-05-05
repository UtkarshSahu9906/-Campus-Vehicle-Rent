package com.college.vehiclerent.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.college.vehiclerent.CustomerDashboardActivity;
import com.college.vehiclerent.ProfileActivity;
import com.college.vehiclerent.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class OwnerProfileFragment extends Fragment {

    private TextView tvUserName, tvUserEmail;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_profile, container, false);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Owner");
            tvUserEmail.setText(user.getEmail());
        }

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), ProfileActivity.class))
        );

        view.findViewById(R.id.btnSwitchToCustomer).setOnClickListener(v -> switchRole());
        
        return view;
    }

    private void switchRole() {
        if (mAuth.getUid() == null) return;
        
        db.collection("users").document(mAuth.getUid()).update("role", "customer")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Switched to Customer Mode", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getContext(), CustomerDashboardActivity.class);
                    // Clear task to prevent back stack loops
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to switch role", Toast.LENGTH_SHORT).show());
    }
}
