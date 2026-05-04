package com.college.vehiclerent.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class RentalSession {
    private String id;
    private String vehicleId;
    private String vehicleType;
    private String ownerId;
    private String ownerName;
    private String customerId;
    private String customerName;
    private double pricePerHour;
    private double pricePerDay;
    private long startTime;    // millis
    private long endTime;      // millis
    private double totalCost;
    private String status;     // pending, active, returning, completed
    private String returnCode; // 4-digit code for pairing on return

    public RentalSession() {}

    public RentalSession(String vehicleId, String vehicleType,
                         String ownerId, String ownerName,
                         String customerId, String customerName,
                         double pricePerHour, double pricePerDay) {
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.customerId = customerId;
        this.customerName = customerName;
        this.pricePerHour = pricePerHour;
        this.pricePerDay = pricePerDay;
        this.status = "pending";
        this.startTime = 0;
        this.endTime = 0;
        this.totalCost = 0;
        this.returnCode = "";
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }

    public double getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(double pricePerDay) { this.pricePerDay = pricePerDay; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReturnCode() { return returnCode; }
    public void setReturnCode(String returnCode) { this.returnCode = returnCode; }
}
