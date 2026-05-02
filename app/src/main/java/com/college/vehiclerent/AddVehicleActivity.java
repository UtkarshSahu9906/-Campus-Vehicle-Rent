package com.college.vehiclerent;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.college.vehiclerent.model.Vehicle;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;

public class AddVehicleActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PERMISSION_REQUEST  = 200;

    private ImageView imgVehicle;
    private EditText  etVehicleType, etDescription, etPrice, etMobile, etLocation;
    private AutoCompleteTextView spinnerCategory;
    private Button    btnSave;
    private ProgressBar progressBar;

    private Uri imageUri;
    private FirebaseAuth      mAuth;
    private FirebaseFirestore db;

    // If editing, store the vehicleId
    private String editVehicleId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vehicle);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mAuth   = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();

        imgVehicle    = findViewById(R.id.imgVehicle);
        etVehicleType = findViewById(R.id.etVehicleType);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etLocation    = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        etPrice       = findViewById(R.id.etPrice);
        etMobile      = findViewById(R.id.etMobile);
        btnSave       = findViewById(R.id.btnSave);
        progressBar   = findViewById(R.id.progressBar);

        setupCategorySpinner();

        // Check if editing existing vehicle
        Intent intent = getIntent();
        if (intent.hasExtra("vehicleId")) {
            editVehicleId = intent.getStringExtra("vehicleId");
            etVehicleType.setText(intent.getStringExtra("vehicleType"));
            spinnerCategory.setText(intent.getStringExtra("category"), false);
            etLocation.setText(intent.getStringExtra("location"));
            etDescription.setText(intent.getStringExtra("description"));
            etPrice.setText(String.valueOf(intent.getDoubleExtra("pricePerHour", 0)));
            etMobile.setText(intent.getStringExtra("mobileNo"));
            getSupportActionBar().setTitle("Edit Vehicle");
            btnSave.setText("Update Vehicle");
        } else {
            getSupportActionBar().setTitle("Add Vehicle");
        }
    }

    private void setupCategorySpinner() {
        String[] categories = {"Bike", "Scooter", "Car", "Cycle"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(adapter);
    }

        imgVehicle.setOnClickListener(v -> checkPermissionAndPick());
        btnSave.setOnClickListener(v -> validateAndSave());
    }

    // ── Permission handling ────────────────────────────────────────────────────

    private void checkPermissionAndPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST);
                return;
            }
        }
        openImagePicker();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            Toast.makeText(this, "Permission denied to read images", Toast.LENGTH_SHORT).show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).centerCrop().into(imgVehicle);
        }
    }

    // ── Validation & Save ─────────────────────────────────────────────────────

    private void validateAndSave() {
        String type     = etVehicleType.getText().toString().trim();
        String category = spinnerCategory.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String desc     = etDescription.getText().toString().trim();
        String price    = etPrice.getText().toString().trim();
        String mobile   = etMobile.getText().toString().trim();

        if (type.isEmpty()) { etVehicleType.setError("Enter vehicle model"); return; }
        if (category.isEmpty()) { spinnerCategory.setError("Select category"); return; }
        if (location.isEmpty()) { etLocation.setError("Enter pickup location"); return; }
        if (price.isEmpty()) { etPrice.setError("Enter price per hour"); return; }
        if (mobile.isEmpty() || mobile.length() < 10) { etMobile.setError("Enter valid 10-digit mobile"); return; }

        if (imageUri == null && editVehicleId == null) {
            Toast.makeText(this, "Please select a vehicle image", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (imageUri != null) {
            uploadImageThenSave(type, category, location, desc, Double.parseDouble(price), mobile);
        } else {
            // Editing without changing the image — keep old URL
            String oldUrl = getIntent().getStringExtra("imageUrl");
            saveToFirestore(type, category, location, desc, Double.parseDouble(price), mobile, oldUrl);
        }
    }

    private void uploadImageThenSave(String type, String category, String location, String desc, double price, String mobile) {
        byte[] data = getCompressedImageData(imageUri);
        if (data == null) {
            onSaveFail("Failed to process image");
            return;
        }

        MediaManager.get().upload(data)
                .unsigned("ml_default1121")
                .option("folder", "vehicles")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveToFirestore(type, category, location, desc, price, mobile, imageUrl);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        onSaveFail("Cloudinary Upload Error: " + error.getDescription());
                    }
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private byte[] getCompressedImageData(Uri uri) {
        try {
            // 1. Downsample for memory efficiency
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
            if (bitmap == null) return null;

            // 2. Compress to JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void saveToFirestore(String type, String category, String location, String desc, double price, String mobile, String imageUrl) {
        String uid  = mAuth.getCurrentUser().getUid();
        String name = mAuth.getCurrentUser().getDisplayName();

        if (editVehicleId != null) {
            // UPDATE existing document
            db.collection("vehicles").document(editVehicleId)
                    .update("vehicleType", type,
                            "category", category,
                            "location", location,
                            "description", desc,
                            "pricePerHour", price,
                            "mobileNo", mobile,
                            "imageUrl", imageUrl)
                    .addOnSuccessListener(v -> onSaveSuccess("Vehicle updated!"))
                    .addOnFailureListener(e -> onSaveFail("Update failed: " + e.getMessage()));
        } else {
            // ADD new document
            Vehicle vehicle = new Vehicle(uid, name, type, category, location, desc, imageUrl, price, mobile);
            db.collection("vehicles").add(vehicle)
                    .addOnSuccessListener(ref -> onSaveSuccess("Vehicle listed!"))
                    .addOnFailureListener(e -> onSaveFail("Failed to save: " + e.getMessage()));
        }
    }

    private void onSaveSuccess(String msg) {
        setLoading(false);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void onSaveFail(String msg) {
        setLoading(false);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
