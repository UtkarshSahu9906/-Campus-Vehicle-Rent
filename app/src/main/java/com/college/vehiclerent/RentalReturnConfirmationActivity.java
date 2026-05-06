package com.college.vehiclerent;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class RentalReturnConfirmationActivity extends AppCompatActivity {

    private String sessionId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_return_confirmation);

        db = FirebaseFirestore.getInstance();
        sessionId = getIntent().getStringExtra("sessionId");
        String vehicleName = getIntent().getStringExtra("vehicleName");
        double totalCost = getIntent().getDoubleExtra("totalCost", 0.0);

        TextView tvVehicleName = findViewById(R.id.tvVehicleName);
        TextView tvTotalCost = findViewById(R.id.tvTotalCost);
        MaterialButton btnConfirmReturn = findViewById(R.id.btnConfirmReturn);

        tvVehicleName.setText(vehicleName);
        tvTotalCost.setText(String.format("₹%.2f", totalCost));

        btnConfirmReturn.setOnClickListener(v -> confirmReturn());
    }

    private void confirmReturn() {
        // Find the session to get the vehicle ID
        db.collection("rental_sessions").document(sessionId).get()
                .addOnSuccessListener(doc -> {
                    String vehicleId = doc.getString("vehicleId");
                    
                    // Mark session as completed
                    db.collection("rental_sessions").document(sessionId)
                            .update("status", "completed")
                            .addOnSuccessListener(a -> {
                                // Mark vehicle as available
                                if (vehicleId != null) {
                                    db.collection("vehicles").document(vehicleId)
                                            .update("available", true);
                                }
                                Toast.makeText(this, "Rental Completed! Thank you.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to confirm return", Toast.LENGTH_SHORT).show());
    }
}
