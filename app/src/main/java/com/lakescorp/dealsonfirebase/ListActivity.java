package com.lakescorp.dealsonfirebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

public class ListActivity extends AppCompatActivity {

    ArrayList<TravelDeal> deals;

    FirebaseDatabase mFirebaseDatabase;
    DatabaseReference mDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.list_activity_menu, menu);

        MenuItem insertMenu = menu.findItem(R.id.insert_menu);

        //Toast.makeText(this.getBaseContext(), "Menu is redrawn " + FirebaseUtils.isAdmin , Toast.LENGTH_LONG).show();

        if(FirebaseUtils.isAdmin){
            insertMenu.setVisible(true);
        }
        else {
            insertMenu.setVisible(false);
        }


        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.insert_menu:
                Intent intent = new Intent(this, DealActivity.class);
                startActivity(intent);
                return true;

            case R.id.logout_menu:

                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                FirebaseUtils.attachListener();
                            }
                        });
                FirebaseUtils.detachListener();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        FirebaseUtils.detachListener();
    }

    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUtils.openFirebaseReference("deals", this);
        RecyclerView rvDeals = findViewById(R.id.rvDeals);
        DealAdapter adapter = new DealAdapter();
        rvDeals.setAdapter(adapter);

        //Define the layout manager: linearLayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                RecyclerView.VERTICAL,
                false);

        rvDeals.setLayoutManager(layoutManager);

        FirebaseUtils.attachListener();
    }

    public void showMenu() {
        invalidateOptionsMenu();
    }
}
