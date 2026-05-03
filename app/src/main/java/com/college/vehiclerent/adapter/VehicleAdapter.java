package com.college.vehiclerent.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.college.vehiclerent.AddVehicleActivity;
import com.college.vehiclerent.R;
import com.college.vehiclerent.VehicleDetailActivity;
import com.college.vehiclerent.model.Vehicle;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private final Context       context;
    private final List<Vehicle> vehicleList;
    private final boolean       isOwnerMode;

    public VehicleAdapter(Context context, List<Vehicle> vehicleList, boolean isOwnerMode) {
        this.context     = context;
        this.vehicleList = vehicleList;
        this.isOwnerMode = isOwnerMode;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle v = vehicleList.get(position);

        holder.tvType.setText(v.getVehicleType());
        holder.tvPrice.setText("₹" + String.format("%.0f", v.getPricePerHour()) + "/hr");
        holder.tvLocation.setText(v.getLocation() != null ? v.getLocation() : "Unknown");
        holder.tvCategory.setText(v.getCategory());
        holder.tvSub.setText(isOwnerMode ? v.getDescription() : "by " + v.getOwnerName());

        Glide.with(context)
                .load(v.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.imgVehicle);

        // ── Tap → vehicle detail screen ────────────────────────────────────
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, VehicleDetailActivity.class);
            intent.putExtra("vehicleType",  v.getVehicleType());
            intent.putExtra("category",     v.getCategory());
            intent.putExtra("location",     v.getLocation());
            intent.putExtra("description",  v.getDescription());
            intent.putExtra("imageUrl",     v.getImageUrl());
            intent.putExtra("pricePerHour", v.getPricePerHour());
            intent.putExtra("mobileNo",     v.getMobileNo());
            intent.putExtra("ownerName",    v.getOwnerName());
            intent.putExtra("ownerUid",     v.getOwnerUid());
            context.startActivity(intent);
        });

        // ── Owner mode: long-press shows Edit / Delete dialog ──────────────
        if (isOwnerMode) {
            holder.itemView.setOnLongClickListener(view -> {
                new AlertDialog.Builder(context)
                        .setTitle(v.getVehicleType())
                        .setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
                            if (which == 0) {
                                editVehicle(v);
                            } else {
                                confirmDelete(v);
                            }
                        })
                        .show();
                return true;
            });
        }
    }

    @Override
    public int getItemCount() { return vehicleList.size(); }

    // ── Owner helpers ──────────────────────────────────────────────────────────

    private void editVehicle(Vehicle v) {
        Intent intent = new Intent(context, AddVehicleActivity.class);
        intent.putExtra("vehicleId",    v.getId());
        intent.putExtra("vehicleType",  v.getVehicleType());
        intent.putExtra("category",     v.getCategory());
        intent.putExtra("location",     v.getLocation());
        intent.putExtra("description",  v.getDescription());
        intent.putExtra("imageUrl",     v.getImageUrl());
        intent.putExtra("pricePerHour", v.getPricePerHour());
        intent.putExtra("mobileNo",     v.getMobileNo());
        context.startActivity(intent);
    }

    private void confirmDelete(Vehicle v) {
        android.app.Dialog dialog = new android.app.Dialog(context);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_delete_confirm);
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

        ((android.widget.TextView) dialog.findViewById(R.id.dialogMessage))
                .setText("Remove \"" + v.getVehicleType() + "\" from your listings? This can't be undone.");

        dialog.findViewById(R.id.btnDialogDelete).setOnClickListener(btn -> {
            FirebaseFirestore.getInstance()
                    .collection("vehicles")
                    .document(v.getId())
                    .delete()
                    .addOnSuccessListener(a -> Toast.makeText(context, "Deleted!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show());
            dialog.dismiss();
        });

        dialog.findViewById(R.id.btnDialogCancel).setOnClickListener(btn -> dialog.dismiss());

        dialog.show();
    }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        ImageView imgVehicle;
        TextView  tvType, tvPrice, tvSub, tvLocation, tvCategory;

        VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            imgVehicle = itemView.findViewById(R.id.imgVehicle);
            tvType     = itemView.findViewById(R.id.tvVehicleType);
            tvPrice    = itemView.findViewById(R.id.tvPrice);
            tvSub      = itemView.findViewById(R.id.tvSub);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }
}
