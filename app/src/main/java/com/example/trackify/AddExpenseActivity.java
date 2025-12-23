package com.example.trackify;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Ensure this import is present
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

// Import the Transaction class for convenience
import com.example.trackify.DatabaseHelper.Transaction;

public class AddExpenseActivity extends AppCompatActivity {

    private static final String TAG = "AddExpenseActivity"; // Used for Logcat

    private TextInputEditText editTextAmount;
    private TextInputEditText editTextNote;
    private TextInputEditText editTextDate;
    private RadioGroup radioGroupType;
    private AutoCompleteTextView spinnerCategory;
    private MaterialButton buttonSaveTransaction;
    private MaterialButton buttonBackToHome;

    private DatabaseHelper databaseHelper;
    private SharedPreferences sharedPreferences;
    private String userEmail;
    private Calendar selectedDate;

    // --- Edit Mode Variable ---
    private long transactionIdToEdit = -1;
    public static final String EXTRA_TRANSACTION_ID = "TRANSACTION_ID_TO_EDIT";

    // Constants
    private static final Locale INDIA_LOCALE = new Locale("en", "IN");
    private static final String PREF_NAME = "TrackifyPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "loggedInEmail";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    // UPDATED: Revised category lists based on user feedback (simpler structure)
    private static final List<String> EXPENSE_CATEGORIES = Arrays.asList(
            "Food", "Rent", "Transportation", "Utilities", "Groceries",
            "Health & Fitness", "Entertainment", "Shopping", "Travel", "Education",
            "Miscellaneous"
    );

    private static final List<String> INCOME_CATEGORIES = Arrays.asList(
            "Salary", "Business/Freelance", "Investments", "Rental Income",
            "Gifts", "Other"
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Initialization
        databaseHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        userEmail = sharedPreferences.getString(KEY_LOGGED_IN_EMAIL, null);
        selectedDate = Calendar.getInstance();

        // UI Binding
        editTextAmount = findViewById(R.id.edit_text_amount);
        editTextNote = findViewById(R.id.edit_text_note);
        editTextDate = findViewById(R.id.edit_text_date);
        radioGroupType = findViewById(R.id.radio_group_type);
        spinnerCategory = findViewById(R.id.spinner_category);
        buttonSaveTransaction = findViewById(R.id.button_save_transaction);
        buttonBackToHome = findViewById(R.id.button_back_to_home);

        // 1. --- EDIT MODE CHECK: This determines if we load data ---
        transactionIdToEdit = getIntent().getLongExtra(EXTRA_TRANSACTION_ID, -1);

        // <<< LOGGING STEP 1: Check if the ID was passed correctly >>>
        Log.d(TAG, "onCreate: Checking for edit mode. Received ID: " + transactionIdToEdit);

        if (transactionIdToEdit != -1) {
            // Load existing data for editing
            loadTransactionData(transactionIdToEdit);
            buttonSaveTransaction.setText("Update Transaction");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Transaction");
            }
        } else {
            // Default setup for new transaction
            updateDateInView();
            // Set default category list (Expense)
            setCategoryAdapter(EXPENSE_CATEGORIES);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Add New Transaction");
            }
        }

        // Listeners
        buttonBackToHome.setOnClickListener(v -> finish());
        buttonSaveTransaction.setOnClickListener(v -> saveTransaction());
        editTextDate.setOnClickListener(v -> showDatePickerDialog());

        // Set initial adapter before setting the listener
        onTransactionTypeChanged(radioGroupType.getCheckedRadioButtonId());
        radioGroupType.setOnCheckedChangeListener((group, checkedId) -> onTransactionTypeChanged(checkedId));
    }

    /**
     * Helper method to load existing data for edit mode on a background thread.
     */
    private void loadTransactionData(long id) {

        // Start a new thread for the database operation (The ANR fix)
        new Thread(() -> {
            // <<< LOGGING STEP 2: Confirm background thread started >>>
            Log.d(TAG, "loadTransactionData: Starting background thread to fetch transaction ID: " + id);

            Transaction transaction = databaseHelper.getTransactionById(id);

            // Update UI (must run on the Main Thread)
            runOnUiThread(() -> {
                if (transaction != null) {
                    // <<< LOGGING STEP 3: Check if data was retrieved >>>
                    Log.d(TAG, "loadTransactionData: SUCCESS! Loaded data. Amount=" + transaction.amount + ", Type=" + transaction.type);

                    // --- FIX: Ensure the amount is correctly set and formatted ---
                    DecimalFormat df = new DecimalFormat("0.00");
                    String formattedAmount = df.format(transaction.amount);
                    // Use post() to ensure text is set after the view is fully ready
                    editTextAmount.post(() -> editTextAmount.setText(formattedAmount));

                    // Set Note
                    editTextNote.setText(transaction.note);

                    // Set Transaction Type (Radio Group)
                    if (transaction.type.equals("Income")) {
                        radioGroupType.check(R.id.radio_income);
                        setCategoryAdapter(INCOME_CATEGORIES);
                    } else {
                        radioGroupType.check(R.id.radio_expense);
                        setCategoryAdapter(EXPENSE_CATEGORIES);
                    }

                    // Set Category Spinner after the adapter is confirmed
                    spinnerCategory.setText(transaction.category, false);

                    // Set Date Picker
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
                        selectedDate.setTime(sdf.parse(transaction.date));
                        updateDateInView();
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing saved date: " + transaction.date, e);
                        Toast.makeText(this, "Error parsing saved date.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // <<< LOGGING STEP 3 (FAILURE): Check if data was null >>>
                    Log.w(TAG, "loadTransactionData: FAILURE! Transaction returned null for ID: " + id + ". Check DatabaseHelper logs.");
                    Toast.makeText(this, "Transaction not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }).start();
    }


    private void showDatePickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, monthOfYear);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateInView();
        };

        new DatePickerDialog(this, dateSetListener,
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateInView() {
        // Display format: 15 Jun, 2024
        SimpleDateFormat sdfDisplay = new SimpleDateFormat("dd MMM, yyyy", INDIA_LOCALE);
        editTextDate.setText(sdfDisplay.format(selectedDate.getTime()));
    }

    private void onTransactionTypeChanged(int checkedId) {
        if (checkedId == R.id.radio_expense) {
            setCategoryAdapter(EXPENSE_CATEGORIES);
        } else if (checkedId == R.id.radio_income) {
            setCategoryAdapter(INCOME_CATEGORIES);
        }
    }

    private void setCategoryAdapter(List<String> categories) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories
        );
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setText("", false); // Clear category on type switch to force selection
    }

    private void saveTransaction() {
        String amountStr = editTextAmount.getText().toString();
        String note = editTextNote.getText().toString().trim();
        String category = spinnerCategory.getText().toString().trim();

        if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Amount and Category are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine transaction type and category list
        String type = radioGroupType.getCheckedRadioButtonId() == R.id.radio_income ? "Income" : "Expense";
        List<String> currentCategories = type.equals("Income") ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;

        // Validate category against the predefined list
        if (!currentCategories.contains(category)) {
            Toast.makeText(this, "Invalid category for " + type + ". Please select from the dropdown.", Toast.LENGTH_LONG).show();
            return;
        }

        if (userEmail == null) {
            Toast.makeText(this, "Authentication error. Please re-login.", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            // Ensure the decimal separator (period) is used for parsing
            amount = Double.parseDouble(amountStr.replace(',', '.'));
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than zero.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format. Please use numbers only.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Database date format: yyyy-MM-dd
        String date = new SimpleDateFormat(DATE_FORMAT, Locale.US).format(selectedDate.getTime());
        boolean success;

        // --- Logic to Differentiate Insert vs. Update ---
        if (transactionIdToEdit != -1) {
            // Update Existing Transaction
            success = databaseHelper.updateExpense(transactionIdToEdit, userEmail, category, amount, date, note, type);
            if (success) {
                Toast.makeText(this, type + " updated successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to update transaction. Check logs.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Insert New Transaction
            success = databaseHelper.insertExpense(userEmail, category, amount, date, note, type);
            if (success) {
                Toast.makeText(this, type + " saved successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save transaction. Check logs.", Toast.LENGTH_SHORT).show();
            }
        }

        if (success) {
            finish(); // Return to the previous activity (likely MainActivity or ViewAllTransactions)
        }
    }
}