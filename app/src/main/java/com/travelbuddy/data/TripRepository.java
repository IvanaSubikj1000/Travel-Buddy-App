package com.travelbuddy.data;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.travelbuddy.data.local.AppDatabase;
import com.travelbuddy.data.local.Trip;
import com.travelbuddy.data.local.TripDao;
import com.travelbuddy.sync.SyncManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TripRepository {

    private final TripDao tripDao;
    private final SyncManager syncManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TripRepository(Application application) {
        tripDao = AppDatabase.getInstance(application).tripDao();
        syncManager = SyncManager.getInstance(application);
    }

    public LiveData<List<Trip>> getTripsForUser(String userId) {
        return tripDao.getTripsForUser(userId);
    }

    public LiveData<Trip> observeTripById(String id) {
        return tripDao.observeTripById(id);
    }

    public void insert(Trip trip) {
        executor.execute(() -> {
            tripDao.insert(trip);
            syncManager.syncTrip(trip);
        });
    }

    public void update(Trip trip) {
        executor.execute(() -> {
            tripDao.update(trip);
            syncManager.syncTrip(trip);
        });
    }

    public void delete(Trip trip) {
        executor.execute(() -> {
            tripDao.delete(trip);
            syncManager.deleteTrip(trip.getId(), trip.getUserId());
        });
    }

    public void getTripById(String id, Consumer<Trip> callback) {
        executor.execute(() -> {
            Trip trip = tripDao.getTripByIdSync(id);
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(trip));
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
