package com.example.teketeke;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.teketeke.databinding.ActivityUserTypeBinding;

public class UserTypeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUserTypeBinding binding = DataBindingUtil.setContentView(this,R.layout.activity_user_type);
        binding.setDriverClick(new ClickListener());
        binding.setCustomerClick(new ClickListener());

    }

 public class ClickListener{
        public void onClickDriverRegister(View view){
            Intent loginDriver = new Intent(UserTypeActivity.this,DriverRegistration.class);
            startActivity(loginDriver);
        }
        public void onClickCustomerRegister(View view){
            Intent loginCustomer = new Intent(UserTypeActivity.this,CustomerRegistration.class);
            startActivity(loginCustomer);

        }
 }
}