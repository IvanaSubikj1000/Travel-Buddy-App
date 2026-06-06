package com.travelbuddy;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.travelbuddy.data.PlaceRepository;
import com.travelbuddy.data.local.Place;

public class AddEditPlaceActivity extends AppCompatActivity {

    public static final String EXTRA_TRIP_ID = "extra_trip_id";
    public static final String EXTRA_PLACE_ID = "extra_place_id";

    private PlaceRepository placeRepository;
    private TextInputLayout nameLayout;
    private TextInputEditText nameInput;
    private MaterialAutoCompleteTextView categoryDropdown;
    private TextInputEditText addressInput;
    private TextInputEditText notesInput;
    private MaterialCheckBox visitedCheckbox;

    private String tripId;
    private Place currentPlace;
    private boolean isEditMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_place);

        placeRepository = new PlaceRepository(getApplication());

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        nameLayout = findViewById(R.id.nameLayout);
        nameInput = findViewById(R.id.nameInput);
        categoryDropdown = findViewById(R.id.categoryDropdown);
        addressInput = findViewById(R.id.addressInput);
        notesInput = findViewById(R.id.notesInput);
        visitedCheckbox = findViewById(R.id.visitedCheckbox);

        String[] categories = getResources().getStringArray(R.array.place_categories);
        ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, categories);
        categoryDropdown.setAdapter(dropdownAdapter);
        if (categories.length > 0) {
            categoryDropdown.setText(categories[0], false);
        }

        tripId = getIntent().getStringExtra(EXTRA_TRIP_ID);
        String placeId = getIntent().getStringExtra(EXTRA_PLACE_ID);
        isEditMode = placeId != null;

        if (isEditMode) {
            setTitle(R.string.edit_place);
            placeRepository.getPlaceById(placeId, place -> {
                if (place != null) {
                    currentPlace = place;
                    populateFields(place);
                }
            });
        } else {
            setTitle(R.string.add_place);
        }

        findViewById(R.id.saveButton).setOnClickListener(v -> save());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isEditMode) {
            getMenuInflater().inflate(R.menu.add_edit_place_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_place) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateFields(Place place) {
        nameInput.setText(place.getName());
        categoryDropdown.setText(place.getCategory(), false);
        addressInput.setText(place.getAddress());
        notesInput.setText(place.getNotes());
        visitedCheckbox.setChecked(place.isVisited());
    }

    private void save() {
        nameLayout.setError(null);
        String name = text(nameInput);
        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.error_name_required));
            return;
        }

        Place place = currentPlace != null ? currentPlace : new Place();
        place.setName(name);
        place.setCategory(categoryDropdown.getText().toString());
        place.setAddress(text(addressInput));
        place.setNotes(text(notesInput));
        place.setVisited(visitedCheckbox.isChecked());
        place.setUpdatedAt(System.currentTimeMillis());

        if (currentPlace == null) {
            place.setTripId(tripId);
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            place.setUserId(user != null ? user.getUid() : "");
        }

        if (isEditMode) {
            placeRepository.update(place);
        } else {
            placeRepository.insert(place);
        }
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_place_title)
                .setMessage(R.string.confirm_delete_place_message)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    if (currentPlace != null) placeRepository.delete(currentPlace);
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
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
