package com.travelbuddy.data;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.travelbuddy.data.local.AppDatabase;
import com.travelbuddy.data.local.Place;
import com.travelbuddy.data.local.PlaceDao;
import com.travelbuddy.sync.SyncManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PlaceRepository {

    private final PlaceDao placeDao;
    private final SyncManager syncManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlaceRepository(Application application) {
        placeDao = AppDatabase.getInstance(application).placeDao();
        syncManager = SyncManager.getInstance(application);
    }

    public LiveData<List<Place>> getPlacesForTrip(String tripId) {
        return placeDao.getPlacesForTrip(tripId);
    }

    public void insert(Place place) {
        executor.execute(() -> {
            placeDao.insert(place);
            syncManager.syncPlace(place);
        });
    }

    public void update(Place place) {
        executor.execute(() -> {
            placeDao.update(place);
            syncManager.syncPlace(place);
        });
    }

    public void delete(Place place) {
        executor.execute(() -> {
            placeDao.delete(place);
            syncManager.deletePlace(place.getId(), place.getUserId());
        });
    }

    public void getPlaceById(String id, Consumer<Place> callback) {
        executor.execute(() -> {
            Place place = placeDao.getPlaceByIdSync(id);
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(place));
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
