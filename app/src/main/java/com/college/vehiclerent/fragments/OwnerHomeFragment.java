package com.college.vehiclerent.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.vehiclerent.ActiveRentalActivity;
import com.college.vehiclerent.R;
import com.college.vehiclerent.adapter.BookingAdapter;
import com.college.vehiclerent.model.RentalSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OwnerHomeFragment extends Fragment {

    private TextView tvTotalEarnedOverall, tvEarnedMonth, tvTotalTrips, tvEmpty;
    private View btnActiveRental;
    private RecyclerView rvRecentRentals;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration activeSessionListener;
    private ListenerRegistration recentRentalsListener;
    
    private BookingAdapter adapter;
    private List<RentalSession> recentList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_home, container, false);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        tvTotalEarnedOverall = view.findViewById(R.id.tvTotalEarnedOverall);
        tvEarnedMonth = view.findViewById(R.id.tvEarnedMonth);
        tvTotalTrips = view.findViewById(R.id.tvTotalTrips);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        btnActiveRental = view.findViewById(R.id.btnActiveRental);
        rvRecentRentals = view.findViewById(R.id.rvRecentRentals);
        
        recentList = new ArrayList<>();
        adapter = new BookingAdapter(getContext(), recentList, true);
        rvRecentRentals.setLayoutManager(new LinearLayoutManager(getContext()));
        rvRecentRentals.setAdapter(adapter);
        
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadAnalytics();
        listenActiveRentals();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (activeSessionListener != null) activeSessionListener.remove();
        if (recentRentalsListener != null) recentRentalsListener.remove();
    }

    private void loadAnalytics() {
        if (mAuth.getUid() == null) return;
        
        recentRentalsListener = db.collection("rental_sessions")
                .whereEqualTo("ownerId", mAuth.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    double totalEarned = 0;
                    double monthEarned = 0;
                    int trips = 0;
                    
                    long currentMonthStart = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // Rough 30 days
                    
                    recentList.clear();
                    
                    for (QueryDocumentSnapshot doc : value) {
                        RentalSession session = doc.toObject(RentalSession.class);
                        session.setId(doc.getId());
                        
                        if ("completed".equals(session.getStatus())) {
                            totalEarned += session.getTotalCost();
                            trips++;
                            
                            if (session.getEndTime() > currentMonthStart) {
                                monthEarned += session.getTotalCost();
                            }
                        }
                        
                        recentList.add(session);
                    }
                    
                    tvTotalEarnedOverall.setText(String.format("₹%.0f", totalEarned));
                    tvEarnedMonth.setText(String.format("₹%.0f", monthEarned));
                    tvTotalTrips.setText(String.valueOf(trips));
                    
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(recentList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void listenActiveRentals() {
        if (mAuth.getUid() == null) return;
        activeSessionListener = db.collection("rental_sessions")
                .whereEqualTo("ownerId", mAuth.getUid())
                .whereIn("status", java.util.Arrays.asList("pending", "active", "returning"))
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || value.isEmpty()) {
                        btnActiveRental.setVisibility(View.GONE);
                        return;
                    }
                    btnActiveRental.setVisibility(View.VISIBLE);
                    
                    com.google.firebase.firestore.DocumentSnapshot doc = value.getDocuments().get(0);
                    btnActiveRental.setOnClickListener(v -> {
                        Intent intent = new Intent(getContext(), ActiveRentalActivity.class);
                        intent.putExtra("sessionId", doc.getId());
                        intent.putExtra("userRole", "owner");
                        startActivity(intent);
                    });
                });
    }
}
