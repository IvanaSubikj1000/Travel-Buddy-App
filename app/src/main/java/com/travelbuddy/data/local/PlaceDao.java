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
public interface PlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Place place);

    @Update
    void update(Place place);

    @Delete
    void delete(Place place);

    @Query("SELECT * FROM places WHERE tripId = :tripId ORDER BY name ASC")
    LiveData<List<Place>> getPlacesForTrip(String tripId);

    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    Place getPlaceByIdSync(String id);
}
