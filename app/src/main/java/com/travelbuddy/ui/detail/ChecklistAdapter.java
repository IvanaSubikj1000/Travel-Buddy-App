package com.travelbuddy.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.travelbuddy.R;
import com.travelbuddy.data.local.ChecklistItem;

import java.util.ArrayList;
import java.util.List;

public class ChecklistAdapter extends RecyclerView.Adapter<ChecklistAdapter.ViewHolder> {

    public interface OnCheckedToggleListener {
        void onCheckedToggle(ChecklistItem item, boolean checked);
    }

    public interface OnDeleteListener {
        void onDelete(ChecklistItem item);
    }

    private List<ChecklistItem> items = new ArrayList<>();
    private final OnCheckedToggleListener checkedToggleListener;
    private final OnDeleteListener deleteListener;

    public ChecklistAdapter(OnCheckedToggleListener checkedToggleListener,
                            OnDeleteListener deleteListener) {
        this.checkedToggleListener = checkedToggleListener;
        this.deleteListener = deleteListener;
    }

    public void setItems(List<ChecklistItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_checklist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChecklistItem item = items.get(position);

        holder.labelText.setText(item.getLabel());

        // Prevent listener from firing during rebind
        holder.checkedBox.setOnCheckedChangeListener(null);
        holder.checkedBox.setChecked(item.isChecked());
        holder.checkedBox.setOnCheckedChangeListener((btn, isChecked) ->
                checkedToggleListener.onCheckedToggle(item, isChecked));

        holder.deleteButton.setOnClickListener(v -> deleteListener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final CheckBox checkedBox;
        final TextView labelText;
        final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkedBox = itemView.findViewById(R.id.checkedBox);
            labelText = itemView.findViewById(R.id.labelText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
