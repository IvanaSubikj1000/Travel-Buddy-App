package com.travelbuddy.ui.trips;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.travelbuddy.data.TripRepository;
import com.travelbuddy.data.local.Trip;

import java.util.List;
import java.util.function.Consumer;

public class TripViewModel extends AndroidViewModel {

    private final TripRepository repository;
    private final String userId;

    public TripViewModel(@NonNull Application application) {
        super(application);
        repository = new TripRepository(application);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";
    }

    public LiveData<List<Trip>> getTrips() {
        return repository.getTripsForUser(userId);
    }

    public LiveData<Trip> observeTripById(String id) {
        return repository.observeTripById(id);
    }

    public void insert(Trip trip) {
        trip.setUserId(userId);
        trip.setUpdatedAt(System.currentTimeMillis());
        repository.insert(trip);
    }

    public void update(Trip trip) {
        trip.setUpdatedAt(System.currentTimeMillis());
        repository.update(trip);
    }

    public void delete(Trip trip) {
        repository.delete(trip);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.shutdown();
    }
}
