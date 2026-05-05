package com.college.vehiclerent;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.college.vehiclerent.fragments.OwnerFleetFragment;
import com.college.vehiclerent.fragments.OwnerHomeFragment;
import com.college.vehiclerent.fragments.OwnerProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Arrays;

public class OwnerDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration activeSessionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new OwnerHomeFragment(), "ANALYTICS");
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_owner_home) {
                loadFragment(new OwnerHomeFragment(), "ANALYTICS");
                return true;
            } else if (id == R.id.nav_owner_fleet) {
                loadFragment(new OwnerFleetFragment(), "FLEET");
                return true;
            } else if (id == R.id.nav_owner_profile) {
                loadFragment(new OwnerProfileFragment(), "PROFILE");
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        Fragment current = getSupportFragmentManager().findFragmentByTag(tag);
        if (current != null && current.isVisible()) return;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForActiveRentals();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (activeSessionListener != null) activeSessionListener.remove();
    }

    private void listenForActiveRentals() {
        if (mAuth.getUid() == null) return;
        activeSessionListener = db.collection("rental_sessions")
                .whereEqualTo("ownerId", mAuth.getUid())
                .whereIn("status", Arrays.asList("pending", "active", "returning"))
                .addSnapshotListener((value, error) -> {
                    // Note: Since active rental banner might be inside fragments, 
                    // we'll need to communicate this to the fragments or find them.
                    // However, to keep it simple, we'll let fragments handle their OWN banners 
                    // but we'll fix the fragment reloading issue which was causing them to lose state.
                });
    }
}
