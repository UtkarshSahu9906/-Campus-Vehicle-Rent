package com.college.vehiclerent;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Delay for 2 seconds to show the splash screen
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuth, 2000);
    }

    private void checkAuth() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Already signed in, check role
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists() && doc.getString("role") != null) {
                            String role = doc.getString("role");
                            Class<?> dest = "owner".equals(role)
                                    ? OwnerDashboardActivity.class
                                    : CustomerDashboardActivity.class;
                            startActivity(new Intent(this, dest));
                        } else {
                            // User exists but role not set
                            startActivity(new Intent(this, RoleSelectionActivity.class));
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // Error fetching role, fallback to Login
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
        } else {
            // Not signed in, go to Login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
