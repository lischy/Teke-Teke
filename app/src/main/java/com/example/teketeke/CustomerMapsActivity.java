package com.example.teketeke;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class CustomerMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationCallback locationCallback;
    private Boolean mLocationPermissionGranted = false;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 13;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    public static final String TAG = "CustomerMapsActivity";
    private static final float DEFAULT_ZOOM = 15f;
    private boolean requestingLocationUpdates = false;
    LocationRequest locationRequest;

    private GeoFire mGeoFire;

    private FirebaseAuth mCustomersAuth;
    private DatabaseReference mCustomerDatabaseRef,mDriverAvailable,driverRef,mDriverLocationRef;
    private FirebaseUser mCurrentUserRef;

    private String customerID;
    private LatLng customerPickUPLocation;
    Marker driverMarker;
    private int radius = 1;
    private boolean driverFound = false;
    private String driverFoundID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_maps);

        getLocationPermissions();
        mCustomersAuth =  FirebaseAuth.getInstance();
        mCurrentUserRef = mCustomersAuth.getCurrentUser();
        mDriverAvailable = FirebaseDatabase.getInstance().getReference().child("Available Driver");
        mDriverLocationRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
        customerID = mCurrentUserRef.getUid();
        Button callCabBtn = findViewById(R.id.callCabBtn);
        callCabBtn.setOnClickListener(new ClickListener());


    }
    private void getLocationPermissions(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;
                initMap();
            }else {
                ActivityCompat.requestPermissions(this,permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else {
            ActivityCompat.requestPermissions(this,permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if (grantResults.length > 0 ) {
                    for (int i =0 ; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    initMap();
                }
            }
        }
    }
    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mLocationPermissionGranted) {
            getCustomerDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }
    private void getCustomerDeviceLocation(){
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()){
                        Log.d(TAG,"onComplete: found location");
                        Location currentLocation = (Location) task.getResult();
                        customerPickUPLocation = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
                        moveCamera(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),
                                DEFAULT_ZOOM,"Your Location");
                        updateUserLocationInDatabase(currentLocation);
                        requestingLocationUpdates = true;
                        locationCallback = new LocationCallback(){
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                if (locationResult == null ){
                                    return;
                                }for (Location location : locationResult.getLocations()){
                                    UpdateUIWithLocationData(location);
                                }
                            }
                        };
                    }else {
                        Log.d(TAG,"onComplete : current location is null");
                        Toast.makeText(CustomerMapsActivity.this,"Unable to get the current location",Toast.LENGTH_SHORT).show();
                        requestingLocationUpdates = false;
                    }
                }
            });
        }catch (SecurityException e){
            Log.d(TAG,"getDriverDeviceLocation:SecurityException"+e.getMessage());
        }

    }
    private void moveCamera(LatLng latLng,float zoom,String title){
        Log.d(TAG,"moveCamera : moving the camera to: lat; "+latLng.latitude +" , lng "+ latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
        MarkerOptions options =new MarkerOptions()
                .position(latLng)
                .title(title);
        mMap.addMarker(options);
    }
    private void UpdateUIWithLocationData(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        moveCamera(latLng, DEFAULT_ZOOM, "New location");
        customerPickUPLocation = latLng;
        updateUserLocationInDatabase(location);

    }
    private void updateUserLocationInDatabase(Location location) {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG,"updateUserLocationInDatabase : userId : "+ userID);
        mCustomerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customers Request");
        mGeoFire = new GeoFire(mCustomerDatabaseRef);
        mGeoFire.setLocation(userID,new GeoLocation(location.getLatitude(),location.getLongitude()));
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

    }
    @Override
    protected void onPause() {
        super.onPause();
        if(requestingLocationUpdates)
            stopLocationUpdates();
    }
    private void stopLocationUpdates() {

        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }
    private class ClickListener implements View.OnClickListener{
        private void onClickDriverSignOutBtn(){
            Intent signOut = new Intent(CustomerMapsActivity.this,UserTypeActivity.class);
            signOut.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(signOut);
            finish();
        }

        private void onClickCallCabBtn(){
            //TODO:Change the text of the call cab btn to getting your driver
            getClosestDriverCab();

        }

        @Override
        public void onClick(View v) {
            getClosestDriverCab();
        }
    }

    private void getClosestDriverCab() {
        GeoFire geoFire = new GeoFire(mDriverAvailable);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(customerPickUPLocation.latitude,customerPickUPLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                //Called anytime the driver is found.
                if (!driverFound){
                    driverFound = true;
                    driverFoundID = key;

                    driverRef = FirebaseDatabase.getInstance().getReference()
                    .child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRequestID",customerID);
                    driverRef.updateChildren(driverMap);

                    retrieveDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                //called when the driver is not found and the search has to be repeated.
                if(!driverFound){
                    radius = radius + 1;
                    getClosestDriverCab();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void retrieveDriverLocation() {
        //TODO:change text of the call cab button to looking for driver location.
        mDriverLocationRef.child(driverFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                            double latitude = 0;
                            double longitude = 0;
                            //TODO:set text of the call cab btn to driver found.
                            if (driverLocationMap.get(0) != null ){
                                latitude = Double.parseDouble(driverLocationMap.get(0).toString());

                            }
                            if (driverLocationMap.get(1) != null){
                                longitude = Double.parseDouble(driverLocationMap.get(1).toString());
                            }
                            LatLng driverLocationLatLng = new LatLng(latitude,longitude);
                            if (driverMarker != null){
                                driverMarker.remove();
                            }
                            Location locationCustomer = new Location("");
                            locationCustomer.setLatitude(customerPickUPLocation.latitude);
                            locationCustomer.setLongitude(customerPickUPLocation.longitude);

                            Location locationDriver = new Location("");
                            locationDriver.setLatitude(driverLocationLatLng.latitude);
                            locationDriver.setLongitude(driverLocationLatLng.longitude);

                            float distanceDifference = locationCustomer.distanceTo(locationDriver);
                            //TODO: set the distance to the  call cab btn.
                            driverMarker = mMap.addMarker(new MarkerOptions().position(driverLocationLatLng).title("Delivery person location"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }
}