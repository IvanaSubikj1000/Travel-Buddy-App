package com.travelbuddy.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ChecklistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChecklistItem item);

    @Update
    void update(ChecklistItem item);

    @Delete
    void delete(ChecklistItem item);

    @Query("SELECT * FROM checklist_items WHERE tripId = :tripId ORDER BY rowid ASC")
    LiveData<List<ChecklistItem>> getItemsForTrip(String tripId);

    @Query("SELECT * FROM checklist_items WHERE id = :id LIMIT 1")
    ChecklistItem getChecklistItemByIdSync(String id);
}
