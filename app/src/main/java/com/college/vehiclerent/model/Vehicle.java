package com.college.vehiclerent.model;

public class Vehicle {
    private String id;
    private String ownerUid;
    private String ownerName;
    private String vehicleType;
    private String description;
    private String imageUrl;
    private double pricePerHour;
    private String mobileNo;
    private boolean available;

    // Required empty constructor for Firestore
    public Vehicle() {}

    public Vehicle(String ownerUid, String ownerName, String vehicleType,
                   String description, String imageUrl,
                   double pricePerHour, String mobileNo) {
        this.ownerUid = ownerUid;
        this.ownerName = ownerName;
        this.vehicleType = vehicleType;
        this.description = description;
        this.imageUrl = imageUrl;
        this.pricePerHour = pricePerHour;
        this.mobileNo = mobileNo;
        this.available = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }

    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
