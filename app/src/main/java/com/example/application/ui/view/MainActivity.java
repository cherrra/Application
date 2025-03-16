package com.example.application.ui.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.application.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupButtons();
    }

    private void setupButtons() {
        findViewById(R.id.button_login).setOnClickListener(v -> openActivity(LoginActivity.class));
        findViewById(R.id.button_register).setOnClickListener(v -> openActivity(RegisterActivity.class));
    }

    private void openActivity(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }
}
