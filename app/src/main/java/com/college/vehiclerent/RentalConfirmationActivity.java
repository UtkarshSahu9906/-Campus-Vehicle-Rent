package com.college.vehiclerent;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RentalConfirmationActivity extends AppCompatActivity {

    private String sessionId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_confirmation);

        db = FirebaseFirestore.getInstance();
        sessionId = getIntent().getStringExtra("sessionId");
        String vehicleName = getIntent().getStringExtra("vehicleName");
        String ownerName = getIntent().getStringExtra("ownerName");
        double rateHour = getIntent().getDoubleExtra("rateHour", 0);
        double rateDay = getIntent().getDoubleExtra("rateDay", 0);

        TextView tvVehicleName = findViewById(R.id.tvVehicleName);
        TextView tvOwnerName = findViewById(R.id.tvOwnerName);
        TextView tvRate = findViewById(R.id.tvRate);
        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);
        MaterialButton btnDecline = findViewById(R.id.btnDecline);

        tvVehicleName.setText(vehicleName);
        tvOwnerName.setText("Owner: " + ownerName);
        tvRate.setText(String.format("Rate: ₹%.0f/hr | ₹%.0f/day", rateHour, rateDay));

        btnConfirm.setOnClickListener(v -> confirmRental());
        btnDecline.setOnClickListener(v -> declineRental());
    }

    private void confirmRental() {
        String uid = FirebaseAuth.getInstance().getUid();
        String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (name == null) name = "Student";

        db.collection("rental_sessions").document(sessionId)
                .update("status", "active",
                        "startTime", System.currentTimeMillis(),
                        "customerName", name)
                .addOnSuccessListener(a -> {
                    // Mark vehicle as unavailable
                    db.collection("rental_sessions").document(sessionId).get()
                            .addOnSuccessListener(doc -> {
                                String vId = doc.getString("vehicleId");
                                if (vId != null) {
                                    db.collection("vehicles").document(vId).update("available", false);
                                }
                            });
                    
                    Toast.makeText(this, "Ride Started!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ActiveRentalActivity.class);
                    intent.putExtra("sessionId", sessionId);
                    intent.putExtra("userRole", "customer");
                    startActivity(intent);
                    finish();
                });
    }

    private void declineRental() {
        db.collection("rental_sessions").document(sessionId).delete()
            .addOnSuccessListener(a -> {
                Toast.makeText(this, "Rental Declined.", Toast.LENGTH_SHORT).show();
                finish();
            });
    }
}
