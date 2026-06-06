package com.travelbuddy.ui.trips;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.travelbuddy.R;
import com.travelbuddy.data.local.Trip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.ViewHolder> {

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    private List<Trip> trips = new ArrayList<>();
    private final OnTripClickListener listener;

    public TripAdapter(OnTripClickListener listener) {
        this.listener = listener;
    }

    public void setTrips(List<Trip> trips) {
        this.trips = trips != null ? trips : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Trip trip = trips.get(position);
        holder.title.setText(trip.getTitle());

        String dest = trip.getDestination();
        if (dest != null && !dest.isEmpty()) {
            holder.destination.setText(dest);
            holder.destination.setVisibility(View.VISIBLE);
        } else {
            holder.destination.setVisibility(View.GONE);
        }

        String start = formatDate(trip.getStartDateMillis());
        String end = formatDate(trip.getEndDateMillis());
        if (!start.isEmpty() && !end.isEmpty()) {
            holder.dateRange.setText(start + " – " + end);
            holder.dateRange.setVisibility(View.VISIBLE);
        } else if (!start.isEmpty()) {
            holder.dateRange.setText(start);
            holder.dateRange.setVisibility(View.VISIBLE);
        } else {
            holder.dateRange.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onTripClick(trip));
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    private static String formatDate(long millis) {
        if (millis == 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView destination;
        final TextView dateRange;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tripTitle);
            destination = itemView.findViewById(R.id.tripDestination);
            dateRange = itemView.findViewById(R.id.tripDateRange);
        }
    }
}
