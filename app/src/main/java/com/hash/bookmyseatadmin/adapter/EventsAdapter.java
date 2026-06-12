package com.hash.bookmyseatadmin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.hash.bookmyseatadmin.R;
import com.hash.bookmyseatadmin.model.Event;

import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<Event> events;
    private OnItemClickListener listener;

    // ✅ Updated interface with Edit and Delete methods
    public interface OnItemClickListener {
        void onItemClick(Event event);      // View details
        void onEditClick(Event event);      // Edit event
        void onDeleteClick(Event event);    // Delete event
    }

    public EventsAdapter(List<Event> events, OnItemClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_admin, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);

        // Set text data
        holder.tvTitle.setText(event.getTitle());
        holder.tvMovie.setText(event.getMovieTitle());
        holder.tvDate.setText(event.getDate());
        holder.tvTime.setText(event.getTime());
        holder.tvVenue.setText(event.getVenue());
        holder.tvPrice.setText("LKR " + event.getPricePerSeat());

        // Set status with color
        String status = event.getStatus();
        int statusColor;
        if ("upcoming".equals(status)) {
            status = "UPCOMING";
            statusColor = 0xFF4CAF50;  // Green
        } else if ("coming_soon".equals(status)) {
            status = "COMING SOON";
            statusColor = 0xFFFF9800;  // Orange
        } else if ("ongoing".equals(status)) {
            status = "ONGOING";
            statusColor = 0xFF2196F3;  // Blue
        } else {
            status = "COMPLETED";
            statusColor = 0xFF888888;  // Gray
        }
        holder.tvStatus.setText(status);
        holder.tvStatus.setTextColor(statusColor);

        // Card click - view details
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(event);
            }
        });

        // ✅ Edit button click
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(event);
            }
        });

        // ✅ Delete button click
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events != null ? events.size() : 0;
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle, tvMovie, tvDate, tvTime, tvVenue, tvPrice, tvStatus;
        Button btnEdit, btnDelete;  // ✅ Added buttons

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMovie = itemView.findViewById(R.id.tvMovie);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);     // ✅ Find edit button
            btnDelete = itemView.findViewById(R.id.btnDelete); // ✅ Find delete button
        }
    }
}