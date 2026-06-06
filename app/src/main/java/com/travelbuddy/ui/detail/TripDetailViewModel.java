package com.travelbuddy.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.travelbuddy.data.ChecklistRepository;
import com.travelbuddy.data.PlaceRepository;
import com.travelbuddy.data.TripRepository;
import com.travelbuddy.data.local.ChecklistItem;
import com.travelbuddy.data.local.Place;
import com.travelbuddy.data.local.Trip;

import java.util.List;

public class TripDetailViewModel extends AndroidViewModel {

    private final TripRepository tripRepository;
    private final PlaceRepository placeRepository;
    private final ChecklistRepository checklistRepository;
    private final FirebaseAnalytics analytics;
    private final String tripId;
    private final String userId;

    public TripDetailViewModel(@NonNull Application application, String tripId) {
        super(application);
        this.tripId = tripId;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userId = user != null ? user.getUid() : "";
        tripRepository = new TripRepository(application);
        placeRepository = new PlaceRepository(application);
        checklistRepository = new ChecklistRepository(application);
        analytics = FirebaseAnalytics.getInstance(application);
    }

    // --- Trip ---

    public LiveData<Trip> observeTrip() {
        return tripRepository.observeTripById(tripId);
    }

    public void deleteTrip(Trip trip) {
        tripRepository.delete(trip);
        analytics.logEvent("trip_deleted", null);
    }

    // --- Places ---

    public LiveData<List<Place>> getPlaces() {
        return placeRepository.getPlacesForTrip(tripId);
    }

    public void insertPlace(Place place) {
        place.setTripId(tripId);
        place.setUserId(userId);
        place.setUpdatedAt(System.currentTimeMillis());
        placeRepository.insert(place);
        analytics.logEvent("place_added", null);
    }

    public void updatePlace(Place place) {
        place.setUpdatedAt(System.currentTimeMillis());
        placeRepository.update(place);
    }

    public void deletePlace(Place place) {
        placeRepository.delete(place);
    }

    // --- Checklist ---

    public LiveData<List<ChecklistItem>> getChecklistItems() {
        return checklistRepository.getItemsForTrip(tripId);
    }

    public void insertChecklistItem(ChecklistItem item) {
        item.setTripId(tripId);
        item.setUserId(userId);
        item.setUpdatedAt(System.currentTimeMillis());
        checklistRepository.insert(item);
        analytics.logEvent("checklist_item_added", null);
    }

    public void updateChecklistItem(ChecklistItem item) {
        item.setUpdatedAt(System.currentTimeMillis());
        checklistRepository.update(item);
    }

    public void deleteChecklistItem(ChecklistItem item) {
        checklistRepository.delete(item);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        tripRepository.shutdown();
        placeRepository.shutdown();
        checklistRepository.shutdown();
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final Application application;
        private final String tripId;

        public Factory(Application application, String tripId) {
            this.application = application;
            this.tripId = tripId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new TripDetailViewModel(application, tripId);
        }
    }
}