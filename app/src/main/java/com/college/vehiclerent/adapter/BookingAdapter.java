package com.college.vehiclerent.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.college.vehiclerent.ActiveRentalActivity;
import com.college.vehiclerent.R;
import com.college.vehiclerent.model.RentalSession;
import com.google.android.material.button.MaterialButton;

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

        holder.tvVehicleName.setText(session.getVehicleType());
        
        if (isOwnerMode) {
            holder.tvOwnerName.setText("Rented to: " + session.getCustomerName());
        } else {
            holder.tvOwnerName.setText("Owner: " + session.getOwnerName());
        }

        String status = session.getStatus();
        holder.tvStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));
        
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
            Intent intent = new Intent(context, ActiveRentalActivity.class);
            intent.putExtra("sessionId", session.getId());
            intent.putExtra("userRole", isOwnerMode ? "owner" : "customer");
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvVehicleName, tvStatus, tvOwnerName, tvCost;
        MaterialButton btnViewDetails;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVehicleName = itemView.findViewById(R.id.tvVehicleName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvOwnerName = itemView.findViewById(R.id.tvOwnerName);
            tvCost = itemView.findViewById(R.id.tvCost);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}
