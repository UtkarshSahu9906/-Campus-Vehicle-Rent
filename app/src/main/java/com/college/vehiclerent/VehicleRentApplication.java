package com.college.vehiclerent;
import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class VehicleRentApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Cloudinary
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dzjwrkopb");
        config.put("api_key", "573213361223711");
        config.put("secure", "true");
        MediaManager.init(this, config);
    }
}
