package com.travelbuddy.ui.trips;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.travelbuddy.AddEditTripActivity;
import com.travelbuddy.R;
import com.travelbuddy.TripDetailActivity;
import com.travelbuddy.ui.detail.TripDetailFragment;

public class TripsFragment extends Fragment implements TripDetailFragment.Callbacks {

    private static final String KEY_DETAIL_ID = "detail_trip_id";

    private boolean isTwoPane;
    private String currentDetailTripId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trips, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isTwoPane = view.findViewById(R.id.detailContainer) != null;

        if (savedInstanceState != null) {
            currentDetailTripId = savedInstanceState.getString(KEY_DETAIL_ID);
        }

        TripViewModel viewModel = new ViewModelProvider(requireActivity())
                .get(TripViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        View emptyText = view.findViewById(R.id.emptyText);
        View fab = view.findViewById(R.id.fab);

        TripAdapter adapter = new TripAdapter(trip -> {
            if (isTwoPane) {
                showDetail(trip.getId());
            } else {
                Intent intent = new Intent(requireContext(), TripDetailActivity.class);
                intent.putExtra(TripDetailActivity.EXTRA_TRIP_ID, trip.getId());
                startActivity(intent);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getTrips().observe(getViewLifecycleOwner(), trips -> {
            adapter.setTrips(trips);
            boolean empty = trips == null || trips.isEmpty();
            emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

            if (isTwoPane && trips != null && !trips.isEmpty() && currentDetailTripId == null) {
                showDetail(trips.get(0).getId());
            }
        });

        fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddEditTripActivity.class)));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentDetailTripId != null) {
            outState.putString(KEY_DETAIL_ID, currentDetailTripId);
        }
    }

    private void showDetail(String tripId) {
        currentDetailTripId = tripId;
        getChildFragmentManager().beginTransaction()
                .replace(R.id.detailContainer, TripDetailFragment.newInstance(tripId))
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.SCREEN_NAME, "trips");
        params.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "TripsFragment");
        FirebaseAnalytics.getInstance(requireContext())
                .logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params);
    }

    @Override
    public void onTripLoaded(String title) {
        // no-op on tablet; title is visible in the list
    }

    @Override
    public void onEditTrip(String tripId) {
        Intent intent = new Intent(requireContext(), AddEditTripActivity.class);
        intent.putExtra(AddEditTripActivity.EXTRA_TRIP_ID, tripId);
        startActivity(intent);
    }

    @Override
    public void onTripDeleted() {
        currentDetailTripId = null;
        Fragment detail = getChildFragmentManager().findFragmentById(R.id.detailContainer);
        if (detail != null) {
            getChildFragmentManager().beginTransaction().remove(detail).commit();
        }
    }
}