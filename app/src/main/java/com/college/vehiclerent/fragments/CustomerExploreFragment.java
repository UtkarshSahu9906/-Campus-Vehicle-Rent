package com.college.vehiclerent.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.vehiclerent.ActiveRentalActivity;
import com.college.vehiclerent.R;
import com.college.vehiclerent.RentalConfirmationActivity;
import com.college.vehiclerent.adapter.VehicleAdapter;
import com.college.vehiclerent.model.Vehicle;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerExploreFragment extends Fragment {

    private RecyclerView rvVehicles;
    private VehicleAdapter adapter;
    private List<Vehicle> vehicleList;
    private List<Vehicle> filteredList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration activeSessionListener;
    private ListenerRegistration exploreListener;
    
    private EditText etSearch;
    private ChipGroup chipGroupCategory;
    private TextView tvEmpty;
    private View btnActiveRide;
    
    private String currentCategory = "All";
    private String currentSearch = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_explore, container, false);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        rvVehicles = view.findViewById(R.id.rvVehicles);
        etSearch = view.findViewById(R.id.etSearch);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        btnActiveRide = view.findViewById(R.id.btnActiveRide);
        
        vehicleList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new VehicleAdapter(getContext(), filteredList, false);
        rvVehicles.setLayoutManager(new LinearLayoutManager(getContext()));
        rvVehicles.setAdapter(adapter);
        
        setupFilters();
        
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadVehicles();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (exploreListener != null) exploreListener.remove();
    }

    private void setupFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = s.toString().toLowerCase();
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipAll) currentCategory = "All";
            else if (checkedId == R.id.chipScooty) currentCategory = "Scooty";
            else if (checkedId == R.id.chipBike) currentCategory = "Bike";
            else if (checkedId == R.id.chipCycle) currentCategory = "Cycle";
            applyFilters();
        });
    }

    private void applyFilters() {
        filteredList.clear();
        for (Vehicle v : vehicleList) {
            boolean matchCategory = currentCategory.equals("All") || currentCategory.equalsIgnoreCase(v.getCategory());
            
            // Search in both vehicle type and location
            String searchLower = currentSearch.toLowerCase();
            boolean matchType = v.getVehicleType().toLowerCase().contains(searchLower);
            boolean matchLocation = (v.getLocation() != null && v.getLocation().toLowerCase().contains(searchLower));
            
            if (matchCategory && (matchType || matchLocation)) {
                filteredList.add(v);
            }
        }

        // Sort by location match first if searching
        if (!currentSearch.isEmpty()) {
            java.util.Collections.sort(filteredList, (v1, v2) -> {
                String searchLower = currentSearch.toLowerCase();
                boolean loc1 = v1.getLocation() != null && v1.getLocation().toLowerCase().contains(searchLower);
                boolean loc2 = v2.getLocation() != null && v2.getLocation().toLowerCase().contains(searchLower);
                
                if (loc1 && !loc2) return -1; // v1 matches location, v2 doesn't -> v1 comes first
                if (!loc1 && loc2) return 1;  // v2 matches location, v1 doesn't -> v2 comes first
                return 0; // Both match or both don't
            });
        }
        
        adapter.notifyDataSetChanged();
        
        if (filteredList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvVehicles.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvVehicles.setVisibility(View.VISIBLE);
        }
    }

    private void loadVehicles() {
        exploreListener = db.collection("vehicles")
                .whereEqualTo("visible", true) // Exclude invisible
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    vehicleList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Vehicle v = doc.toObject(Vehicle.class);
                        v.setId(doc.getId());
                        vehicleList.add(v);
                    }
                    applyFilters();
                });
    }
}
