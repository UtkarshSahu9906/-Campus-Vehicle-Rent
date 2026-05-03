package com.college.vehiclerent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.college.vehiclerent.model.RentalSession;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvName, tvEmail, tvTotalRides, tvAvgRating;
    private ImageView ivProfile;
    private RecyclerView rvHistory;
    // We could use a simplified adapter for history, but for now we'll just use a basic implementation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        initViews();
        loadUserData(user);
        loadRentalHistory(user.getUid());

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSignOut).setOnClickListener(v -> signOut());
    }

    private void initViews() {
        tvName = findViewById(R.id.tvUserName);
        tvEmail = findViewById(R.id.tvUserEmail);
        tvTotalRides = findViewById(R.id.tvTotalRides);
        tvAvgRating = findViewById(R.id.tvAvgRating);
        ivProfile = findViewById(R.id.ivProfileLarge);
        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadUserData(FirebaseUser user) {
        if (user != null) {
            tvName.setText(user.getDisplayName());
            tvEmail.setText(user.getEmail());
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).circleCrop().into(ivProfile);
            }
        }
    }

    private void loadRentalHistory(String uid) {
        db.collection("rental_sessions")
                .whereEqualTo("customerId", uid)
                .whereEqualTo("status", "completed")
                .orderBy("endTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvTotalRides.setText(String.valueOf(count));
                    // Simplified history display logic
                });
    }

    private void signOut() {
        mAuth.signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
