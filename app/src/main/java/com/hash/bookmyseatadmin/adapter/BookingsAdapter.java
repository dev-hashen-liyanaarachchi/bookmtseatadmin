package com.hash.bookmyseatadmin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.hash.bookmyseatadmin.R;
import com.hash.bookmyseatadmin.model.AdminBooking;

import java.util.List;

public class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.BookingViewHolder> {

    private List<AdminBooking> bookings;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AdminBooking booking);
    }

    public BookingsAdapter(List<AdminBooking> bookings, OnItemClickListener listener) {
        this.bookings = bookings;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        AdminBooking booking = bookings.get(position);

        holder.tvBookingId.setText("ID: " + booking.getBookingId());
        holder.tvMovie.setText(booking.getMovieTitle());
        holder.tvSeats.setText("Seats: " + booking.getSeats());
        holder.tvAmount.setText("LKR " + booking.getTotalAmount());

        // Status indicators
        String status = "";
        int statusColor;

        if (booking.isAttended()) {
            status = "ATTENDED";
            statusColor = 0xFF4CAF50; // Green
        } else if (booking.isTicketIssued()) {
            status = "TICKET ISSUED";
            statusColor = 0xFFFF9800; // Orange
        } else {
            status = "PENDING";
            statusColor = 0xFFF44336; // Red
        }

        holder.tvStatus.setText(status);
        holder.tvStatus.setTextColor(statusColor);

        holder.cardView.setOnClickListener(v -> listener.onItemClick(booking));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvBookingId, tvMovie, tvSeats, tvAmount, tvStatus;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvMovie = itemView.findViewById(R.id.tvMovie);
            tvSeats = itemView.findViewById(R.id.tvSeats);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}