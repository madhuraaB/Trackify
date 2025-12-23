package com.example.trackify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Imports for Notification
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.graphics.Color;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.example.trackify.DatabaseHelper.Transaction;
import com.example.trackify.TransactionAdapter.OnTransactionActionListener;

public class MainActivity extends AppCompatActivity implements OnTransactionActionListener {

    // --- UI Variables ---
    private TextView textViewWelcome, textMonthlyExpenseAmount, textTotalBalanceAmount;
    private TextView textNoRecentTransactions;
    private RecyclerView recyclerViewRecentTransactions;

    private MaterialButton buttonAddTransaction, buttonViewAllTransactions, buttonProfile;

    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private String userEmail;
    private String userName;

    // Constants for SharedPreferences
    private static final String PREF_NAME = "TrackifyPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "loggedInEmail";
    private static final String KEY_LOGGED_IN_NAME = "loggedInName";

    // Notification Constants
    private static final String CHANNEL_ID = "TrackifyWelcomeChannel";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 10;

    // 💰 FEATURE: Define the threshold for low balance alert
    private static final double LOW_BALANCE_THRESHOLD = 2000.00; // Alert if balance falls below ₹2000

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Helpers
        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 2. Retrieve User Session Info
        userEmail = sharedPreferences.getString(KEY_LOGGED_IN_EMAIL, null);
        userName = sharedPreferences.getString(KEY_LOGGED_IN_NAME, "User");

        if (userEmail == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // 3. Initialize UI Elements
        textViewWelcome = findViewById(R.id.text_view_welcome);
        textMonthlyExpenseAmount = findViewById(R.id.text_monthly_expense_amount);
        textTotalBalanceAmount = findViewById(R.id.text_total_balance_amount);
        textNoRecentTransactions = findViewById(R.id.text_no_recent_transactions);
        recyclerViewRecentTransactions = findViewById(R.id.recycler_view_recent_transactions);
        buttonAddTransaction = findViewById(R.id.button_add_transaction);
        buttonViewAllTransactions = findViewById(R.id.button_view_all_transactions);
        // Note: Assuming R.id.icon_profile is the correct ID for the profile button.
        buttonProfile = findViewById(R.id.icon_profile);

        textViewWelcome.setText(String.format("Welcome back, %s!", userName));

        // 4. Setup Listeners
        buttonAddTransaction.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddExpenseActivity.class)));
        // Note: Assuming HomeActivity is where your full transactions list is located.
        buttonViewAllTransactions.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HomeActivity.class)));
        buttonProfile.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));

        // 5. Setup RecyclerView
        recyclerViewRecentTransactions.setLayoutManager(new LinearLayoutManager(this));

        // 6. Notification Setup
        createNotificationChannel();
        // Request permission for API 33+ devices
        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
    }

    /**
     * Loads and displays the total balance, monthly expense, and recent transactions.
     */
    private void loadDashboardData() {

        // --- CURRENCY SETUP ---
        Locale indianLocale = new Locale("en", "IN");
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(indianLocale);
        // ----------------------

        // 1. Fetch total balance (Cumulative across all transactions)
        // Note: Assuming databaseHelper.getTotalBalance(userEmail) returns Net Balance (Income - Expense)
        double totalBalance = databaseHelper.getTotalBalance(userEmail);

        // Display Total Balance
        textTotalBalanceAmount.setText(currencyFormat.format(totalBalance));

        // 🚨 LOW BALANCE CHECK 🚨
        if (totalBalance < LOW_BALANCE_THRESHOLD) {
            showLowBalanceNotification(this, totalBalance);
        }

        // 2. Fetch monthly expense (for the current month)
        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1;
        int currentYear = calendar.get(Calendar.YEAR);
        // Note: Assuming DatabaseHelper.getMonthlyExpense exists and returns only total expenses
        double monthlyExpense = databaseHelper.getMonthlyExpense(userEmail, currentMonth, currentYear);

        // Set the text color based on the expense being non-zero
        if (monthlyExpense > 0) {
            // Note: R.color.expense_red must be defined in your colors.xml
            textMonthlyExpenseAmount.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
        } else {
            textMonthlyExpenseAmount.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
        }

        // Display Monthly Expense
        textMonthlyExpenseAmount.setText(currencyFormat.format(monthlyExpense));

        // 3. Fetch and display recent transactions (limit 3)
        List<Transaction> recentTransactions = databaseHelper.getRecentTransactions(userEmail, 3);

        if (recentTransactions.isEmpty()) {
            recyclerViewRecentTransactions.setVisibility(View.GONE);
            textNoRecentTransactions.setVisibility(View.VISIBLE);
        } else {
            recyclerViewRecentTransactions.setVisibility(View.VISIBLE);
            textNoRecentTransactions.setVisibility(View.GONE);

            // ✅ FIX APPLIED: Set 'showActions' to 'false' to hide Edit/Delete buttons.
            TransactionAdapter adapter = new TransactionAdapter(MainActivity.this, recentTransactions, this, false);
            recyclerViewRecentTransactions.setAdapter(adapter);
        }
    }


    // ----------------------------------------------------------------------
    // --- NEW Notification Method for Low Balance ---
    // ----------------------------------------------------------------------

    public static void showLowBalanceNotification(Context context, double balance) {
        final String CHANNEL_ID = MainActivity.CHANNEL_ID;
        final int NOTIFICATION_ID = 102; // Unique ID for Low Balance Alert

        // 1. Check for permission before attempting to notify (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("MainActivity", "Notification permission not granted. Cannot show low balance alert.");
                return;
            }
        }

        // 2. Format the balance for the message
        Locale indianLocale = new Locale("en", "IN");
        String formattedBalance = NumberFormat.getCurrencyInstance(indianLocale).format(balance);

        // 3. Build the notification content
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 CRITICAL ALERT: Low Balance")
                .setContentText("Your current balance is " + formattedBalance + ". Review your budget!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(Color.RED)
                .setAutoCancel(true);

        // 4. Show the notification
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(NOTIFICATION_ID, builder.build());
    }

    // ----------------------------------------------------------------------
    // --- Existing Notification Methods ---
    // ----------------------------------------------------------------------

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    // Static method to be called from LoginActivity
    public static void showWelcomeNotification(Context context, String userName) {
        int notificationId = 101;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("MainActivity", "Notification permission not granted. Cannot show welcome notification.");
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_person_24)
                .setContentTitle("Welcome to Trackify!")
                .setContentText("Hello, " + userName + "! Let's start tracking your finances.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        notificationManagerCompat.notify(notificationId, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Welcome Notifications";
            String description = "Notifications for user login and greetings";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. Alerts won't be shown.", Toast.LENGTH_LONG).show();
            }
        }
    }


    // --- Implementation of TransactionAdapter.OnTransactionActionListener (Unchanged) ---

    @Override
    public void onTransactionEdit(DatabaseHelper.Transaction transaction) {
        // This is only called if showActions was true, but kept for completeness
        Toast.makeText(this,
                "Transaction: " + transaction.category + ". Please go to 'View All Transactions' to edit/delete.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTransactionDelete(long transactionId) {
        // This is only called if showActions was true, but kept for completeness
        Toast.makeText(this, "Please go to 'View All Transactions' to delete items.", Toast.LENGTH_SHORT).show();
    }
}