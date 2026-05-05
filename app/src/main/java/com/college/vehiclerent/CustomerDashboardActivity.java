package com.college.vehiclerent;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.college.vehiclerent.fragments.CustomerBookingsFragment;
import com.college.vehiclerent.fragments.CustomerExploreFragment;
import com.college.vehiclerent.fragments.CustomerProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomerDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new CustomerExploreFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            
            int id = item.getItemId();
            if (id == R.id.nav_customer_explore) {
                selectedFragment = new CustomerExploreFragment();
            } else if (id == R.id.nav_customer_bookings) {
                selectedFragment = new CustomerBookingsFragment();
            } else if (id == R.id.nav_customer_profile) {
                selectedFragment = new CustomerProfileFragment();
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
