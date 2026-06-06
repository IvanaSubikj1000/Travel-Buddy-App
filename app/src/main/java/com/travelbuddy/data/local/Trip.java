package com.travelbuddy.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "trips")
public class Trip {

    @PrimaryKey
    @NonNull
    private String id;

    private String userId;
    private String title;
    private String destination;
    private String notes;
    private long startDateMillis;
    private long endDateMillis;
    private long updatedAt;

    public Trip() {
        this.id = UUID.randomUUID().toString();
        this.updatedAt = System.currentTimeMillis();
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getStartDateMillis() { return startDateMillis; }
    public void setStartDateMillis(long startDateMillis) { this.startDateMillis = startDateMillis; }

    public long getEndDateMillis() { return endDateMillis; }
    public void setEndDateMillis(long endDateMillis) { this.endDateMillis = endDateMillis; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
