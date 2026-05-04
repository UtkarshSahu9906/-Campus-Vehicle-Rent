package com.college.vehiclerent;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.college.vehiclerent.model.RentalSession;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;
import java.util.Random;

public class ActiveRentalActivity extends AppCompatActivity {

    private String sessionId;
    private String userRole; // "owner" or "customer"
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private RentalSession session;

    private TextView tvStatus, tvVehicleName, tvPartnerInfo, tvTimer, tvCost, tvRate, tvWaiting;
    private View returnCodeCard, enterCodeCard;
    private android.widget.ImageView ivReturnQR;
    private MaterialButton btnConfirmPickup, btnReturnVehicle, btnVerifyCode;

    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_rental);

        sessionId = getIntent().getStringExtra("sessionId");
        userRole = getIntent().getStringExtra("userRole");
        db = FirebaseFirestore.getInstance();

        initViews();
        startListening();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvVehicleName = findViewById(R.id.tvVehicleName);
        tvPartnerInfo = findViewById(R.id.tvPartnerInfo);
        tvTimer = findViewById(R.id.tvTimer);
        tvCost = findViewById(R.id.tvCost);
        tvRate = findViewById(R.id.tvRate);
        tvWaiting = findViewById(R.id.tvWaiting);
        
        returnCodeCard = findViewById(R.id.returnCodeCard);
        enterCodeCard = findViewById(R.id.enterCodeCard);
        ivReturnQR = findViewById(R.id.ivReturnQR);

        btnConfirmPickup = findViewById(R.id.btnConfirmPickup);
        btnReturnVehicle = findViewById(R.id.btnReturnVehicle);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);

        btnConfirmPickup.setOnClickListener(v -> confirmPickup());
        btnReturnVehicle.setOnClickListener(v -> initiateReturn());
        btnVerifyCode.setOnClickListener(v -> scanReturnQR());
    }

    private void startListening() {
        listener = db.collection("rental_sessions").document(sessionId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    session = snapshot.toObject(RentalSession.class);
                    if (session != null) updateUI();
                });
    }

    private void updateUI() {
        tvVehicleName.setText(session.getVehicleType());
        tvRate.setText(String.format("Rate: ₹%.0f/hr", session.getPricePerHour()));
        
        if ("owner".equals(userRole)) {
            tvPartnerInfo.setText("Rented to: " + session.getCustomerName());
        } else {
            tvPartnerInfo.setText("Owner: " + session.getOwnerName());
        }

        String status = session.getStatus();
        resetVisibility();

        switch (status) {
            case "pending":
                tvStatus.setText("● Waiting for Pickup");
                if ("customer".equals(userRole)) {
                    btnConfirmPickup.setVisibility(View.VISIBLE);
                } else {
                    tvWaiting.setVisibility(View.VISIBLE);
                    tvWaiting.setText("Waiting for customer to confirm pickup...");
                }
                break;

            case "active":
                tvStatus.setText("● Active Ride");
                startTimer();
                if ("customer".equals(userRole)) {
                    btnReturnVehicle.setVisibility(View.VISIBLE);
                } else {
                    tvWaiting.setVisibility(View.VISIBLE);
                    tvWaiting.setText("Ride in progress...");
                }
                break;

            case "returning":
                tvStatus.setText("● Processing Return");
                stopTimer();
                calculateFinalCost();
                if ("customer".equals(userRole)) {
                    returnCodeCard.setVisibility(View.VISIBLE);
                    android.graphics.Bitmap qr = com.college.vehiclerent.utils.QRUtils.generateQRCode(sessionId, 500, 500);
                    ivReturnQR.setImageBitmap(qr);
                } else {
                    enterCodeCard.setVisibility(View.VISIBLE);
                }
                break;

            case "completed":
                tvStatus.setText("● Finished");
                stopTimer();
                tvCost.setText(String.format("₹%.2f", session.getTotalCost()));
                if ("customer".equals(userRole)) {
                    showRatingDialog();
                } else {
                    Toast.makeText(this, "Rental Completed!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }

    private void showRatingDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_switch_role); // Re-use layout structure
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }

        // Customize the re-used layout for rating
        ((TextView) dialog.findViewById(R.id.dialogTitle)).setText("Rate Your Ride!");
        ((TextView) dialog.findViewById(R.id.dialogMessage)).setText("How was your experience with the " + session.getVehicleType() + "?");
        ((MaterialButton) dialog.findViewById(R.id.btnDialogPrimary)).setText("Submit Rating");
        ((MaterialButton) dialog.findViewById(R.id.btnDialogSecondary)).setVisibility(View.GONE);
        ((MaterialButton) dialog.findViewById(R.id.btnDialogCancel)).setText("Skip");

        // Add a temporary RatingBar to the dialog
        android.widget.RatingBar rb = new android.widget.RatingBar(this);
        rb.setNumStars(5);
        rb.setStepSize(1);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 40, 0, 40);
        rb.setLayoutParams(lp);
        ((android.widget.LinearLayout) dialog.findViewById(R.id.dialogMessage)).getParent();
        // Since I'm re-using dialog_switch_role, I'll just add the rating bar above the buttons
        android.widget.LinearLayout root = (android.widget.LinearLayout) dialog.findViewById(R.id.btnDialogPrimary).getParent();
        root.addView(rb, 3); // Insert before buttons

        dialog.findViewById(R.id.btnDialogPrimary).setOnClickListener(v -> {
            float rating = rb.getRating();
            if (rating > 0) {
                db.collection("vehicles").document(session.getVehicleId())
                        .update("totalRating", com.google.firebase.firestore.FieldValue.increment(rating),
                                "ratingCount", com.google.firebase.firestore.FieldValue.increment(1))
                        .addOnSuccessListener(a -> {
                            Toast.makeText(this, "Thank you!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();
                        });
            } else {
                Toast.makeText(this, "Please select stars", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.findViewById(R.id.btnDialogCancel).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private void resetVisibility() {
        btnConfirmPickup.setVisibility(View.GONE);
        btnReturnVehicle.setVisibility(View.GONE);
        tvWaiting.setVisibility(View.GONE);
        returnCodeCard.setVisibility(View.GONE);
        enterCodeCard.setVisibility(View.GONE);
    }

    private void confirmPickup() {
        String uid = FirebaseAuth.getInstance().getUid();
        String name = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (name == null) name = "Student";

        db.collection("rental_sessions").document(sessionId)
                .update("status", "active", 
                        "startTime", System.currentTimeMillis(),
                        "customerId", uid,
                        "customerName", name)
                .addOnSuccessListener(a -> Toast.makeText(this, "Ride Started!", Toast.LENGTH_SHORT).show());
    }

    private void initiateReturn() {
        db.collection("rental_sessions").document(sessionId)
                .update("status", "returning", "endTime", System.currentTimeMillis())
                .addOnSuccessListener(a -> Toast.makeText(this, "Show the QR code to the owner", Toast.LENGTH_SHORT).show());
    }

    private void scanReturnQR() {
        com.google.zxing.integration.android.IntentIntegrator integrator = new com.google.zxing.integration.android.IntentIntegrator(this);
        integrator.setPrompt("Scan Customer's Return QR Code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        com.google.zxing.integration.android.IntentResult result = com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scannedSessionId = result.getContents();
                if (scannedSessionId.equals(sessionId)) {
                    completeReturn();
                } else {
                    Toast.makeText(this, "Invalid QR Code for this session", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void completeReturn() {
        calculateFinalCost();
        db.collection("rental_sessions").document(sessionId)
                .update("status", "completed", "totalCost", session.getTotalCost())
                .addOnSuccessListener(a -> {
                    // Mark vehicle as available again
                    db.collection("vehicles").document(session.getVehicleId())
                            .update("available", true);
                });
    }

    private void startTimer() {
        if (timerRunnable != null) return;
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - session.getStartTime();
                updateTimerUI(elapsed);
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            timerRunnable = null;
        }
    }

    private void updateTimerUI(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));

        double hoursElapsed = millis / (1000.0 * 60 * 60);
        double currentCost = hoursElapsed * session.getPricePerHour();
        tvCost.setText(String.format(Locale.getDefault(), "₹%.2f", currentCost));
    }

    private void calculateFinalCost() {
        long duration = session.getEndTime() - session.getStartTime();
        
        // Duration in hours
        double hours = duration / (1000.0 * 60 * 60);
        
        // Dual pricing logic:
        double finalCost = 0;
        double pricePerHour = session.getPricePerHour();
        double pricePerDay = session.getPricePerDay();
        
        // If pricePerDay is set and valid, use the best combination
        if (pricePerDay > 0) {
            int days = (int) (hours / 24);
            double remainingHours = hours % 24;
            
            // If remaining hours cost more than a full day, cap it to a full day cost
            double hoursCost = remainingHours * pricePerHour;
            if (hoursCost > pricePerDay) {
                hoursCost = pricePerDay;
            }
            finalCost = (days * pricePerDay) + hoursCost;
        } else {
            finalCost = hours * pricePerHour;
        }
        
        // Enforce minimum 1 hour cost
        if (finalCost < pricePerHour) {
            finalCost = pricePerHour;
        }

        session.setTotalCost(finalCost);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
        stopTimer();
    }
}
