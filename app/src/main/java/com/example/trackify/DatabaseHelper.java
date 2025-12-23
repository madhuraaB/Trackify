package com.example.trackify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Patterns;
import android.os.Build;

// ADDED IMPORTS FOR PASSWORD HASHING
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Info
    private static final String DATABASE_NAME = "trackify.db";
    private static final int DATABASE_VERSION = 3;
    private static final String TAG = "DatabaseHelper";

    // Table Names
    private static final String TABLE_EXPENSES = "expenses";
    private static final String TABLE_USERS = "users";

    // Expense Table Columns
    private static final String COLUMN_EXPENSE_ID = "expense_id";
    private static final String COLUMN_EXPENSE_USER_EMAIL = "user_email";
    private static final String COLUMN_EXPENSE_TYPE = "type"; // 'Income' or 'Expense'
    private static final String COLUMN_EXPENSE_CATEGORY = "category";
    private static final String COLUMN_EXPENSE_AMOUNT = "amount";
    private static final String COLUMN_EXPENSE_DATE = "date"; // YYYY-MM-DD format
    private static final String COLUMN_EXPENSE_NOTE = "note";

    // User Table Columns
    private static final String COLUMN_USER_EMAIL = "email";
    private static final String COLUMN_USER_PASSWORD = "password";
    private static final String COLUMN_USER_NAME = "name";

    //  UserDetails ---
    public static class UserDetails {
        public String email;
        public String name;

        public UserDetails(String email, String name) {
            this.email = email;
            this.name = name;
        }
    }

    // --- Model Class: Transaction ---
    public static class Transaction {
        public long id;
        public String userEmail;
        public String type;
        public String category;
        public double amount;
        public String date;
        public String note;

        public Transaction(long id, String userEmail, String type, String category, double amount, String date, String note) {
            this.id = id;
            this.userEmail = userEmail;
            this.type = type;
            this.category = category;
            this.amount = amount;
            this.date = date;
            this.note = note;
        }
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
        } else {
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_USER_EMAIL + " TEXT PRIMARY KEY,"
                + COLUMN_USER_NAME + " TEXT,"
                + COLUMN_USER_PASSWORD + " TEXT)";
        db.execSQL(CREATE_USERS_TABLE);

        String CREATE_EXPENSES_TABLE = "CREATE TABLE " + TABLE_EXPENSES + "("
                + COLUMN_EXPENSE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EXPENSE_USER_EMAIL + " TEXT,"
                + COLUMN_EXPENSE_TYPE + " TEXT,"
                + COLUMN_EXPENSE_CATEGORY + " TEXT,"
                + COLUMN_EXPENSE_AMOUNT + " REAL,"
                + COLUMN_EXPENSE_DATE + " TEXT,"
                + COLUMN_EXPENSE_NOTE + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_EXPENSE_USER_EMAIL + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USER_EMAIL + "))";
        db.execSQL(CREATE_EXPENSES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drops tables and calls onCreate to recreate the schema (necessary for Hashing change)
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ". All existing user data will be lost due to security schema change.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // ---PASSWORD HASHING METHOD ---//

    /**
     * Converts a plain text password into a secure SHA-256 hash string.
     */
    private String hashPassword(String password) {
        String sha256 = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());
            byte[] bytes = md.digest();

            // Convert byte array to a readable hexadecimal string
            Formatter formatter = new Formatter();
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            sha256 = formatter.toString();
            formatter.close();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not available. Password hashing failed!", e);
        }
        return sha256;
    }


    // -------------------------------------------------------------------------
    // --- User Management Methods (MODIFIED FOR HASHING) ---
    // -------------------------------------------------------------------------

    /**
     * Attempts to insert a new user. Password is now HASHED before storage.
     */
    public int insertUser(String name, String email, String password) {
        // 1. Validation check for email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return -1;
        }

        String lowerCaseEmail = email.toLowerCase(Locale.ROOT);

        // 2. Check for duplicate email
        SQLiteDatabase readDb = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = readDb.query(TABLE_USERS, new String[]{COLUMN_USER_EMAIL}, COLUMN_USER_EMAIL + " = ?", new String[]{lowerCaseEmail}, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                return 0; // Duplicate email
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // Explicitly close after read operation
            if (readDb != null && readDb.isOpen()) {
                readDb.close();
            }
        }

        // 3. Insert user
        SQLiteDatabase writeDb = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USER_EMAIL, lowerCaseEmail);
        values.put(COLUMN_USER_NAME, name);

        // *** HASH THE PASSWORD BEFORE STORING ***
        String hashedPassword = hashPassword(password);
        values.put(COLUMN_USER_PASSWORD, hashedPassword);

        long result = writeDb.insert(TABLE_USERS, null, values);

        // Explicitly close after write operation
        if (writeDb != null && writeDb.isOpen()) {
            writeDb.close();
        }

        return (result != -1) ? 1 : -2;
    }


    /**
     * Fetches user details by email.
     */
    public UserDetails getUserDetails(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_EMAIL, COLUMN_USER_NAME};
        String selection = COLUMN_USER_EMAIL + " = ?";
        String[] selectionArgs = {email};
        UserDetails userDetails = null;
        Cursor cursor = null;

        try {
            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
            if (cursor.moveToFirst()) {
                int emailIndex = cursor.getColumnIndexOrThrow(COLUMN_USER_EMAIL);
                int userNameIndex = cursor.getColumnIndexOrThrow(COLUMN_USER_NAME);

                String userEmail = cursor.getString(emailIndex);
                String userName = cursor.getString(userNameIndex);
                userDetails = new UserDetails(userEmail, userName);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return userDetails;
    }


    /**
     * Checks if a user exists with the given email and password*/
    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_EMAIL};

        // *** HASH THE LOGIN PASSWORD ATTEMPT ***
        String hashedPasswordAttempt = hashPassword(password);
        String selection = COLUMN_USER_EMAIL + " = ?" + " AND " + COLUMN_USER_PASSWORD + " = ?";

        String lowerCaseEmail = email.toLowerCase(Locale.ROOT);
        String[] selectionArgs = {lowerCaseEmail, hashedPasswordAttempt};

        Cursor cursor = null;
        int cursorCount = 0;

        try {
            cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
            cursorCount = cursor.getCount();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return cursorCount > 0;
    }

    // -------------------------------------------------------------------------
    // --- Transaction/Expense Methods ---
    // -------------------------------------------------------------------------

    /**
     * Calculates the total balance (Total Income - Total Expense) for a user.
     */
    public double getTotalBalance(String userEmail) {
        double totalBalance = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        String query = "SELECT " + COLUMN_EXPENSE_TYPE + ", SUM(" + COLUMN_EXPENSE_AMOUNT + ") AS TotalAmount FROM " + TABLE_EXPENSES +
                " WHERE " + COLUMN_EXPENSE_USER_EMAIL + " = ? " +
                " GROUP BY " + COLUMN_EXPENSE_TYPE;

        try {
            cursor = db.rawQuery(query, new String[]{userEmail});

            if (cursor.moveToFirst()) {
                do {
                    String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_TYPE));
                    double total = cursor.getDouble(cursor.getColumnIndexOrThrow("TotalAmount"));

                    if (type.equals("Income")) {
                        totalBalance += total;
                    } else if (type.equals("Expense")) {
                        totalBalance -= total;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating total balance: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return totalBalance;
    }

    /**
     * Calculates the total amount for a given transaction type (Income/Expense)
     * for a specific user and month.
     */
    public double getMonthlyTotal(String userEmail, String monthYearFilter, String type) {
        SQLiteDatabase db = this.getReadableDatabase();
        double total = 0.0;
        Cursor cursor = null;

        // LIKE ? to match transactions for the entire month (e.g., '2025-10%')
        String query = "SELECT SUM(" + COLUMN_EXPENSE_AMOUNT + ") FROM " + TABLE_EXPENSES +
                " WHERE " + COLUMN_EXPENSE_USER_EMAIL + " = ?" +
                " AND " + COLUMN_EXPENSE_DATE + " LIKE ?" +
                " AND " + COLUMN_EXPENSE_TYPE + " = ?";

        try {
            cursor = db.rawQuery(query, new String[]{userEmail, monthYearFilter + "%", type});

            if (cursor.moveToFirst()) {
                total = cursor.getDouble(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating monthly total for type " + type + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return total;
    }

    /**
     * Calculates the total expense for a specific month and year.
     */
    public double getMonthlyExpense(String userEmail, int month, int year) {
        // Format to yyyy-MM
        String monthYearFilter = String.format(Locale.US, "%d-%02d", year, month);
        return getMonthlyTotal(userEmail, monthYearFilter, "Expense");
    }

    /**
     * Inserts a new expense/income transaction.
     */
    public boolean insertExpense(String userEmail, String category, double amount, String date, String note, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXPENSE_USER_EMAIL, userEmail);
        values.put(COLUMN_EXPENSE_CATEGORY, category);
        values.put(COLUMN_EXPENSE_AMOUNT, amount);
        values.put(COLUMN_EXPENSE_DATE, date);
        values.put(COLUMN_EXPENSE_NOTE, note);
        values.put(COLUMN_EXPENSE_TYPE, type);

        long result = db.insert(TABLE_EXPENSES, null, values);
        if (db != null && db.isOpen()) {
            db.close();
        }
        return result != -1;
    }

    /**
     * Updates an existing expense/income transaction.
     */
    public boolean updateExpense(long id, String userEmail, String category, double amount, String date, String note, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXPENSE_USER_EMAIL, userEmail);
        values.put(COLUMN_EXPENSE_CATEGORY, category);
        values.put(COLUMN_EXPENSE_AMOUNT, amount);
        values.put(COLUMN_EXPENSE_DATE, date);
        values.put(COLUMN_EXPENSE_NOTE, note);
        values.put(COLUMN_EXPENSE_TYPE, type);

        int rowsAffected = db.update(TABLE_EXPENSES, values, COLUMN_EXPENSE_ID + " = ?",
                new String[]{String.valueOf(id)});
        if (db != null && db.isOpen()) {
            db.close();
        }
        return rowsAffected > 0;
    }

    /**
     * Fetches a single transaction by its ID. (FIXED RESOURCE CLOSURE)
     */
    public Transaction getTransactionById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Transaction transaction = null;

        String selectQuery = "SELECT * FROM " + TABLE_EXPENSES + " WHERE " + COLUMN_EXPENSE_ID + " = ?";

        try {
            cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(id)});

            if (cursor.moveToFirst()) {
                long transactionId = id;
                String userEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_USER_EMAIL));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_TYPE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_CATEGORY));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_AMOUNT));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_DATE));
                String note = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_NOTE));

                transaction = new Transaction(transactionId, userEmail, type, category, amount, date, note);
            } else {
                Log.w(TAG, "Transaction not found for ID: " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching transaction by ID: " + e.getMessage());
        } finally {
            // *** CRITICAL FIX: Ensure the cursor is closed first ***
            if (cursor != null) {
                cursor.close();
            }
            // *** CRITICAL FIX: Ensure the database connection is closed safely ***
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return transaction;
    }

    /**
     * Deletes a transaction by its ID.
     */
    public boolean deleteTransaction(long transactionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_EXPENSES, COLUMN_EXPENSE_ID + " = ?",
                new String[]{String.valueOf(transactionId)});
        if (db != null && db.isOpen()) {
            db.close();
        }
        return result > 0;
    }

    /**
     * Fetches all transactions for a specific user, filtered by a YYYY-MM string.
     */
    public List<Transaction> getMonthlyTransactions(String userEmail, String monthYearFilter) {
        List<Transaction> transactionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        String selectQuery = "SELECT * FROM " + TABLE_EXPENSES
                + " WHERE " + COLUMN_EXPENSE_USER_EMAIL + " = ? AND "
                + COLUMN_EXPENSE_DATE + " LIKE ?"
                + " ORDER BY " + COLUMN_EXPENSE_DATE + " DESC, " + COLUMN_EXPENSE_ID + " DESC";

        try {
            cursor = db.rawQuery(selectQuery, new String[]{userEmail, monthYearFilter + "%"});

            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_ID));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_TYPE));
                    String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_CATEGORY));
                    double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_AMOUNT));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_DATE));
                    String note = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_NOTE));

                    Transaction transaction = new Transaction(id, userEmail, type, category, amount, date, note);
                    transactionList.add(transaction);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching monthly transactions: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return transactionList;
    }

    /**
     * Calculates the total income and total expense for a given month and year.
     */
    public Map<String, Double> getMonthlySummary(String userEmail, String monthYearFilter) {
        Map<String, Double> summary = new HashMap<>();
        summary.put("monthly_income", getMonthlyTotal(userEmail, monthYearFilter, "Income"));
        summary.put("monthly_expense", getMonthlyTotal(userEmail, monthYearFilter, "Expense"));
        return summary;
    }

    /**
     * Gets expense totals grouped by category for a specific month.
     */
    public Map<String, Double> getMonthlyCategoryExpenses(String userEmail, String monthYearFilter) {
        Map<String, Double> categoryExpenses = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        String query = "SELECT " + COLUMN_EXPENSE_CATEGORY + ", SUM(" + COLUMN_EXPENSE_AMOUNT + ") AS TotalAmount FROM " + TABLE_EXPENSES +
                " WHERE " + COLUMN_EXPENSE_USER_EMAIL + " = ? AND " +
                COLUMN_EXPENSE_DATE + " LIKE ? AND " +
                COLUMN_EXPENSE_TYPE + " = 'Expense' " +
                " GROUP BY " + COLUMN_EXPENSE_CATEGORY +
                " ORDER BY TotalAmount DESC";

        try {
            cursor = db.rawQuery(query, new String[]{userEmail, monthYearFilter + "%"});

            if (cursor.moveToFirst()) {
                do {
                    String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_CATEGORY));
                    double total = cursor.getDouble(cursor.getColumnIndexOrThrow("TotalAmount"));
                    categoryExpenses.put(category, total);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching category expenses: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return categoryExpenses;
    }

    /**
     * Fetches the N most recent transactions for a user.
     */
    public List<Transaction> getRecentTransactions(String userEmail, int limit) {
        List<Transaction> transactionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        String selectQuery = "SELECT * FROM " + TABLE_EXPENSES
                + " WHERE " + COLUMN_EXPENSE_USER_EMAIL + " = ? "
                + " ORDER BY " + COLUMN_EXPENSE_DATE + " DESC, " + COLUMN_EXPENSE_ID + " DESC"
                + " LIMIT " + limit;

        try {
            cursor = db.rawQuery(selectQuery, new String[]{userEmail});

            if (cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_ID));
                    String type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_TYPE));
                    String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_CATEGORY));
                    double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_AMOUNT));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_DATE));
                    String note = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXPENSE_NOTE));

                    Transaction transaction = new Transaction(id, userEmail, type, category, amount, date, note);
                    transactionList.add(transaction);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching recent transactions: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
        return transactionList;
    }
}