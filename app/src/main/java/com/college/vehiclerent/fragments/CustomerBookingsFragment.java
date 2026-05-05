package com.college.vehiclerent.fragments;

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

import com.college.vehiclerent.R;
import com.college.vehiclerent.adapter.BookingAdapter;
import com.college.vehiclerent.model.RentalSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private TextView tvEmpty;
    private BookingAdapter adapter;
    private List<RentalSession> bookingList;
    private FirebaseFirestore db;
    private ListenerRegistration listenerReg;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_bookings, container, false);
        
        db = FirebaseFirestore.getInstance();
        bookingList = new ArrayList<>();

        rvBookings = view.findViewById(R.id.rvBookings);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingAdapter(getContext(), bookingList, false);
        rvBookings.setAdapter(adapter);
        
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        listenToBookings();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (listenerReg != null) listenerReg.remove();
    }

    private void listenToBookings() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        listenerReg = db.collection("rental_sessions")
                .whereEqualTo("customerId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    bookingList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        RentalSession session = doc.toObject(RentalSession.class);
                        session.setId(doc.getId());
                        bookingList.add(session);
                    }
                    
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
