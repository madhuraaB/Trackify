package com.example.trackify;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater; // Fragment Imports
import android.view.View;           // Fragment Imports
import android.view.ViewGroup;        // Fragment Imports
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Fragment Annotations
import androidx.annotation.Nullable; // Fragment Annotations
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment; // *** KEY CHANGE: Fragment Base Class ***
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.example.trackify.DatabaseHelper.Transaction;
import com.example.trackify.TransactionAdapter.OnTransactionActionListener;

// *** KEY CHANGE: Class now extends Fragment ***
public class ViewTransactionsFragment extends Fragment implements OnTransactionActionListener {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private TextView emptyStateText;
    private TextInputEditText editTextMonthYear;
    private TextView textViewTotalExpense;
    private TextView textViewAvailableBalance;

    private DatabaseHelper databaseHelper;
    private String userEmail;
    private Calendar selectedMonth;
    private SharedPreferences sharedPreferences;

    private static final String PREF_NAME = "TrackifyPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "loggedInEmail";
    private static final String DATE_FORMAT_DB = "yyyy-MM";
    private static final String DATE_FORMAT_DISPLAY = "MMMM yyyy";

    // *** KEY CHANGE: onCreateView() replaces setContentView() ***
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 1. Inflate the layout to get the root View
        View view = inflater.inflate(R.layout.activity_view_transactions, container, false);

        // 2. Initialize context-dependent fields (using getContext())
        databaseHelper = new DatabaseHelper(getContext());
        sharedPreferences = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        userEmail = sharedPreferences.getString(KEY_LOGGED_IN_EMAIL, null);
        selectedMonth = Calendar.getInstance();

        // 3. Initialize UI components using 'view.findViewById'
        MaterialButton buttonBackToHome = view.findViewById(R.id.button_back_to_home);
        recyclerView = view.findViewById(R.id.recycler_view_transactions);
        emptyStateText = view.findViewById(R.id.text_no_transactions);
        editTextMonthYear = view.findViewById(R.id.edit_text_month_year);
        textViewTotalExpense = view.findViewById(R.id.text_view_total_expense);
        textViewAvailableBalance = view.findViewById(R.id.text_view_available_balance);

        updateMonthInView();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext())); // Use getContext()

        // 4. Set Listeners (use getActivity().finish() to close the host Activity)
        buttonBackToHome.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
        editTextMonthYear.setOnClickListener(v -> showMonthYearPickerDialog());

        // Note: The getSupportActionBar().hide() call is removed, as it's the Host Activity's job.

        return view;
    }

    // *** KEY CHANGE: onResume is still used to load data ***
    @Override
    public void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void showMonthYearPickerDialog() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, monthOfYear, dayOfMonth) -> {
            selectedMonth.set(Calendar.YEAR, year);
            selectedMonth.set(Calendar.MONTH, monthOfYear);
            selectedMonth.set(Calendar.DAY_OF_MONTH, 1);

            updateMonthInView();
            loadTransactions();
        };

        // *** KEY CHANGE: Use getActivity() for the DatePickerDialog ***
        DatePickerDialog dialog = new DatePickerDialog(getActivity(), 0, dateSetListener,
                selectedMonth.get(Calendar.YEAR),
                selectedMonth.get(Calendar.MONTH),
                selectedMonth.get(Calendar.DAY_OF_MONTH));

        // Custom logic to hide day selection (getResources is fine)
        dialog.show();
        DatePicker datePicker = findDatePicker(dialog);
        if (datePicker != null) {
            int dayId = getResources().getIdentifier("day", "id", "android");
            if (dayId != 0) {
                // *** KEY CHANGE: getWindow() is only available on the Activity ***
                View dayView = datePicker.findViewById(dayId);
                if (dayView != null) {
                    dayView.setVisibility(View.GONE);
                }
            }
        }
    }

    // Helper method to find the DatePicker (modified to accept the dialog directly)
    private DatePicker findDatePicker(DatePickerDialog dialog) {
        try {
            // *** KEY CHANGE: Use dialog.findViewById ***
            DatePicker dp = (DatePicker) dialog.findViewById(getResources().getIdentifier("datePicker", "id", "android"));
            if (dp != null) return dp;
        } catch (Exception e) {
            // Ignore exception if the standard ID isn't found
        }
        // Fallback: iterate through view group
        return findDatePicker((ViewGroup) dialog.getWindow().getDecorView());
    }

    // This helper method remains largely the same
    private DatePicker findDatePicker(ViewGroup group) {
        if (group != null) {
            for (int i = 0, j = group.getChildCount(); i < j; i++) {
                View child = group.getChildAt(i);
                if (child instanceof DatePicker) {
                    return (DatePicker) child;
                } else if (child instanceof ViewGroup) {
                    DatePicker result = findDatePicker((ViewGroup) child);
                    if (result != null)
                        return result;
                }
            }
        }
        return null;
    }


    private void updateMonthInView() {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_DISPLAY, Locale.US);
        editTextMonthYear.setText(sdf.format(selectedMonth.getTime()));
    }

    private void loadTransactions() {
        if (userEmail == null) {
            // *** KEY CHANGE: Use getContext() for Toast ***
            Toast.makeText(getContext(), "User session not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdfDb = new SimpleDateFormat(DATE_FORMAT_DB, Locale.US);
        String monthYearFilter = sdfDb.format(selectedMonth.getTime());

        // Fetch and Display Summary
        double totalIncome = databaseHelper.getMonthlyTotal(userEmail, monthYearFilter, "Income");
        double totalExpense = databaseHelper.getMonthlyTotal(userEmail, monthYearFilter, "Expense");
        double availableBalance = totalIncome - totalExpense;

        // Format and display the values
        String currencyFormat = "₹ %.2f";

        textViewTotalExpense.setText(String.format(Locale.US, currencyFormat, totalExpense));
        textViewAvailableBalance.setText(String.format(Locale.US, currencyFormat, availableBalance));

        List<Transaction> transactions = databaseHelper.getMonthlyTransactions(userEmail, monthYearFilter);

        if (transactions.isEmpty()) {
            emptyStateText.setText("No transactions recorded for " + editTextMonthYear.getText().toString() + ".");
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            if (adapter == null) {
                // *** KEY CHANGE: Use getContext() for TransactionAdapter constructor ***
                adapter = new TransactionAdapter(getContext(), transactions, this, true);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateData(transactions);
            }
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Handles the Edit action by showing a confirmation dialog first.
     */
    @Override
    public void onTransactionEdit(Transaction transaction) {
        // *** KEY CHANGE: Use getContext() for AlertDialog.Builder ***
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Edit")
                .setMessage("Are you sure you want to edit this transaction? You will be taken to the edit screen.")
                .setPositiveButton("Edit", (dialog, which) -> {
                    // User confirmed, proceed to AddExpenseActivity
                    // *** KEY CHANGE: Use getContext() for Intent constructor ***
                    Intent intent = new Intent(getContext(), AddExpenseActivity.class);

                    intent.putExtra(AddExpenseActivity.EXTRA_TRANSACTION_ID, transaction.id);

                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Handles the Delete action by showing a confirmation dialog first.
     */
    @Override
    public void onTransactionDelete(long transactionId) {
        // *** KEY CHANGE: Use getContext() for AlertDialog.Builder ***
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you absolutely sure you want to permanently delete this transaction? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User confirmed, perform the database deletion
                    if (databaseHelper.deleteTransaction(transactionId)) {
                        // *** KEY CHANGE: Use getContext() for Toast ***
                        Toast.makeText(getContext(), "Transaction deleted successfully.", Toast.LENGTH_SHORT).show();
                        loadTransactions(); // Refresh the list
                    } else {
                        // *** KEY CHANGE: Use getContext() for Toast ***
                        Toast.makeText(getContext(), "Failed to delete transaction.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
}