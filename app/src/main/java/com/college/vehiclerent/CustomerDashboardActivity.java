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
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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

        // Custom header in layout - no toolbar needed

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvEmpty      = findViewById(R.id.tvEmpty);
        etSearch     = findViewById(R.id.etSearch);
        chipGroup    = findViewById(R.id.chipGroup);
        recyclerView = findViewById(R.id.rvVehicles);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

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

        // Header Buttons
        findViewById(R.id.ivProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.btnShowMyQR).setOnClickListener(v -> showMyQRCode());

        // Bottom Nav
        findViewById(R.id.navExplore).setOnClickListener(v -> showToast("Already on Explore"));
        findViewById(R.id.navOrders).setOnClickListener(v -> startActivity(new Intent(this, CustomerBookingsActivity.class)));
        findViewById(R.id.navSwitchRole).setOnClickListener(v -> showRoleSwitchDialog());

        checkActiveSessions();
    }

    private void checkActiveSessions() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("rental_sessions")
                .whereEqualTo("customerId", uid)
                .whereIn("status", java.util.Arrays.asList("pending", "active", "returning"))
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || value.isEmpty()) {
                        findViewById(R.id.btnActiveRide).setVisibility(View.GONE);
                        return;
                    }

                    com.google.firebase.firestore.DocumentSnapshot doc = value.getDocuments().get(0);
                    String status = doc.getString("status");
                    
                    if ("pending".equals(status)) {
                        Intent intent = new Intent(this, RentalConfirmationActivity.class);
                        intent.putExtra("sessionId", doc.getId());
                        intent.putExtra("vehicleName", doc.getString("vehicleType"));
                        intent.putExtra("ownerName", doc.getString("ownerName"));
                        intent.putExtra("rateHour", doc.getDouble("pricePerHour"));
                        intent.putExtra("rateDay", doc.getDouble("pricePerDay"));
                        startActivity(intent);
                    } else {
                        findViewById(R.id.btnActiveRide).setVisibility(View.VISIBLE);
                        findViewById(R.id.btnActiveRide).setOnClickListener(v -> {
                            Intent intent = new Intent(this, ActiveRentalActivity.class);
                            intent.putExtra("sessionId", doc.getId());
                            intent.putExtra("userRole", "customer");
                            startActivity(intent);
                        });
                    }
                });
    }

    private void showMyQRCode() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_confirm); // Re-use an existing dialog layout for simplicity
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        // Generate QR
        android.graphics.Bitmap qrBitmap = com.college.vehiclerent.utils.QRUtils.generateQRCode(uid, 500, 500);

        // Modify the dialog view to show the QR Code
        android.widget.TextView tvTitle = dialog.findViewById(R.id.dialogTitle);
        android.widget.TextView tvMsg = dialog.findViewById(R.id.dialogMessage);
        com.google.android.material.button.MaterialButton btnPrimary = dialog.findViewById(R.id.btnDialogDelete);
        com.google.android.material.button.MaterialButton btnCancel = dialog.findViewById(R.id.btnDialogCancel);

        tvTitle.setText("My QR Code");
        tvMsg.setText("Show this to the owner to start the rental.");
        btnPrimary.setVisibility(View.GONE);
        btnCancel.setText("Close");

        // Add ImageView dynamically above the buttons
        android.widget.ImageView ivQr = new android.widget.ImageView(this);
        ivQr.setImageBitmap(qrBitmap);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 500);
        params.setMargins(0, 32, 0, 32);
        ivQr.setLayoutParams(params);

        android.widget.LinearLayout parent = (android.widget.LinearLayout) tvMsg.getParent();
        parent.addView(ivQr, 2); // Insert after message

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showRoleSwitchDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_switch_role);
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
                    .update("role", "owner")
                    .addOnSuccessListener(unused -> {
                        startActivity(new Intent(this, OwnerDashboardActivity.class));
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

    private void signOut() {
        mAuth.signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener(t -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
                .whereEqualTo("visible", true)
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
