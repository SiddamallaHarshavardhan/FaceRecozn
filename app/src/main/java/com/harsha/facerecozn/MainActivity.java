package com.harsha.facerecozn;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button register,recognize;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FindViewById();
        setOnClickListerners();

    }

//    method for finding id's
    private void FindViewById() {
        register=findViewById(R.id.button_register);
        recognize=findViewById(R.id.button_recognition);
    }

    //    onclick method
    private void setOnClickListerners() {
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this, Register.class);
                startActivity(intent);
            }
        });
    }





}