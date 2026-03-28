package com.hash.bookmyseatadmin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public interface OnItemClickListener {
        void onItemClick(Event event);
    }

    // Constructor with both parameters
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

        holder.tvTitle.setText(event.getTitle());
        holder.tvMovie.setText(event.getMovieTitle());
        holder.tvDate.setText(event.getDate());
        holder.tvTime.setText(event.getTime());
        holder.tvVenue.setText(event.getVenue());
        holder.tvPrice.setText("LKR " + event.getPricePerSeat());

        // Status
        String status = event.getStatus();
        int statusColor;
        if ("upcoming".equals(status)) {
            status = "UPCOMING";
            statusColor = 0xFF4CAF50; // Green
        } else if ("ongoing".equals(status)) {
            status = "ONGOING";
            statusColor = 0xFFFF9800; // Orange
        } else {
            status = "COMPLETED";
            statusColor = 0xFF888888; // Gray
        }
        holder.tvStatus.setText(status);
        holder.tvStatus.setTextColor(statusColor);

        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(event);
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
        }
    }
}