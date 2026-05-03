package com.college.vehiclerent.model;

import com.google.firebase.firestore.Exclude;

public class Vehicle {
    private String id;
    private String ownerUid;
    private String ownerName;
    private String vehicleType; // Specific model name (e.g. Activa 6G)
    private String category;    // Bike, Scooter, Car, Cycle
    private String location;    // e.g. Hostel A, Main Gate
    private String description;
    private String imageUrl;
    private double pricePerHour;
    private String mobileNo;
    private boolean available;
    private double totalRating;
    private int ratingCount;

    // Required empty constructor for Firestore
    public Vehicle() {}

    public Vehicle(String ownerUid, String ownerName, String vehicleType, String category,
                   String location, String description, String imageUrl,
                   double pricePerHour, String mobileNo) {
        this.ownerUid = ownerUid;
        this.ownerName = ownerName;
        this.vehicleType = vehicleType;
        this.category = category;
        this.location = location;
        this.description = description;
        this.imageUrl = imageUrl;
        this.pricePerHour = pricePerHour;
        this.mobileNo = mobileNo;
        this.available = true;
        this.totalRating = 0;
        this.ratingCount = 0;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerUid() { return ownerUid; }
    public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

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

    public double getTotalRating() { return totalRating; }
    public void setTotalRating(double totalRating) { this.totalRating = totalRating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    @Exclude
    public double getAverageRating() {
        return ratingCount > 0 ? totalRating / ratingCount : 0;
    }
}
