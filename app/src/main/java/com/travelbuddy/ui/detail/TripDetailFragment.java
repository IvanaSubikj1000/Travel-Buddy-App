package com.travelbuddy.ui.detail;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.android.material.textfield.TextInputEditText;
import com.travelbuddy.AddEditPlaceActivity;
import com.travelbuddy.R;
import com.travelbuddy.data.local.ChecklistItem;
import com.travelbuddy.data.local.Trip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TripDetailFragment extends Fragment {

    public static final String ARG_TRIP_ID = "tripId";

    public interface Callbacks {
        void onTripLoaded(String title);
        void onEditTrip(String tripId);
        void onTripDeleted();
    }

    private TripDetailViewModel viewModel;
    private Trip currentTrip;
    private String tripId;

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

    public static TripDetailFragment newInstance(String tripId) {
        TripDetailFragment f = new TripDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TRIP_ID, tripId);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trip_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tripId = requireArguments().getString(ARG_TRIP_ID);
        viewModel = new ViewModelProvider(this,
                new TripDetailViewModel.Factory(requireActivity().getApplication(), tripId))
                .get(TripDetailViewModel.class);

        titleText = view.findViewById(R.id.titleText);
        destinationText = view.findViewById(R.id.destinationText);
        dateRangeText = view.findViewById(R.id.dateRangeText);
        notesText = view.findViewById(R.id.notesText);
        emptyPlacesText = view.findViewById(R.id.emptyPlacesText);
        emptyChecklistText = view.findViewById(R.id.emptyChecklistText);
        checklistInput = view.findViewById(R.id.checklistInput);

        view.findViewById(R.id.editButton).setOnClickListener(v -> {
            Callbacks cb = findCallbacks();
            if (cb != null && currentTrip != null) cb.onEditTrip(currentTrip.getId());
        });

        view.findViewById(R.id.deleteButton).setOnClickListener(v -> confirmDeleteTrip());

        setupPlaces(view);
        setupChecklist(view);
        observeTrip();
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.SCREEN_NAME, "trip_detail");
        params.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "TripDetailFragment");
        FirebaseAnalytics.getInstance(requireContext())
                .logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
    }

    private void setupPlaces(View view) {
        placeAdapter = new PlaceAdapter(
                place -> {
                    Intent intent = new Intent(requireContext(), AddEditPlaceActivity.class);
                    intent.putExtra(AddEditPlaceActivity.EXTRA_TRIP_ID, tripId);
                    intent.putExtra(AddEditPlaceActivity.EXTRA_PLACE_ID, place.getId());
                    startActivity(intent);
                },
                (place, visited) -> {
                    place.setVisited(visited);
                    viewModel.updatePlace(place);
                }
        );

        RecyclerView placesRecyclerView = view.findViewById(R.id.placesRecyclerView);
        placesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        placesRecyclerView.setAdapter(placeAdapter);
        placesRecyclerView.setNestedScrollingEnabled(false);

        viewModel.getPlaces().observe(getViewLifecycleOwner(), places -> {
            placeAdapter.setPlaces(places);
            boolean empty = places == null || places.isEmpty();
            emptyPlacesText.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        view.findViewById(R.id.addPlaceButton).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditPlaceActivity.class);
            intent.putExtra(AddEditPlaceActivity.EXTRA_TRIP_ID, tripId);
            startActivity(intent);
        });
    }

    private void setupChecklist(View view) {
        checklistAdapter = new ChecklistAdapter(
                (item, checked) -> {
                    item.setChecked(checked);
                    viewModel.updateChecklistItem(item);
                },
                item -> viewModel.deleteChecklistItem(item)
        );

        RecyclerView checklistRecyclerView = view.findViewById(R.id.checklistRecyclerView);
        checklistRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        checklistRecyclerView.setAdapter(checklistAdapter);
        checklistRecyclerView.setNestedScrollingEnabled(false);

        viewModel.getChecklistItems().observe(getViewLifecycleOwner(), items -> {
            checklistAdapter.setItems(items);
            boolean empty = items == null || items.isEmpty();
            emptyChecklistText.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        view.findViewById(R.id.addChecklistButton).setOnClickListener(v -> addChecklistItem());

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
        viewModel.observeTrip().observe(getViewLifecycleOwner(), trip -> {
            if (trip == null) {
                Callbacks cb = findCallbacks();
                if (cb != null) cb.onTripDeleted();
                return;
            }
            currentTrip = trip;
            bindTrip(trip);
        });
    }

    private void bindTrip(Trip trip) {
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

        Callbacks cb = findCallbacks();
        if (cb != null) cb.onTripLoaded(trip.getTitle());
    }

    private String formatDate(long millis) {
        if (millis == 0) return "";
        return dateFormat.format(new Date(millis));
    }

    private void confirmDeleteTrip() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    if (currentTrip != null) {
                        viewModel.deleteTrip(currentTrip);
                        Callbacks cb = findCallbacks();
                        if (cb != null) cb.onTripDeleted();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private Callbacks findCallbacks() {
        if (getParentFragment() instanceof Callbacks) return (Callbacks) getParentFragment();
        if (getActivity() instanceof Callbacks) return (Callbacks) getActivity();
        return null;
    }
}