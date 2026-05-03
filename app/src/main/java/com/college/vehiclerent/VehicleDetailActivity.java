package com.college.vehiclerent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class VehicleDetailActivity extends AppCompatActivity {

    private String vehicleId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Unpack extras sent from the adapter
        String vehicleType   = getIntent().getStringExtra("vehicleType");
        String description   = getIntent().getStringExtra("description");
        String imageUrl      = getIntent().getStringExtra("imageUrl");
        double price         = getIntent().getDoubleExtra("pricePerHour", 0);
        String mobile        = getIntent().getStringExtra("mobileNo");
        String ownerName     = getIntent().getStringExtra("ownerName");
        String pickupLocation = getIntent().getStringExtra("location");
        vehicleId            = getIntent().getStringExtra("vehicleId");
        double totalRating   = getIntent().getDoubleExtra("totalRating", 0);
        int ratingCount      = getIntent().getIntExtra("ratingCount", 0);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(vehicleType);

        ImageView      imgVehicle  = findViewById(R.id.imgVehicle);
        TextView       tvType      = findViewById(R.id.tvVehicleType);
        TextView       tvDesc      = findViewById(R.id.tvDescription);
        TextView       tvPrice     = findViewById(R.id.tvPrice);
        TextView       tvOwner     = findViewById(R.id.tvOwner);
        TextView       tvMobile    = findViewById(R.id.tvMobile);
        TextView       tvLocation  = findViewById(R.id.tvLocation);
        MaterialButton btnWhatsApp = findViewById(R.id.btnWhatsApp);
        TextView       tvAvgRating = findViewById(R.id.tvAvgRating);
        TextView       tvRatingCnt = findViewById(R.id.tvRatingCount);
        RatingBar      ratingBar   = findViewById(R.id.ratingBar);
        MaterialButton btnRate     = findViewById(R.id.btnSubmitRating);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(imgVehicle);

        tvType.setText(vehicleType);
        tvDesc.setText(description == null || description.isEmpty() ? "No description provided." : description);
        tvPrice.setText("₹" + String.format("%.0f", price) + " / hour");
        tvLocation.setText(pickupLocation != null ? pickupLocation : "Location not specified");
        tvOwner.setText(ownerName);
        tvMobile.setText(mobile);

        // Rating display
        if (ratingCount > 0) {
            double avg = totalRating / ratingCount;
            tvAvgRating.setText(String.format("%.1f", avg));
            tvRatingCnt.setText("(" + ratingCount + " reviews)");
        } else {
            tvAvgRating.setText("New");
            tvRatingCnt.setText("No reviews yet");
        }

        // Submit rating
        btnRate.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            if (rating == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            if (vehicleId == null || vehicleId.isEmpty()) {
                Toast.makeText(this, "Unable to rate this vehicle", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore.getInstance()
                    .collection("vehicles")
                    .document(vehicleId)
                    .update(
                            "totalRating", FieldValue.increment(rating),
                            "ratingCount", FieldValue.increment(1)
                    )
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Thanks for rating! ⭐", Toast.LENGTH_SHORT).show();
                        btnRate.setEnabled(false);
                        btnRate.setText("Rated ✓");
                        ratingBar.setIsIndicator(true);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Rating failed. Try again.", Toast.LENGTH_SHORT).show()
                    );
        });

        // Unpack owner info
        String ownerUid = getIntent().getStringExtra("ownerUid");
        String currentUid = FirebaseAuth.getInstance().getUid();

        // WhatsApp deep link
        btnWhatsApp.setOnClickListener(v -> openWhatsApp(mobile, vehicleType));

        // If owner is viewing their own vehicle, change "Contact" to "Release Vehicle"
        if (currentUid != null && currentUid.equals(ownerUid)) {
            btnWhatsApp.setText("Release Vehicle");
            btnWhatsApp.setIconResource(android.R.drawable.ic_menu_send);
            btnWhatsApp.setOnClickListener(v -> startRentalSession(ownerUid, ownerName, vehicleType, price));
            
            // Hide rating section for owner
            findViewById(R.id.ratingBar).setVisibility(View.GONE);
            findViewById(R.id.btnSubmitRating).setVisibility(View.GONE);
        }
    }

    private void startRentalSession(String ownerUid, String ownerName, String vehicleType, double price) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        String currentName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (currentName == null) currentName = "Student";

        com.college.vehiclerent.model.RentalSession newSession = new com.college.vehiclerent.model.RentalSession(
                vehicleId, vehicleType,
                ownerUid, ownerName,
                "WAITING", "Student", // Updated when customer confirms
                price
        );

        FirebaseFirestore.getInstance().collection("rental_sessions")
                .add(newSession)
                .addOnSuccessListener(docRef -> {
                    // Update vehicle availability
                    FirebaseFirestore.getInstance().collection("vehicles").document(vehicleId)
                            .update("available", false);

                    Intent intent = new Intent(this, ActiveRentalActivity.class);
                    intent.putExtra("sessionId", docRef.getId());
                    intent.putExtra("userRole", "owner");
                    startActivity(intent);
                    finish();
                });
    }

    private void openWhatsApp(String mobile, String vehicleType) {
        String cleanNumber = mobile.replaceAll("[^0-9]", "");
        if (!cleanNumber.startsWith("91")) {
            cleanNumber = "91" + cleanNumber;
        }
        String message = "Hello! I found your *" + vehicleType +
                "* listed on Campus Ride. I'm interested in renting it. Please share details.";
        String url = "https://wa.me/" + cleanNumber + "?text=" + Uri.encode(message);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage("com.whatsapp");

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            intent.setPackage(null);
            startActivity(intent);
            Toast.makeText(this, "WhatsApp not installed. Opening in browser.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
