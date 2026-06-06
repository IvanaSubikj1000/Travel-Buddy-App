package com.travelbuddy.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.travelbuddy.R;
import com.travelbuddy.data.local.Place;

import java.util.ArrayList;
import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.ViewHolder> {

    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
    }

    public interface OnVisitedToggleListener {
        void onVisitedToggle(Place place, boolean visited);
    }

    private List<Place> places = new ArrayList<>();
    private final OnPlaceClickListener clickListener;
    private final OnVisitedToggleListener visitedToggleListener;

    public PlaceAdapter(OnPlaceClickListener clickListener,
                        OnVisitedToggleListener visitedToggleListener) {
        this.clickListener = clickListener;
        this.visitedToggleListener = visitedToggleListener;
    }

    public void setPlaces(List<Place> places) {
        this.places = places != null ? places : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Place place = places.get(position);

        holder.nameText.setText(place.getName());
        holder.categoryText.setText(place.getCategory());

        String address = place.getAddress();
        if (address != null && !address.isEmpty()) {
            holder.addressText.setText(address);
            holder.addressText.setVisibility(View.VISIBLE);
        } else {
            holder.addressText.setVisibility(View.GONE);
        }

        // Prevent listener from firing during rebind
        holder.visitedCheckbox.setOnCheckedChangeListener(null);
        holder.visitedCheckbox.setChecked(place.isVisited());
        holder.visitedCheckbox.setOnCheckedChangeListener((btn, isChecked) ->
                visitedToggleListener.onVisitedToggle(place, isChecked));

        holder.itemView.setOnClickListener(v -> clickListener.onPlaceClick(place));
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView categoryText;
        final TextView addressText;
        final CheckBox visitedCheckbox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.placeNameText);
            categoryText = itemView.findViewById(R.id.placeCategoryText);
            addressText = itemView.findViewById(R.id.placeAddressText);
            visitedCheckbox = itemView.findViewById(R.id.visitedCheckbox);
        }
    }
}
