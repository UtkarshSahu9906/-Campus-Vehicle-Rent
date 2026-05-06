package com.college.vehiclerent;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.vehiclerent.adapter.BookingAdapter;
import com.college.vehiclerent.model.RentalSession;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VehicleHistoryActivity extends AppCompatActivity {

    private String vehicleId, vehicleName;
    private FirebaseFirestore db;
    
    private TextView tvTitle, tvTotalEarned, tvTotalRides;
    private RecyclerView rvHistory;
    private BookingAdapter adapter;
    private List<RentalSession> historyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_history);

        vehicleId = getIntent().getStringExtra("vehicleId");
        vehicleName = getIntent().getStringExtra("vehicleName");
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(vehicleName + " History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvTotalEarned = findViewById(R.id.tvTotalEarned);
        tvTotalRides = findViewById(R.id.tvTotalRides);
        
        rvHistory = findViewById(R.id.rvVehicleHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter(this, historyList, true);
        rvHistory.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        db.collection("rental_sessions")
                .whereEqualTo("vehicleId", vehicleId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    historyList.clear();
                    double totalEarned = 0;
                    for (QueryDocumentSnapshot doc : query) {
                        RentalSession session = doc.toObject(RentalSession.class);
                        session.setId(doc.getId());
                        historyList.add(session);
                        if ("completed".equals(session.getStatus())) {
                            totalEarned += session.getTotalCost();
                        }
                    }
                    tvTotalRides.setText(String.valueOf(historyList.size()) + " Rides");
                    tvTotalEarned.setText(String.format("₹%.0f Earned", totalEarned));
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
