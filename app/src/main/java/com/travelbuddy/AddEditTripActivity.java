package com.travelbuddy;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.travelbuddy.data.local.Trip;
import com.travelbuddy.ui.trips.TripViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AddEditTripActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";

    private TripViewModel viewModel;
    private TextInputLayout titleLayout;
    private TextInputEditText titleInput;
    private TextInputEditText destinationInput;
    private TextInputEditText notesInput;
    private MaterialButton startDateButton;
    private MaterialButton endDateButton;

    private long startDateMillis = 0;
    private long endDateMillis = 0;
    private Trip currentTrip = null;
    private boolean fieldsPopulated = false;

    private final SimpleDateFormat dateFormat;

    {
        dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_trip);

        viewModel = new ViewModelProvider(this).get(TripViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        titleLayout = findViewById(R.id.titleLayout);
        titleInput = findViewById(R.id.titleInput);
        destinationInput = findViewById(R.id.destinationInput);
        notesInput = findViewById(R.id.notesInput);
        startDateButton = findViewById(R.id.startDateButton);
        endDateButton = findViewById(R.id.endDateButton);

        String tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        if (tripId != null) {
            setTitle(R.string.edit_trip);
            viewModel.observeTripById(tripId).observe(this, trip -> {
                if (trip != null && !fieldsPopulated) {
                    currentTrip = trip;
                    populateFields(trip);
                    fieldsPopulated = true;
                }
            });
        } else {
            setTitle(R.string.add_trip);
        }

        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));
        findViewById(R.id.saveButton).setOnClickListener(v -> save());
    }

    private void populateFields(Trip trip) {
        titleInput.setText(trip.getTitle());
        destinationInput.setText(trip.getDestination());
        notesInput.setText(trip.getNotes());
        startDateMillis = trip.getStartDateMillis();
        endDateMillis = trip.getEndDateMillis();
        updateDateButtons();
    }

    private void showDatePicker(boolean isStart) {
        long current = isStart ? startDateMillis : endDateMillis;
        long selection = current != 0 ? current : MaterialDatePicker.todayInUtcMilliseconds();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStart ? R.string.select_start_date : R.string.select_end_date)
                .setSelection(selection)
                .build();

        picker.addOnPositiveButtonClickListener(sel -> {
            if (isStart) {
                startDateMillis = sel;
                startDateButton.setText(dateFormat.format(new Date(sel)));
            } else {
                endDateMillis = sel;
                endDateButton.setText(dateFormat.format(new Date(sel)));
            }
        });

        picker.show(getSupportFragmentManager(), isStart ? "START_DATE" : "END_DATE");
    }

    private void updateDateButtons() {
        startDateButton.setText(startDateMillis != 0
                ? dateFormat.format(new Date(startDateMillis))
                : getString(R.string.trip_start_date));
        endDateButton.setText(endDateMillis != 0
                ? dateFormat.format(new Date(endDateMillis))
                : getString(R.string.trip_end_date));
    }

    private void save() {
        titleLayout.setError(null);
        String title = text(titleInput);
        if (TextUtils.isEmpty(title)) {
            titleLayout.setError(getString(R.string.error_title_required));
            return;
        }

        Trip trip = currentTrip != null ? currentTrip : new Trip();
        trip.setTitle(title);
        trip.setDestination(text(destinationInput));
        trip.setNotes(text(notesInput));
        trip.setStartDateMillis(startDateMillis);
        trip.setEndDateMillis(endDateMillis);

        if (currentTrip != null) {
            viewModel.update(trip);
        } else {
            viewModel.insert(trip);
        }

        finish();
    }

    private String text(TextInputEditText input) {
        CharSequence t = input.getText();
        return t != null ? t.toString().trim() : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
