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
public interface TripDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Trip trip);

    @Update
    void update(Trip trip);

    @Delete
    void delete(Trip trip);

    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY startDateMillis ASC")
    LiveData<List<Trip>> getTripsForUser(String userId);

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    LiveData<Trip> observeTripById(String id);

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    Trip getTripByIdSync(String id);
}
