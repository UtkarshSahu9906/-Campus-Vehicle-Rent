package com.college.vehiclerent;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.college.vehiclerent.fragments.CustomerBookingsFragment;
import com.college.vehiclerent.fragments.CustomerExploreFragment;
import com.college.vehiclerent.fragments.CustomerProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Arrays;

public class CustomerDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration activeSessionListener;
    private String lastShownPendingId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new CustomerExploreFragment(), "EXPLORE");
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_customer_explore) {
                loadFragment(new CustomerExploreFragment(), "EXPLORE");
                return true;
            } else if (id == R.id.nav_customer_bookings) {
                loadFragment(new CustomerBookingsFragment(), "BOOKINGS");
                return true;
            } else if (id == R.id.nav_customer_profile) {
                loadFragment(new CustomerProfileFragment(), "PROFILE");
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        // Prevent re-loading same fragment
        Fragment current = getSupportFragmentManager().findFragmentByTag(tag);
        if (current != null && current.isVisible()) return;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForPendingRentals();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (activeSessionListener != null) activeSessionListener.remove();
    }

    private void listenForPendingRentals() {
        if (mAuth.getUid() == null) return;
        activeSessionListener = db.collection("rental_sessions")
                .whereEqualTo("customerId", mAuth.getUid())
                .whereIn("status", Arrays.asList("pending", "active", "returning"))
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || value.isEmpty()) return;

                    com.google.firebase.firestore.DocumentSnapshot targetDoc = null;
                    
                    // Prioritize pending or returning sessions over active ones
                    for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                        String s = doc.getString("status");
                        if ("pending".equals(s) || "returning".equals(s)) {
                            targetDoc = doc;
                            break; 
                        }
                    }
                    
                    // If no pending/returning found, just ignore (or we could handle active but dashboard doesn't need to)
                    if (targetDoc == null) {
                        lastShownPendingId = "";
                        return;
                    }

                    String status = targetDoc.getString("status");
                    String sessionId = targetDoc.getId();
                    
                    if ("pending".equals(status)) {
                        if (sessionId.equals(lastShownPendingId)) return;
                        lastShownPendingId = sessionId;

                        Intent intent = new Intent(this, RentalConfirmationActivity.class);
                        intent.putExtra("sessionId", sessionId);
                        intent.putExtra("vehicleName", targetDoc.getString("vehicleType"));
                        intent.putExtra("ownerName", targetDoc.getString("ownerName"));
                        intent.putExtra("rateHour", targetDoc.getDouble("pricePerHour") != null ? targetDoc.getDouble("pricePerHour") : 0.0);
                        intent.putExtra("rateDay", targetDoc.getDouble("pricePerDay") != null ? targetDoc.getDouble("pricePerDay") : 0.0);
                        startActivity(intent);
                    } else if ("returning".equals(status)) {
                        if (sessionId.equals(lastShownPendingId)) return;
                        lastShownPendingId = sessionId;

                        Intent intent = new Intent(this, RentalReturnConfirmationActivity.class);
                        intent.putExtra("sessionId", sessionId);
                        intent.putExtra("vehicleName", targetDoc.getString("vehicleType"));
                        intent.putExtra("totalCost", targetDoc.getDouble("totalCost") != null ? targetDoc.getDouble("totalCost") : 0.0);
                        startActivity(intent);
                    }
                });
    }
}
