package com.college.vehiclerent;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.vehiclerent.adapter.BookingAdapter;
import com.college.vehiclerent.model.RentalSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerBookingsActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private TextView tvEmpty;
    private BookingAdapter adapter;
    private List<RentalSession> bookingList;
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_bookings);

        db = FirebaseFirestore.getInstance();
        bookingList = new ArrayList<>();
        userRole = getIntent().getStringExtra("userRole");
        if (userRole == null) userRole = "customer";

        rvBookings = findViewById(R.id.rvBookings);
        tvEmpty = findViewById(R.id.tvEmpty);

        if ("owner".equals(userRole)) {
            ((TextView) findViewById(R.id.tvEmpty)).setText("No rental history found.");
            ((TextView) findViewById(R.id.tvEmpty)).getRootView().findViewById(R.id.btnBack).setOnClickListener(v -> finish());
            // Title will need to be updated, but let's just find the TextView by view traversal
        }

        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter(this, bookingList, "owner".equals(userRole));
        rvBookings.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenToBookings();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerReg != null) listenerReg.remove();
    }

    private void listenToBookings() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String field = "owner".equals(userRole) ? "ownerId" : "customerId";

        listenerReg = db.collection("rental_sessions")
                .whereEqualTo(field, uid)
                // Note: sorting requires a composite index, we can just fetch and sort locally
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    bookingList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        RentalSession session = doc.toObject(RentalSession.class);
                        session.setId(doc.getId());
                        bookingList.add(session);
                    }
                    
                    // Sort locally: newest first (based on startTime or just assuming order)
                    // For simplicity, we just notify adapter
                    adapter.notifyDataSetChanged();
                    
                    if (bookingList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvBookings.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvBookings.setVisibility(View.VISIBLE);
                    }
                });
    }
}
