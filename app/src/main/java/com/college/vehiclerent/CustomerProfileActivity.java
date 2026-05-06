package com.college.vehiclerent;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.college.vehiclerent.adapter.BookingAdapter;
import com.college.vehiclerent.model.RentalSession;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerProfileActivity extends AppCompatActivity {

    private String customerId;
    private FirebaseFirestore db;
    
    private TextView tvName, tvEmail, tvTotalRides, tvReliability;
    private ImageView ivProfile;
    private RecyclerView rvHistory;
    private BookingAdapter adapter;
    private List<RentalSession> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_profile);

        customerId = getIntent().getStringExtra("customerId");
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvName = findViewById(R.id.tvCustomerName);
        tvEmail = findViewById(R.id.tvCustomerEmail);
        tvTotalRides = findViewById(R.id.tvTotalRides);
        tvReliability = findViewById(R.id.tvReliability);
        ivProfile = findViewById(R.id.ivCustomerProfile);
        
        rvHistory = findViewById(R.id.rvCustomerHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter(this, historyList, true); // Use owner mode to see details
        rvHistory.setAdapter(adapter);

        loadCustomerDetails();
        loadCustomerHistory();
    }

    private void loadCustomerDetails() {
        db.collection("users").document(customerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvName.setText(doc.getString("name"));
                        tvEmail.setText(doc.getString("email"));
                        String profilePic = doc.getString("profilePic");
                        if (profilePic != null) {
                            Glide.with(this).load(profilePic).into(ivProfile);
                        }
                    }
                });
    }

    private void loadCustomerHistory() {
        db.collection("rental_sessions")
                .whereEqualTo("customerId", customerId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    historyList.clear();
                    int completed = 0;
                    for (QueryDocumentSnapshot doc : query) {
                        RentalSession session = doc.toObject(RentalSession.class);
                        session.setId(doc.getId());
                        historyList.add(session);
                        if ("completed".equals(session.getStatus())) {
                            completed++;
                        }
                    }
                    tvTotalRides.setText(String.valueOf(historyList.size()));
                    if (historyList.size() > 0) {
                        int reliability = (completed * 100) / historyList.size();
                        tvReliability.setText(reliability + "%");
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
