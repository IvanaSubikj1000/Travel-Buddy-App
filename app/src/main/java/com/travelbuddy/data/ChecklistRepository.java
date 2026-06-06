package com.travelbuddy.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.travelbuddy.data.local.AppDatabase;
import com.travelbuddy.data.local.ChecklistDao;
import com.travelbuddy.data.local.ChecklistItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChecklistRepository {

    private final ChecklistDao checklistDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ChecklistRepository(Application application) {
        checklistDao = AppDatabase.getInstance(application).checklistDao();
    }

    public LiveData<List<ChecklistItem>> getItemsForTrip(String tripId) {
        return checklistDao.getItemsForTrip(tripId);
    }

    public void insert(ChecklistItem item) {
        executor.execute(() -> checklistDao.insert(item));
    }

    public void update(ChecklistItem item) {
        executor.execute(() -> checklistDao.update(item));
    }

    public void delete(ChecklistItem item) {
        executor.execute(() -> checklistDao.delete(item));
    }

    public void shutdown() {
        executor.shutdown();
    }
}
