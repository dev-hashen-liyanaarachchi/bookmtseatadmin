package com.hash.bookmyseatadmin.model;

public class AdminBooking {
    private String bookingId;
    private String movieTitle;
    private String seats;
    private double totalAmount;
    private boolean ticketIssued;
    private boolean attended;
    private String bookingDate;
    private String userId;
    private String userEmail;

    public AdminBooking(String bookingId, String movieTitle, String seats,
                        double totalAmount, boolean ticketIssued, boolean attended,
                        String bookingDate, String userId, String userEmail) {
        this.bookingId = bookingId;
        this.movieTitle = movieTitle;
        this.seats = seats;
        this.totalAmount = totalAmount;
        this.ticketIssued = ticketIssued;
        this.attended = attended;
        this.bookingDate = bookingDate;
        this.userId = userId;
        this.userEmail = userEmail;
    }

    // Getters
    public String getBookingId() {
        return bookingId; }
    public String getMovieTitle() {
        return movieTitle; }
    public String getSeats() { return seats; }
    public double getTotalAmount() { return totalAmount; }
    public boolean isTicketIssued() { return ticketIssued; }
    public boolean isAttended() { return attended; }
    public String getBookingDate() { return bookingDate; }
    public String getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
}