package com.college.vehiclerent;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.college.vehiclerent.adapter.VehicleAdapter;
import com.college.vehiclerent.model.Vehicle;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OwnerDashboardActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VehicleAdapter adapter;
    private List<Vehicle> vehicleList;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration listenerReg;
    private TextView tvTotalEarnedOverall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_dashboard);

        // No toolbar in the new design; header is handled in the layout

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        vehicleList = new ArrayList<>();

        tvEmpty     = findViewById(R.id.tvEmpty);
        tvTotalEarnedOverall = findViewById(R.id.tvTotalEarnedOverall);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // true = owner mode (shows delete on long-press)
        adapter = new VehicleAdapter(this, vehicleList, true);
        recyclerView.setAdapter(adapter);

        View fabView = findViewById(R.id.fab);
        fabView.setOnClickListener(v -> startActivity(new Intent(this, AddVehicleActivity.class)));

        // Profile icon → open profile
        findViewById(R.id.ivOwnerProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        
        findViewById(R.id.btnViewHistory).setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerBookingsActivity.class);
            intent.putExtra("userRole", "owner");
            startActivity(intent);
        });

        findViewById(R.id.btnSwitchToCustomer).setOnClickListener(v -> {
            db.collection("users").document(mAuth.getUid()).update("role", "customer")
                    .addOnSuccessListener(a -> {
                        startActivity(new Intent(this, CustomerDashboardActivity.class));
                        finish();
                    });
        });

        checkActiveRentals();
        calculateTotalEarnings();
    }

    private void calculateTotalEarnings() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("rental_sessions")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("status", "completed")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    double totalEarned = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        Double cost = doc.getDouble("totalCost");
                        if (cost != null) totalEarned += cost;
                    }
                    tvTotalEarnedOverall.setText(String.format("₹%.0f", totalEarned));
                });
    }

    private void checkActiveRentals() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("rental_sessions")
                .whereEqualTo("ownerId", uid)
                .whereIn("status", java.util.Arrays.asList("pending", "active", "returning"))
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || value.isEmpty()) {
                        findViewById(R.id.btnActiveRental).setVisibility(View.GONE);
                        return;
                    }

                    com.google.firebase.firestore.DocumentSnapshot doc = value.getDocuments().get(0);
                    findViewById(R.id.btnActiveRental).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnActiveRental).setOnClickListener(v -> {
                        Intent intent = new Intent(this, ActiveRentalActivity.class);
                        intent.putExtra("sessionId", doc.getId());
                        intent.putExtra("userRole", "owner");
                        startActivity(intent);
                    });
                });
    }

    private void showSwitchDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_owner_switch);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
            android.view.WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.gravity = android.view.Gravity.BOTTOM;
            lp.y = 40;
            dialog.getWindow().setAttributes(lp);
        }

        dialog.findViewById(R.id.btnDialogPrimary).setOnClickListener(v -> {
            db.collection("users").document(mAuth.getUid())
                    .update("role", "customer")
                    .addOnSuccessListener(unused -> {
                        startActivity(new Intent(this, CustomerDashboardActivity.class));
                        finish();
                    });
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btnDialogSecondary).setOnClickListener(v -> {
            dialog.dismiss();
            signOut();
        });

        dialog.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenToMyVehicles();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listenerReg != null) listenerReg.remove();
    }

    private void listenToMyVehicles() {
        String uid = mAuth.getCurrentUser().getUid();
        listenerReg = db.collection("vehicles")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    vehicleList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Vehicle v = doc.toObject(Vehicle.class);
                        v.setId(doc.getId());
                        vehicleList.add(v);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(vehicleList.isEmpty() ? View.VISIBLE : View.GONE);
                });
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
                    .update("role", "customer")
                    .addOnSuccessListener(v -> {
                        startActivity(new Intent(this, CustomerDashboardActivity.class));
                        finish();
                    });
            return true;
        }
        if (item.getItemId() == R.id.action_signout) {
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        mAuth.signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener(task -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
