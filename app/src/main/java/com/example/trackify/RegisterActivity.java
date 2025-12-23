package com.example.trackify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextName, editTextEmail, editTextPassword;
    private Button buttonRegister;
    private ProgressBar progressBar;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "TrackifyPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "loggedInEmail";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Initialize Views
        editTextName = findViewById(R.id.edit_text_name);
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextPassword = findViewById(R.id.edit_text_password);
        buttonRegister = findViewById(R.id.button_register);
        progressBar = findViewById(R.id.progress_bar);
        TextView loginLink = findViewById(R.id.text_view_login_link);

        // Setup Listeners
        buttonRegister.setOnClickListener(v -> registerUser());
        loginLink.setOnClickListener(v -> {
            // Changed finish() to explicitly start LoginActivity to ensure a clean transition
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // 1. Client-side validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Basic Email validation (Optional, but good practice)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format.", Toast.LENGTH_LONG).show();
            return;
        }

        // Show progress and disable button
        progressBar.setVisibility(View.VISIBLE);
        buttonRegister.setEnabled(false);

        // 2. Insert User into SQLite Database
        int result = databaseHelper.insertUser(name, email, password);

        // Hide progress and re-enable button
        progressBar.setVisibility(View.GONE);
        buttonRegister.setEnabled(true);

        if (result == 1) {
            // Success: Navigate to Login activity and clear the back stack
            Toast.makeText(RegisterActivity.this, "Registration successful! Please log in.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);

            // Ensures the user can't press back to registration
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish();
        } else if (result == 0) {
            // Duplicate email
            Toast.makeText(RegisterActivity.this, "Registration failed: Email already registered.", Toast.LENGTH_LONG).show();
        } else {
            // Other Insertion failed (e.g., database error)
            Toast.makeText(RegisterActivity.this, "Registration failed due to an unexpected error. Result: " + result, Toast.LENGTH_SHORT).show();
        }
    }
}