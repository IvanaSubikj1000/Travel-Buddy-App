package com.travelbuddy;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.travelbuddy.ui.detail.TripDetailFragment;

public class TripDetailActivity extends AppCompatActivity
        implements TripDetailFragment.Callbacks {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_detail);

        String tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (tripId == null) {
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detailFragmentContainer, TripDetailFragment.newInstance(tripId))
                    .commit();
        }
    }

    @Override
    public void onTripLoaded(String title) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void onEditTrip(String tripId) {
        Intent intent = new Intent(this, AddEditTripActivity.class);
        intent.putExtra(AddEditTripActivity.EXTRA_TRIP_ID, tripId);
        startActivity(intent);
    }

    @Override
    public void onTripDeleted() {
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}