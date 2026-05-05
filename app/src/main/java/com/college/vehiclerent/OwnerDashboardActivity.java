package com.college.vehiclerent;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.college.vehiclerent.fragments.OwnerFleetFragment;
import com.college.vehiclerent.fragments.OwnerHomeFragment;
import com.college.vehiclerent.fragments.OwnerProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class OwnerDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new OwnerHomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int id = item.getItemId();
            if (id == R.id.nav_owner_home) {
                selectedFragment = new OwnerHomeFragment();
            } else if (id == R.id.nav_owner_fleet) {
                selectedFragment = new OwnerFleetFragment();
            } else if (id == R.id.nav_owner_profile) {
                selectedFragment = new OwnerProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }
}
