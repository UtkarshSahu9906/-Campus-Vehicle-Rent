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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.components.XAxis;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OwnerHomeFragment extends Fragment {

    private TextView tvTotalEarnedOverall, tvEarnedMonth, tvRunningVehicles, tvEmpty;
    private View btnActiveRental;
    private RecyclerView rvRecentRentals;
    private LineChart earningsChart;
    
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
        tvRunningVehicles = view.findViewById(R.id.tvRunningVehicles);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        btnActiveRental = view.findViewById(R.id.btnActiveRental);
        rvRecentRentals = view.findViewById(R.id.rvRecentRentals);
        earningsChart = view.findViewById(R.id.earningsChart);
        
        setupChart();
        
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
                    int running = 0;
                    
                    Map<Integer, Double> dayEarnings = new TreeMap<>();
                    // Init last 7 days with 0
                    for (int i = 0; i < 7; i++) dayEarnings.put(i, 0.0);
                    
                    long now = System.currentTimeMillis();
                    long currentMonthStart = now - (30L * 24 * 60 * 60 * 1000);
                    
                    recentList.clear();
                    
                    for (QueryDocumentSnapshot doc : value) {
                        RentalSession session = doc.toObject(RentalSession.class);
                        session.setId(doc.getId());
                        String status = session.getStatus();
                        
                        if ("active".equals(status) || "returning".equals(status)) {
                            running++;
                        }
                        
                        if ("completed".equals(status)) {
                            totalEarned += session.getTotalCost();
                            
                            if (session.getEndTime() > currentMonthStart) {
                                monthEarned += session.getTotalCost();
                            }
                            
                            // For chart: check if it's within last 7 days
                            long diff = now - session.getEndTime();
                            int dayIndex = (int) (diff / (24 * 60 * 60 * 1000));
                            if (dayIndex >= 0 && dayIndex < 7) {
                                double currentDayVal = dayEarnings.get(6 - dayIndex) != null ? dayEarnings.get(6 - dayIndex) : 0;
                                dayEarnings.put(6 - dayIndex, currentDayVal + session.getTotalCost());
                            }
                        }
                        
                        recentList.add(session);
                    }
                    
                    tvTotalEarnedOverall.setText(String.format("₹%.0f", totalEarned));
                    tvEarnedMonth.setText(String.format("₹%.0f", monthEarned));
                    tvRunningVehicles.setText(String.valueOf(running));
                    
                    updateChartData(dayEarnings);
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(recentList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void setupChart() {
        earningsChart.getDescription().setEnabled(false);
        earningsChart.setDrawGridBackground(false);
        earningsChart.getLegend().setEnabled(false);
        earningsChart.setTouchEnabled(false);
        
        XAxis xAxis = earningsChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getResources().getColor(R.color.text_secondary));
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Simplified: show "6d ago" ... "Today"
                int val = (int) value;
                if (val == 6) return "Today";
                return (6 - val) + "d ago";
            }
        });

        earningsChart.getAxisLeft().setDrawGridLines(true);
        earningsChart.getAxisLeft().setGridColor(getResources().getColor(R.color.card_outline));
        earningsChart.getAxisLeft().setTextColor(getResources().getColor(R.color.text_secondary));
        earningsChart.getAxisRight().setEnabled(false);
    }

    private void updateChartData(Map<Integer, Double> dayEarnings) {
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : dayEarnings.entrySet()) {
            entries.add(new Entry(entry.getKey(), entry.getValue().floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Earnings");
        dataSet.setColor(getResources().getColor(R.color.primary));
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(getResources().getColor(R.color.primary));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        // Fill gradient
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.primary));
        dataSet.setFillAlpha(30);

        LineData lineData = new LineData(dataSet);
        earningsChart.setData(lineData);
        earningsChart.invalidate();
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
