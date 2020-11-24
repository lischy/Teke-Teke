package com.example.teketeke;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.teketeke.databinding.ActivityCustomerRegistrationBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class CustomerRegistration extends AppCompatActivity {
    private static final String TAG = "Customer Activity class";
    private FirebaseAuth mAuth;
    private DatabaseReference customerDatabaseRef ;
    private ProgressDialog loadingBar;
    private String onlineCustomerID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCustomerRegistrationBinding binding = DataBindingUtil.setContentView(this,R.layout.activity_customer_registration);
        binding.setCustomerLogin(new ClickListener(binding));

        mAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);
    }
    public class ClickListener{
        ActivityCustomerRegistrationBinding binding;
        public ClickListener(){}
        public ClickListener(ActivityCustomerRegistrationBinding binding){
            this.binding = binding;
        }
        public void onClickCustomerLoginBtn(View view ){
            String customerEmail = binding.customerEmailEtv.getText().toString();
            String customerPassword = binding.customerPasswordEtv.getText().toString();
            SignInCustomer(customerEmail,customerPassword);

        }
        public void onClickCustomerRegisterLink(View view ){
            binding.customerLoginBtn.setVisibility(View.GONE);
            binding.customerRegisterLink.setVisibility(View.GONE);
            binding.customerRegisterBtn.setVisibility(View.VISIBLE);

        }
        public void onClickCustomerRegisterBtn(View view ){
            String customerEmail = binding.customerEmailEtv.getText().toString();
            String customerPassword = binding.customerPasswordEtv.getText().toString();
            RegisterCustomer(customerEmail,customerPassword);

        }
    }

    private void RegisterCustomer(String customerEmail, String customerPassword) {
        if(TextUtils.isEmpty(customerEmail)){
            Toast.makeText(CustomerRegistration.this,"Please input your email address",Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(customerPassword)){
            Toast.makeText(CustomerRegistration.this,"Please input your password",Toast.LENGTH_SHORT).show();
        }else {
            loadingBar.setTitle("Customer registration");
            loadingBar.setMessage("Registering...");
            loadingBar.show();
            mAuth.createUserWithEmailAndPassword(customerEmail,customerPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                onlineCustomerID = mAuth.getCurrentUser().getUid();
                                customerDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users")
                                        .child("Customers")
                                        .child(onlineCustomerID);
                                customerDatabaseRef.setValue(true);
                                Toast.makeText(CustomerRegistration.this,"Registration successful",Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                                Intent customerMap = new Intent(CustomerRegistration.this,CustomerMapsActivity.class);
                                startActivity(customerMap);
                            }else {
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(CustomerRegistration.this,"Registration unsuccessful",Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }
    }

    private void SignInCustomer(String customerEmail, String customerPassword) {
        if(TextUtils.isEmpty(customerEmail)){
            Toast.makeText(CustomerRegistration.this,"Please input your email address",Toast.LENGTH_SHORT).show();
        }
        if(TextUtils.isEmpty(customerPassword)){
            Toast.makeText(CustomerRegistration.this,"Please input your password",Toast.LENGTH_SHORT).show();
        }else {
            loadingBar.setTitle("Customer sign in");
            loadingBar.setMessage("Signing in...");
            loadingBar.show();
            mAuth.signInWithEmailAndPassword(customerEmail,customerPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()){
                                Intent customerMap = new Intent(CustomerRegistration.this,CustomerMapsActivity.class);
                                startActivity(customerMap);
                                Toast.makeText(CustomerRegistration.this,"Sign in successful",Toast.LENGTH_SHORT).show();
                            }else {
                                Log.w(TAG, "signInUserWithEmail:failure", task.getException());
                                Toast.makeText(CustomerRegistration.this,"Sign in unsuccessful",Toast.LENGTH_SHORT).show();
                            }
                            loadingBar.dismiss();
                        }
                    });
        }
    }
}