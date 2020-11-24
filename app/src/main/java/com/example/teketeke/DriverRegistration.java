package com.example.teketeke;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.teketeke.databinding.ActivityDriverRegistrationBinding;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class DriverRegistration extends AppCompatActivity {
    private static final String TAG = DriverRegistration.class.getSimpleName();
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
    private String onlineDriverID;
    private DatabaseReference driverDatabaseRef;
    Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityDriverRegistrationBinding dataBinding = DataBindingUtil.setContentView(this,R.layout.activity_driver_registration);
        dataBinding.setDriverRegLink(new ClickListener(dataBinding));
        dataBinding.setDriverLogin(new ClickListener(dataBinding));
        context = getApplicationContext();

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);
    }
    public class ClickListener{
        ActivityDriverRegistrationBinding binding;
        public ClickListener(){}
        public ClickListener(ActivityDriverRegistrationBinding binding){
            this.binding = binding;
        }
        public void onClickDriverLoginBtn(View view ){
            String driverEmail = binding.driverEmailEtv.getText().toString();
            String driverPassword = binding.driverPasswordEtv.getText().toString();
            SignInDriver(driverEmail,driverPassword);

        }
        public void onClickDriverRegisterLink(View view ){
            binding.driverLoginBtn.setVisibility(View.GONE);
            binding.driverRegisterLink.setVisibility(View.GONE);
            binding.driverRegisterBtn.setVisibility(View.VISIBLE);

        }
        public void onClickDriverRegisterBtn(View view ){
            String driverEmail = binding.driverEmailEtv.getText().toString();
            String driverPassword = binding.driverPasswordEtv.getText().toString();
            RegisterDriver(driverEmail,driverPassword);

        }
    }

    private void SignInDriver(String driverEmail, String driverPassword) {
        if (TextUtils.isEmpty(driverEmail)){
            Toast.makeText(DriverRegistration.this,"Please Input your email address",Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(driverPassword)){
            Toast.makeText(DriverRegistration.this,"Please Input your password",Toast.LENGTH_SHORT).show();
        }else {
            loadingBar.setTitle("Driver sign in");
            loadingBar.setMessage("Signing in...");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(driverEmail,driverPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                Intent driverMap = new Intent(DriverRegistration.this,DriverMapsActivity.class);
                                startActivity(driverMap);
                                Toast.makeText(DriverRegistration.this,"Sign in  successful",Toast.LENGTH_SHORT).show();
                            }else {
                                Log.w(TAG, "signInUserWithEmail:failure", task.getException());
                                Toast.makeText(DriverRegistration.this, "Sign in failure", Toast.LENGTH_SHORT).show();
                            }
                            loadingBar.dismiss();
                        }
                    });
        }
    }

    private void RegisterDriver(String driverEmail, String driverPassword) {
        if (TextUtils.isEmpty(driverEmail)){
            Toast.makeText(DriverRegistration.this,"Please Input your email address",Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(driverPassword)){
            Toast.makeText(DriverRegistration.this,"Please Input your password",Toast.LENGTH_SHORT).show();
        }
        else{
            loadingBar.setTitle("Driver registration");
            loadingBar.setMessage("Registering...");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(driverEmail,driverPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                onlineDriverID = mAuth.getCurrentUser().getUid();
                                driverDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                                    .child("Users")
                                                    .child("Drivers")
                                                    .child(onlineDriverID);
                                driverDatabaseRef.setValue(true);
                                CheckGpsStatus();
//                                Intent driverMap = new Intent(DriverRegistration.this,DriverMapsActivity.class);
//                                startActivity(driverMap);
                                Toast.makeText(DriverRegistration.this,"Driver registration successful",Toast.LENGTH_SHORT).show();

                            }else {
                                Toast.makeText(DriverRegistration.this,"Driver registration unsuccessful",Toast.LENGTH_SHORT).show();
                            }
                            loadingBar.dismiss();
                        }
                    });
        }
    }
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;
    LocationManager locationManager;
    boolean GpsStatus ;
    public void CheckGpsStatus(){
        locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(GpsStatus == true) {
            Intent driverMap = new Intent(DriverRegistration.this,DriverMapsActivity.class);
                                startActivity(driverMap);
        } else {
            createLocationRequestIntent();
        }
    }
    protected void createLocationRequestIntent(){
        Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent1);
        GpsStatus = true;
    }
    protected void createLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

//        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
//            @Override
//            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
//                try {
//                    task.getResult(ApiException.class);
//                }catch (ApiException e){
//                    switch (e.getStatusCode()){
//                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//                            try {
//                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
//                                resolvableApiException.startResolutionForResult(DriverRegistration.this,REQUEST_CHECK_SETTINGS);
//                            } catch (IntentSender.SendIntentException ex) {
//                                ex.printStackTrace();
//                            } catch (ClassCastException ex){
//                                ex.printStackTrace();
//                            }
//                            break;
//                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//                            break;
//                    }
//                    e.printStackTrace();
//                }
//            }
//        });

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                Intent driverMap = new Intent(DriverRegistration.this,DriverMapsActivity.class);
                startActivity(driverMap);
            }
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(DriverRegistration.this,
                                REQUEST_CHECK_SETTINGS);

                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case REQUEST_CHECK_SETTINGS:
                Intent driverMap = new Intent(DriverRegistration.this,DriverMapsActivity.class);
                startActivity(driverMap);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(GpsStatus == true) {
            CheckGpsStatus();
        }
    }
}