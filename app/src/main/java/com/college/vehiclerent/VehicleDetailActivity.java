package com.college.vehiclerent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

public class VehicleDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Unpack extras sent from the adapter
        String vehicleType = getIntent().getStringExtra("vehicleType");
        String description  = getIntent().getStringExtra("description");
        String imageUrl     = getIntent().getStringExtra("imageUrl");
        double price        = getIntent().getDoubleExtra("pricePerHour", 0);
        String mobile       = getIntent().getStringExtra("mobileNo");
        String ownerName    = getIntent().getStringExtra("ownerName");
        String pickupLocation = getIntent().getStringExtra("location");

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(vehicleType);

        ImageView     imgVehicle  = findViewById(R.id.imgVehicle);
        TextView      tvType      = findViewById(R.id.tvVehicleType);
        TextView      tvDesc      = findViewById(R.id.tvDescription);
        TextView      tvPrice     = findViewById(R.id.tvPrice);
        TextView      tvOwner     = findViewById(R.id.tvOwner);
        TextView      tvMobile    = findViewById(R.id.tvMobile);
        TextView      tvLocation  = findViewById(R.id.tvLocation);
        MaterialButton btnWhatsApp = findViewById(R.id.btnWhatsApp);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(imgVehicle);

        tvType.setText(vehicleType);
        tvDesc.setText(description.isEmpty() ? "No description provided." : description);
        tvPrice.setText("₹" + String.format("%.0f", price) + " / hour");
        tvLocation.setText(pickupLocation != null ? pickupLocation : "Location not specified");
        tvOwner.setText("Owner: " + ownerName);
        tvMobile.setText("Mobile: " + mobile);

        // ── WhatsApp deep link ─────────────────────────────────────────────
        btnWhatsApp.setOnClickListener(v -> openWhatsApp(mobile, vehicleType));
    }

    private void openWhatsApp(String mobile, String vehicleType) {
        // Remove spaces/dashes from number, prepend country code 91 (India)
        String cleanNumber = mobile.replaceAll("[^0-9]", "");
        if (!cleanNumber.startsWith("91")) {
            cleanNumber = "91" + cleanNumber;
        }
        String message = "Hello! I found your *" + vehicleType +
                "* listed on Campus Vehicle Rent app. I am interested in renting it. Please share the details.";
        String url = "https://wa.me/" + cleanNumber + "?text=" + Uri.encode(message);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage("com.whatsapp");

        // Fallback: if WhatsApp not installed, open in browser
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
                intent.setPackage(null);
                startActivity(intent);
                Toast.makeText(this, "WhatsApp not installed. Opening in browser.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
