package com.example.teketeke;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback{

    public static final int LOCATION_PERMISSION_REQUEST_CODE = 12;
    public static final String TAG = "DriverMapsActivity";
    private static final float DEFAULT_ZOOM = 15f;
    private GoogleMap mMap;
    private Boolean mLocationPermissionGranted = false;
    private Boolean driverLoginStatus = false;


    private FusedLocationProviderClient mFusedLocationProviderClient;

    GoogleApiClient apiClient;
    LocationRequest locationRequest;
    Marker customerMarker;

    private DatabaseReference mDriverAvailableRef;
    private GeoFire mGeoFireWorking, mGeoFireAvailable;
    private boolean requestingLocationUpdates = false;
    private FirebaseAuth mDriversAuth;
    private FirebaseUser currentDriver;
    private DatabaseReference mDriverWorkingRef, mAssignedCustomerRef, mAssignedCustomerLocationRef;

    private String driverID, assignedCustomerID = "null";
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                // Update UI with location data
                currentLocation = location;
                Log.d("TAG", "onLocationResult: " + currentLocation.getLatitude());
                initMap();
                updateUserLocationInDatabase(currentLocation);
                requestingLocationUpdates = true;
            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_maps);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        createLocationRequest();
        settingsCheck();
        //getLocationPermissions();
        mDriversAuth = FirebaseAuth.getInstance();
        driverID = mDriversAuth.getCurrentUser().getUid();
        currentDriver = mDriversAuth.getCurrentUser();
        driverLoginStatus = true;
//        getAssignedCustomerRequest();

    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    private void settingsCheck() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                Log.d("TAG", "onSuccess: settingsCheck");
                startLocationUpdates();
//                getCurrentLocation();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    Log.d("TAG", "onFailure: settingsCheck");
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(DriverMapsActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates(){
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }


    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                Log.d("TAG", "onSuccess: getLastLocation");
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    currentLocation=location;
                    Log.d("TAG", "onSuccess:latitude "+location.getLatitude());
                    Log.d("TAG", "onSuccess:longitude "+location.getLongitude());
                }else{
                    Log.d("TAG", "location is null");
//                    buildLocationCallback();
                }
            }
        });
        locationTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG,e.getLocalizedMessage());
            }
        });
    }


    //called after user responds to location settings popup
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("TAG", "onActivityResult: ");
        if(requestCode==REQUEST_CHECK_SETTINGS && resultCode==RESULT_OK){
            startLocationUpdates();
//            settingsCheck();
        }
//            getCurrentLocation();
        if(requestCode==REQUEST_CHECK_SETTINGS && resultCode==RESULT_CANCELED)
            Toast.makeText(this, "Please enable Location settings...!!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            return;
        }else {
//            settingsCheck();
        }
    }
    //called after user responds to location permission popup
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if (grantResults.length > 0 ){
                    for(int i = 0; i < permissions.length; i++){
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    startLocationUpdates();
                }
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if(requestingLocationUpdates){
            stopLocationUpdates();
            requestingLocationUpdates = false;
        }

    }

    private void initMap() {
        if (currentLocation != null) {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }else {
          startLocationUpdates();
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                        DEFAULT_ZOOM, "Your Location");

    }
    private void moveCamera(LatLng latLng,float zoom,String title){
        Log.d(TAG,"moveCamera : moving the camera to: lat; "+latLng.latitude +" , lng "+ latLng.longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
        MarkerOptions options =new MarkerOptions()
                .position(latLng)
                .title(title);
        if (customerMarker != null){
                        customerMarker.remove();
                    }
        customerMarker = mMap.addMarker(options);
        if (requestingLocationUpdates){
            stopLocationUpdates();
            requestingLocationUpdates = false;
        }

    }
    //    private void UpdateUIWithLocationData(Location location) {
//        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        moveCamera(latLng, DEFAULT_ZOOM, "New location");
//        updateUserLocationInDatabase(location);
//    }

    private void updateUserLocationInDatabase(Location location) {
        if (getApplicationContext() != null) {
            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Available Driver");
            mGeoFireAvailable = new GeoFire(mDriverAvailableRef);

            mDriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
            mGeoFireWorking = new GeoFire(mDriverWorkingRef);

            switch (assignedCustomerID){
                case "null":
                    mGeoFireWorking.removeLocation(userID);
                    mGeoFireAvailable.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;
                default:
                    mGeoFireAvailable.removeLocation(userID);
                    mGeoFireWorking.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                    break;

            }
        }
    }
//
//    private void initMap() {
//        if (mLocationPermissionGranted && currentLocation != null) {
//            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                    .findFragmentById(R.id.map);
//            mapFragment.getMapAsync(this);
//        }else {
////            getDriverDeviceLocation();
//        }
//    }
//
//
//    @Override
//    public void onMapReady(GoogleMap googleMap) {
//
//        if (mLocationPermissionGranted) {
//
//            if (currentLocation != null) {
//                mMap = googleMap;
//                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
//                        DEFAULT_ZOOM, "Your Location");
//
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//                    return;
//                }
//                mMap.setMyLocationEnabled(true);
//                mMap.getUiSettings().setMyLocationButtonEnabled(false);
//            }else {
////                getDriverDeviceLocation();
//            }
//}
//
//    }
//
//    //TODO::comment out the four methods.
//
////    @Override
////    public void onConnected(@Nullable Bundle bundle) {
////        locationRequest = new LocationRequest();
////        locationRequest.setInterval(1000);
////        locationRequest.setFastestInterval(1000);
////        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);
////        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
////            return;
////        }
////        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, locationRequest, this);
////
////    }
////
////    @Override
////    public void onConnectionSuspended(int i) {
////
////    }
////
////    @Override
////    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
////
////    }
////
////    @Override
////    public void onLocationChanged(Location location) {
////        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
////        moveCamera(latLng, DEFAULT_ZOOM, "New location");
////
////        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
////        mDriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Available Driver");
////        mGeoFire = new GeoFire(mDriverAvailableRef);
////        mGeoFire.setLocation(userID,new GeoLocation(location.getLatitude(),location.getLongitude()));
////
////
////    }
//
//    private void startLocationUpdates() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//            return;
//        }
//        mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
//                locationCallback,
//                Looper.getMainLooper());
//
//    }
//    private void moveCamera(LatLng latLng,float zoom,String title){
//        Log.d(TAG,"moveCamera : moving the camera to: lat; "+latLng.latitude +" , lng "+ latLng.longitude);
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
//        MarkerOptions options =new MarkerOptions()
//                .position(latLng)
//                .title(title);
//        mMap.addMarker(options);
//    }
//    private void getDriverDeviceLocation(){
//        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
//        try {
//            final Task location = mFusedLocationProviderClient.getLastLocation();
//            location.addOnCompleteListener(new OnCompleteListener() {
//                @Override
//                public void onComplete(@NonNull Task task) {
//                    if (task.isSuccessful()){
//                        Log.d(TAG,"onComplete: found location");
//                        currentLocation = (Location) task.getResult();
//                        if (currentLocation != null){
////                            initMap();
//                            Log.d(TAG,"onComplete: found location"+currentLocation.toString());
//                            Log.d(TAG,"onComplete: found location initializing map now");
////                            updateUserLocationInDatabase(currentLocation);
////                            requestingLocationUpdates = true;
//                        }else {
////                            getDriverDeviceLocation();
//                        }
//
//                        locationCallback = new LocationCallback(){
//                            @Override
//                            public void onLocationResult(LocationResult locationResult) {
//                                if (locationResult == null ){
//                                    return;
//                                }for (Location location : locationResult.getLocations()){
//                                    UpdateUIWithLocationData(location);
//
//                                }
//                            }
//                        };
//                    }else {
//                        Log.d(TAG,"onComplete : current location is null");
//                        Toast.makeText(DriverMapsActivity.this,"Unable to get the current location",Toast.LENGTH_SHORT).show();
//                        requestingLocationUpdates = false;
//                    }
//                }
//            });
//        }catch (SecurityException e){
//            Log.d(TAG,"getDriverDeviceLocation:SecurityException"+e.getMessage());
//        }
//
//    }
//    private void getLocationPermissions(){
//        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
//        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
//            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
//                mLocationPermissionGranted = true;
////                getDriverDeviceLocation();
//            }else {
//                ActivityCompat.requestPermissions(this,permissions, LOCATION_PERMISSION_REQUEST_CODE);
//            }
//        }else {
//            ActivityCompat.requestPermissions(this,permissions, LOCATION_PERMISSION_REQUEST_CODE);
//        }
//    }
//    @Override
//    protected void onResume() {
//        super.onResume();
//        if (requestingLocationUpdates) {
//            startLocationUpdates();
//        }
//    }
//
////    @Override
////    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
////        mLocationPermissionGranted = false;
////        switch (requestCode){
////            case LOCATION_PERMISSION_REQUEST_CODE:{
////                if (grantResults.length > 0 ) {
////                    for (int i =0 ; i < grantResults.length; i++){
////                        if(grantResults[i] == PackageManager.PERMISSION_GRANTED){
////                            mLocationPermissionGranted = false;
////                            return;
////                        }
////                    }
////                    mLocationPermissionGranted = true;
////                    if (mLocationPermissionGranted) {
////                        getDriverDeviceLocation();
////                    }
////
////                }
////            }
////        }
////    }
//    @Override
//    protected void onPause() {
//        super.onPause();
//        if(requestingLocationUpdates)
//        stopLocationUpdates();
//    }
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if(!driverLoginStatus){
////            toggleDriverAvailability();
//        }
//    }
//
//
//
//    private void stopLocationUpdates() {
//
//        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
//    }
//
//
//    private void toggleDriverAvailability(){
//        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        mDriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("Available Driver");
//        mGeoFireAvailable = new GeoFire(mDriverAvailableRef);
//        mGeoFireAvailable.removeLocation(userID);
//
//    }
//    private class ClickListener{
//        private void onClickDriverSignOutBtn(){
//            Intent signOut = new Intent(DriverMapsActivity.this,UserTypeActivity.class);
//            signOut.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(signOut);
//            finish();
//        }
//    }
//    private void getAssignedCustomerRequest() {
//        mAssignedCustomerRef = FirebaseDatabase.getInstance().getReference()
//                .child("Users")
//                .child("Drivers")
//                .child(driverID)
//                .child("CustomerRequestID");
//        mAssignedCustomerRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()){
//                    assignedCustomerID = dataSnapshot.getValue().toString();
//                    getAssignedCustomerLocation();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
//    }
//
//    private void getAssignedCustomerLocation() {
//        mAssignedCustomerLocationRef = FirebaseDatabase.getInstance().getReference()
//                .child("Customers Request")
//                .child(assignedCustomerID)
//                .child("l");
//        mAssignedCustomerLocationRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()){
//                    List<Object> assignedCustomerLocatonMap = (List<Object>) dataSnapshot.getValue();
//                    double latitude = 0;
//                    double longitude = 0;
//                    if (assignedCustomerLocatonMap.get(0) != null ){
//                        latitude = Double.parseDouble(assignedCustomerLocatonMap.get(0).toString());
//
//                    }
//                    if (assignedCustomerLocatonMap.get(1) != null){
//                        longitude = Double.parseDouble(assignedCustomerLocatonMap.get(1).toString());
//                    }
//                    LatLng customerLocationLatLng = new LatLng(latitude,longitude);
////                    if (customerMarker != null){
////                        customerMarker.remove();
////                    }
//                    mMap.addMarker(new MarkerOptions().position(customerLocationLatLng).title("Customer  location"));
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//
//            }
//        });
//    }
}