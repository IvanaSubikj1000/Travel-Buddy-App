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

import com.travelbuddy.AddEditTripActivity;
import com.travelbuddy.R;
import com.travelbuddy.TripDetailActivity;

public class TripsFragment extends Fragment {

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

        TripViewModel viewModel = new ViewModelProvider(requireActivity())
                .get(TripViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        View emptyText = view.findViewById(R.id.emptyText);
        View fab = view.findViewById(R.id.fab);

        TripAdapter adapter = new TripAdapter(trip -> {
            Intent intent = new Intent(requireContext(), TripDetailActivity.class);
            intent.putExtra(TripDetailActivity.EXTRA_TRIP_ID, trip.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getTrips().observe(getViewLifecycleOwner(), trips -> {
            adapter.setTrips(trips);
            boolean empty = trips == null || trips.isEmpty();
            emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddEditTripActivity.class)));
    }
}
