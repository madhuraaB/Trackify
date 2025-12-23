package com.example.trackify;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackify.DatabaseHelper.Transaction;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private final Context context;
    private List<Transaction> transactionList;
    private final OnTransactionActionListener listener;
    // Field to control action button visibility
    private final boolean showActions;

    // Use Indian Locale for Rupee sign (₹) and currency formatting
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public interface OnTransactionActionListener {
        void onTransactionEdit(Transaction transaction);
        void onTransactionDelete(long transactionId);
    }

    /**
     * Constructor: Accepts a boolean to show/hide action buttons.
     */
    public TransactionAdapter(Context context, List<Transaction> transactionList, OnTransactionActionListener listener, boolean showActions) {
        this.context = context;
        this.transactionList = transactionList;
        this.listener = listener;
        this.showActions = showActions;
        // Ensure decimal places are limited for clean display
        this.currencyFormatter.setMaximumFractionDigits(2);
    }

    public void updateData(List<Transaction> newTransactionList) {
        this.transactionList = newTransactionList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Assuming R.layout.list_item_transaction exists
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);

        holder.categoryText.setText(transaction.category);
        holder.noteText.setText(transaction.note.isEmpty() ? "No Note" : transaction.note);
        holder.dateText.setText(transaction.date);

        // --- Amount Formatting and Coloring ---
        holder.amountText.setText(currencyFormatter.format(transaction.amount));

        int iconColor;
        // Use the first letter of the category for the icon text
        String iconLetter = transaction.category.substring(0, 1).toUpperCase(Locale.ROOT);

        if (transaction.type.equals("Income")) {
            // Use system green for Income
            iconColor = ContextCompat.getColor(context, android.R.color.holo_green_dark);
        } else { // Expense
            // Use system red for Expense
            iconColor = ContextCompat.getColor(context, android.R.color.holo_red_dark);
        }

        holder.amountText.setTextColor(iconColor);

        // Apply background color to the icon Text/Circle (must be a GradientDrawable)
        GradientDrawable background = (GradientDrawable) holder.iconText.getBackground();
        if (background != null) {
            background.setColor(iconColor);
        }
        holder.iconText.setText(iconLetter);

        // --- Action Button Visibility and Listeners ---
        if (holder.layoutActions != null) {
            if (showActions) {
                holder.layoutActions.setVisibility(View.VISIBLE);

                // Set separate listeners for edit and delete buttons ONLY if visible
                holder.editButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTransactionEdit(transaction);
                    }
                });

                holder.deleteButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTransactionDelete(transaction.id);
                    }
                });
            } else {
                holder.layoutActions.setVisibility(View.GONE);
                // Clear listeners (important for recycled views)
                holder.editButton.setOnClickListener(null);
                holder.deleteButton.setOnClickListener(null);
            }
        }
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        public TextView categoryText, noteText, amountText, dateText, iconText;
        public ImageButton editButton, deleteButton;
        public LinearLayout layoutActions; // Reference to the actions layout

        public TransactionViewHolder(View view) {
            super(view);
            // Initialize TextViews
            categoryText = view.findViewById(R.id.text_transaction_category);
            noteText = view.findViewById(R.id.text_transaction_note);
            amountText = view.findViewById(R.id.text_transaction_amount);
            dateText = view.findViewById(R.id.text_transaction_date);
            iconText = view.findViewById(R.id.text_transaction_icon);

            // Initialize Action Buttons and Container
            editButton = view.findViewById(R.id.button_edit);
            deleteButton = view.findViewById(R.id.button_delete);
            layoutActions = view.findViewById(R.id.layout_actions);
        }
    }
}