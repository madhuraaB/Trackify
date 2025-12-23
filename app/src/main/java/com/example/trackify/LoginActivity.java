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

import com.example.trackify.DatabaseHelper.UserDetails;
import java.util.Locale; // Added for email conversion

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private ProgressBar progressBar;
    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "TrackifyPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "loggedInEmail";
    private static final String KEY_LOGGED_IN_NAME = "loggedInName";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Check for existing session
        if (sharedPreferences.contains(KEY_LOGGED_IN_EMAIL)) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }


        editTextEmail = findViewById(R.id.edit_text_email_login); // CORRECT ID
        editTextPassword = findViewById(R.id.edit_text_password_login); // CORRECT ID
        buttonLogin = findViewById(R.id.button_login);
        progressBar = findViewById(R.id.progress_bar_login);
        TextView registerLink = findViewById(R.id.text_view_register_link);

        // Setup Listeners
        buttonLogin.setOnClickListener(v -> loginUser());
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        // Convert email to lowercase before checking, matching the storage format in DatabaseHelper
        String lowerCaseEmail = email.toLowerCase(Locale.ROOT);

        // 1. Check User Credentials in SQLite
        boolean isValid = databaseHelper.checkUser(lowerCaseEmail, password);

        progressBar.setVisibility(View.GONE);
        buttonLogin.setEnabled(true);

        if (isValid) {
            // 2. Fetch User Details to get the name
            UserDetails userDetails = databaseHelper.getUserDetails(lowerCaseEmail);
            String userName = (userDetails != null) ? userDetails.name : "User";

            // 3. Store session state and user name
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_LOGGED_IN_EMAIL, lowerCaseEmail);
            editor.putString(KEY_LOGGED_IN_NAME, userName);
            editor.apply();

            // 4. >>> NOTIFICATION ADDITION <<<
            // The call already exists, but we must ensure MainActivity has this method implemented.
            MainActivity.showWelcomeNotification(this, userName);

            // Navigate to main activity
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(LoginActivity.this, "Login failed: Invalid email or password.", Toast.LENGTH_LONG).show();
        }
    }
}