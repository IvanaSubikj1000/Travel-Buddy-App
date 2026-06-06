package com.travelbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.travelbuddy.data.local.ChecklistItem;
import com.travelbuddy.data.local.Trip;
import com.travelbuddy.ui.detail.ChecklistAdapter;
import com.travelbuddy.ui.detail.PlaceAdapter;
import com.travelbuddy.ui.detail.TripDetailViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TripDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    private TripDetailViewModel viewModel;
    private Trip currentTrip;

    private TextView titleText;
    private TextView destinationText;
    private TextView dateRangeText;
    private TextView notesText;
    private TextView emptyPlacesText;
    private TextView emptyChecklistText;
    private TextInputEditText checklistInput;

    private PlaceAdapter placeAdapter;
    private ChecklistAdapter checklistAdapter;

    private final SimpleDateFormat dateFormat;

    {
        dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

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

        titleText = findViewById(R.id.titleText);
        destinationText = findViewById(R.id.destinationText);
        dateRangeText = findViewById(R.id.dateRangeText);
        notesText = findViewById(R.id.notesText);
        emptyPlacesText = findViewById(R.id.emptyPlacesText);
        emptyChecklistText = findViewById(R.id.emptyChecklistText);
        checklistInput = findViewById(R.id.checklistInput);

        viewModel = new ViewModelProvider(this,
                new TripDetailViewModel.Factory(getApplication(), tripId))
                .get(TripDetailViewModel.class);

        setupPlaces(tripId);
        setupChecklist();
        observeTrip();
    }

    private void setupPlaces(String tripId) {
        placeAdapter = new PlaceAdapter(
                place -> {
                    Intent intent = new Intent(this, AddEditPlaceActivity.class);
                    intent.putExtra(AddEditPlaceActivity.EXTRA_TRIP_ID, tripId);
                    intent.putExtra(AddEditPlaceActivity.EXTRA_PLACE_ID, place.getId());
                    startActivity(intent);
                },
                (place, visited) -> {
                    place.setVisited(visited);
                    viewModel.updatePlace(place);
                }
        );

        RecyclerView placesRecyclerView = findViewById(R.id.placesRecyclerView);
        placesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        placesRecyclerView.setAdapter(placeAdapter);
        placesRecyclerView.setNestedScrollingEnabled(false);

        viewModel.getPlaces().observe(this, places -> {
            placeAdapter.setPlaces(places);
            boolean empty = places == null || places.isEmpty();
            emptyPlacesText.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.addPlaceButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditPlaceActivity.class);
            intent.putExtra(AddEditPlaceActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        });
    }

    private void setupChecklist() {
        checklistAdapter = new ChecklistAdapter(
                (item, checked) -> {
                    item.setChecked(checked);
                    viewModel.updateChecklistItem(item);
                },
                item -> viewModel.deleteChecklistItem(item)
        );

        RecyclerView checklistRecyclerView = findViewById(R.id.checklistRecyclerView);
        checklistRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        checklistRecyclerView.setAdapter(checklistAdapter);
        checklistRecyclerView.setNestedScrollingEnabled(false);

        viewModel.getChecklistItems().observe(this, items -> {
            checklistAdapter.setItems(items);
            boolean empty = items == null || items.isEmpty();
            emptyChecklistText.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        View addButton = findViewById(R.id.addChecklistButton);
        addButton.setOnClickListener(v -> addChecklistItem());

        checklistInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                addChecklistItem();
                return true;
            }
            return false;
        });
    }

    private void addChecklistItem() {
        CharSequence t = checklistInput.getText();
        String label = t != null ? t.toString().trim() : "";
        if (!label.isEmpty()) {
            ChecklistItem item = new ChecklistItem();
            item.setLabel(label);
            viewModel.insertChecklistItem(item);
            checklistInput.setText("");
        }
    }

    private void observeTrip() {
        viewModel.observeTrip().observe(this, trip -> {
            if (trip == null) {
                finish();
                return;
            }
            currentTrip = trip;
            bindTrip(trip);
        });
    }

    private void bindTrip(Trip trip) {
        setTitle(trip.getTitle());
        titleText.setText(trip.getTitle());

        String dest = trip.getDestination();
        destinationText.setText(dest != null && !dest.isEmpty() ? dest : getString(R.string.not_set));

        String start = formatDate(trip.getStartDateMillis());
        String end = formatDate(trip.getEndDateMillis());
        if (!start.isEmpty() && !end.isEmpty()) {
            dateRangeText.setText(start + " – " + end);
        } else if (!start.isEmpty()) {
            dateRangeText.setText(start);
        } else {
            dateRangeText.setText(R.string.not_set);
        }

        String notes = trip.getNotes();
        notesText.setText(notes != null && !notes.isEmpty() ? notes : getString(R.string.not_set));
    }

    private String formatDate(long millis) {
        if (millis == 0) return "";
        return dateFormat.format(new Date(millis));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trip_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            if (currentTrip != null) {
                Intent intent = new Intent(this, AddEditTripActivity.class);
                intent.putExtra(AddEditTripActivity.EXTRA_TRIP_ID, currentTrip.getId());
                startActivity(intent);
            }
            return true;
        } else if (id == R.id.action_delete) {
            confirmDeleteTrip();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteTrip() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    if (currentTrip != null) viewModel.deleteTrip(currentTrip);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
