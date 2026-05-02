package com.college.vehiclerent;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.material.chip.ChipGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.vehiclerent.adapter.VehicleAdapter;
import com.college.vehiclerent.model.Vehicle;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CustomerDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VehicleAdapter adapter;
    private List<Vehicle> allVehicles    = new ArrayList<>();
    private List<Vehicle> filteredList   = new ArrayList<>();
    private TextView      tvEmpty;
    private EditText      etSearch;
    private ChipGroup     chipGroup;
    private String        selectedCategory = "All";

    private FirebaseFirestore db;
    private FirebaseAuth      mAuth;
    private ListenerRegistration listenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Rent a Vehicle");

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvEmpty      = findViewById(R.id.tvEmpty);
        etSearch     = findViewById(R.id.etSearch);
        chipGroup    = findViewById(R.id.chipGroup);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // false = customer mode (no delete, shows owner name)
        adapter = new VehicleAdapter(this, filteredList, false);
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) { filter(); }
            public void afterTextChanged(Editable s) {}
        });

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipBike) selectedCategory = "Bike";
            else if (checkedId == R.id.chipScooter) selectedCategory = "Scooter";
            else if (checkedId == R.id.chipCar) selectedCategory = "Car";
            else if (checkedId == R.id.chipCycle) selectedCategory = "Cycle";
            else selectedCategory = "All";
            filter();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenToAllVehicles();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerReg != null) listenerReg.remove();
    }

    private void listenToAllVehicles() {
        listenerReg = db.collection("vehicles")
                .whereEqualTo("available", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    allVehicles.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Vehicle v = doc.toObject(Vehicle.class);
                        v.setId(doc.getId());
                        allVehicles.add(v);
                    }
                    filter();
                });
    }

    private void filter() {
        String query = etSearch.getText().toString().trim().toLowerCase();
        filteredList.clear();

        for (Vehicle v : allVehicles) {
            boolean matchesCategory = selectedCategory.equals("All") || 
                                     (v.getCategory() != null && v.getCategory().equalsIgnoreCase(selectedCategory));
            
            boolean matchesSearch = query.isEmpty() || 
                                   (v.getVehicleType() != null && v.getVehicleType().toLowerCase().contains(query)) ||
                                   (v.getLocation() != null && v.getLocation().toLowerCase().contains(query)) ||
                                   (v.getDescription() != null && v.getDescription().toLowerCase().contains(query));

            if (matchesCategory && matchesSearch) {
                filteredList.add(v);
            }
        }
        
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_switch_role) {
            db.collection("users").document(mAuth.getUid())
                    .update("role", "owner")
                    .addOnSuccessListener(v -> {
                        startActivity(new Intent(this, OwnerDashboardActivity.class));
                        finish();
                    });
            return true;
        }
        if (item.getItemId() == R.id.action_signout) {
            mAuth.signOut();
            GoogleSignInOptions gso = new GoogleSignInOptions
                    .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail().build();
            GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener(t -> {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
