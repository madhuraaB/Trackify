package com.example.trackify;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout that contains the FrameLayout (fragment_container)
        setContentView(R.layout.activity_home);

        // This check prevents the Fragment from being added multiple times
        // after a configuration change (like screen rotation).
        if (savedInstanceState == null) {
            // 1. Get the FragmentManager
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            // 2. Instantiate your new Fragment
            ViewTransactionsFragment fragment = new ViewTransactionsFragment();

            // 3. Add the Fragment to the FrameLayout container
            // R.id.fragment_container comes from your activity_home.xml
            transaction.add(R.id.fragment_container, fragment);

            // 4. Commit the transaction to display the Fragment
            transaction.commit();
        }
    }
}