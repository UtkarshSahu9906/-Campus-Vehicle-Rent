package com.college.vehiclerent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RoleSelectionActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_selection);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnOwner).setOnClickListener(v -> saveRole("owner"));
        findViewById(R.id.btnCustomer).setOnClickListener(v -> saveRole("customer"));
    }

    private void saveRole(String role) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid",   user.getUid());
        userData.put("name",  user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("photo", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        userData.put("role",  role);

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Class<?> dest = "owner".equals(role)
                            ? OwnerDashboardActivity.class
                            : CustomerDashboardActivity.class;
                    startActivity(new Intent(this, dest));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error saving role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
