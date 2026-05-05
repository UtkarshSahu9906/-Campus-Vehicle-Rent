package com.college.vehiclerent.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.college.vehiclerent.OwnerDashboardActivity;
import com.college.vehiclerent.ProfileActivity;
import com.college.vehiclerent.R;
import com.college.vehiclerent.utils.QRUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class CustomerProfileFragment extends Fragment {

    private TextView tvUserName, tvUserEmail, tvTotalRides, tvUserRating, tvUserLevel;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_profile, container, false);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvTotalRides = view.findViewById(R.id.tvTotalRides);
        tvUserRating = view.findViewById(R.id.tvUserRating);
        tvUserLevel = view.findViewById(R.id.tvUserLevel);
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvUserName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Student");
            tvUserEmail.setText(user.getEmail());
            loadStats(user.getUid());
        }

        view.findViewById(R.id.btnShowQR).setOnClickListener(v -> showMyQR());
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), ProfileActivity.class))
        );
        view.findViewById(R.id.btnSwitchToOwner).setOnClickListener(v -> switchRole());
        
        return view;
    }

    private void showMyQR() {
        if (mAuth.getUid() == null) return;
        Bitmap qrCode = QRUtils.generateQRCode(mAuth.getUid(), 500, 500);
        if (qrCode != null) {
            Dialog dialog = new Dialog(getContext());
            dialog.setContentView(R.layout.dialog_qr_code); // Re-use or create if needed
            // If dialog_qr_code doesn't exist, we can create it programmatically or just add it
            
            // For now, let's assume dialog_qr_code exists from CustomerDashboardActivity
            ImageView ivQR = dialog.findViewById(R.id.ivQR);
            if (ivQR != null) {
                ivQR.setImageBitmap(qrCode);
            }
            
            View btnClose = dialog.findViewById(R.id.btnClose);
            if (btnClose != null) {
                btnClose.setOnClickListener(v -> dialog.dismiss());
            }
            
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            
            dialog.show();
        }
    }

    private void loadStats(String uid) {
        db.collection("rental_sessions")
                .whereEqualTo("customerId", uid)
                .whereEqualTo("status", "completed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvTotalRides.setText(String.valueOf(count));
                    
                    // Simple level logic
                    if (count >= 20) tvUserLevel.setText("Platinum");
                    else if (count >= 10) tvUserLevel.setText("Gold");
                    else if (count >= 5) tvUserLevel.setText("Silver");
                    else tvUserLevel.setText("Bronze");
                    
                    // Fixed high rating if they have rides
                    if (count > 0) tvUserRating.setText("5.0");
                    else tvUserRating.setText("New");
                });
    }

    private void switchRole() {
        if (mAuth.getUid() == null) return;
        
        db.collection("users").document(mAuth.getUid()).update("role", "owner")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Switched to Owner Mode", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getContext(), OwnerDashboardActivity.class);
                    // Clear task to prevent back stack loops
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to switch role", Toast.LENGTH_SHORT).show());
    }
}
