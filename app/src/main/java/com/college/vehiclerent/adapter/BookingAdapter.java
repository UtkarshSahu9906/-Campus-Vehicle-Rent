package com.college.vehiclerent.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.college.vehiclerent.ActiveRentalActivity;
import com.college.vehiclerent.R;
import com.college.vehiclerent.model.RentalSession;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private final Context context;
    private final List<RentalSession> bookingList;
    private final boolean isOwnerMode;

    public BookingAdapter(Context context, List<RentalSession> bookingList, boolean isOwnerMode) {
        this.context = context;
        this.bookingList = bookingList;
        this.isOwnerMode = isOwnerMode;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        RentalSession session = bookingList.get(position);
        if (session == null) return;

        holder.tvVehicleName.setText(session.getVehicleType());
        
        if (isOwnerMode) {
            holder.tvOwnerName.setText("Rented to: " + (session.getCustomerName() != null ? session.getCustomerName() : "Customer"));
        } else {
            holder.tvOwnerName.setText("Owner: " + (session.getOwnerName() != null ? session.getOwnerName() : "Owner"));
        }

        String status = session.getStatus();
        if (status == null) status = "pending";
        
        holder.tvStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));
        
        // Manual Return Button Visibility
        if (isOwnerMode && (status.equals("active") || status.equals("returning"))) {
            holder.btnQuickReturn.setVisibility(View.VISIBLE);
            holder.btnQuickReturn.setOnClickListener(v -> showQuickReturnDialog(session));
        } else {
            holder.btnQuickReturn.setVisibility(View.GONE);
        }

        if (status.equals("completed")) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.primary));
            holder.tvCost.setText(String.format("Cost: ₹%.0f", session.getTotalCost()));
        } else if (status.equals("active")) {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            holder.tvCost.setText("Ongoing");
        } else if (status.equals("returning")) {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
            holder.tvCost.setText("Ongoing");
        } else {
            holder.tvStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
            holder.tvCost.setText("Pending");
        }

        holder.btnViewDetails.setOnClickListener(v -> {
            if (session.getId() == null) {
                Toast.makeText(context, "Session ID missing", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(context, ActiveRentalActivity.class);
            intent.putExtra("sessionId", session.getId());
            intent.putExtra("userRole", isOwnerMode ? "owner" : "customer");
            context.startActivity(intent);
        });
    }

    private void showQuickReturnDialog(RentalSession session) {
        new AlertDialog.Builder(context)
                .setTitle("Manual Return")
                .setMessage("Are you sure you want to manually complete this return? This will end the ride immediately.")
                .setPositiveButton("Complete", (dialog, which) -> performManualReturn(session))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performManualReturn(RentalSession session) {
        if (session.getId() == null) return;
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long endTime = System.currentTimeMillis();
        
        // Reuse calculation logic
        double finalCost = calculateCostForDuration(session.getStartTime(), endTime, session.getPricePerHour(), session.getPricePerDay());
        
        db.collection("rental_sessions").document(session.getId())
                .update("status", "completed", 
                        "endTime", endTime,
                        "totalCost", finalCost)
                .addOnSuccessListener(a -> {
                    // Mark vehicle available
                    if (session.getVehicleId() != null) {
                        db.collection("vehicles").document(session.getVehicleId())
                                .update("available", true);
                    }
                    Toast.makeText(context, "Ride Completed Manually", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error completing ride", Toast.LENGTH_SHORT).show());
    }

    private double calculateCostForDuration(long start, long end, double priceHour, double priceDay) {
        long duration = end - start;
        if (duration < 0) duration = 0;
        double hours = duration / (1000.0 * 60 * 60);
        
        double finalCost = 0;
        if (priceDay > 0) {
            int days = (int) (hours / 24);
            double remainingHours = hours % 24;
            double hoursCost = remainingHours * priceHour;
            if (hoursCost > priceDay) hoursCost = priceDay;
            finalCost = (days * priceDay) + hoursCost;
        } else {
            finalCost = hours * priceHour;
        }
        
        if (finalCost < priceHour) finalCost = priceHour;
        return finalCost;
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvVehicleName, tvStatus, tvOwnerName, tvCost;
        MaterialButton btnViewDetails, btnQuickReturn;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVehicleName = itemView.findViewById(R.id.tvVehicleName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOwnerName = itemView.findViewById(R.id.tvOwnerName);
            tvCost = itemView.findViewById(R.id.tvCost);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnQuickReturn = itemView.findViewById(R.id.btnQuickReturn);
        }
    }
}
