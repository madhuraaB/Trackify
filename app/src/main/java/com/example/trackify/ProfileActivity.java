package com.example.trackify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import com.example.trackify.DatabaseHelper.UserDetails; // Import UserDetails

public class ProfileActivity extends AppCompatActivity {

    private TextView textUserName;
    private TextView textUserEmail;
    private MaterialButton buttonLogout;
    private MaterialButton buttonBackToHome;

    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private String userEmail;

    private static final String PREF_NAME = "TrackifyPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "loggedInEmail";
    private static final String KEY_LOGGED_IN_NAME = "loggedInName"; // Used for clearing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize components
        textUserName = findViewById(R.id.text_user_name);
        textUserEmail = findViewById(R.id.text_user_email);
        buttonLogout = findViewById(R.id.button_profile_logout);
        buttonBackToHome = findViewById(R.id.button_back_to_home);

        // Initialize helper objects
        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Retrieve user email
        userEmail = sharedPreferences.getString(KEY_LOGGED_IN_EMAIL, null);

        // Setup Listeners
        buttonLogout.setOnClickListener(v -> logoutUser());
        buttonBackToHome.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load or reload user data every time the activity is resumed
        loadUserProfile();
    }

    private void loadUserProfile() {
        if (userEmail == null) {
            textUserName.setText("Error: Not logged in");
            textUserEmail.setText("Please log in again.");
            // Optionally, force logout
            logoutUser();
            return;
        }

        // Fetch user details using the new method in DatabaseHelper
        UserDetails userDetails = databaseHelper.getUserDetails(userEmail);

        if (userDetails != null) {
            textUserName.setText(userDetails.name);
            textUserEmail.setText(userDetails.email);
        } else {
            textUserName.setText("User Not Found");
            textUserEmail.setText(userEmail); // Display the email even if the name isn't found
            Toast.makeText(this, "Could not retrieve user name from database.", Toast.LENGTH_LONG).show();
        }
    }

    private void logoutUser() {
        // Clear session data from SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_LOGGED_IN_EMAIL);
        editor.remove(KEY_LOGGED_IN_NAME); // Clear the name as well
        editor.apply();

        // Navigate back to Login Activity and clear the stack
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        finish();
    }
}