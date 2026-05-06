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

import com.college.vehiclerent.AddVehicleActivity;
import com.college.vehiclerent.R;
import com.college.vehiclerent.adapter.VehicleAdapter;
import com.college.vehiclerent.model.Vehicle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OwnerFleetFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private VehicleAdapter adapter;
    private List<Vehicle> vehicleList;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration fleetListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_owner_fleet, container, false);
        
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        
        vehicleList = new ArrayList<>();
        adapter = new VehicleAdapter(getContext(), vehicleList, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        view.findViewById(R.id.fab).setOnClickListener(v -> 
            startActivity(new Intent(getContext(), AddVehicleActivity.class))
        );
        
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
        if (fleetListener != null) fleetListener.remove();
    }

    private void loadVehicles() {
        if (mAuth.getUid() == null) return;
        
        fleetListener = db.collection("vehicles")
                .whereEqualTo("ownerUid", mAuth.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    
                    vehicleList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Vehicle v = doc.toObject(Vehicle.class);
                        v.setId(doc.getId());
                        vehicleList.add(v);
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (vehicleList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
    }
}
