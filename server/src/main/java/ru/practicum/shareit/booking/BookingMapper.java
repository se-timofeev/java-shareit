package ru.practicum.shareit.booking;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.booking.model.BookingDto;
import ru.practicum.shareit.booking.model.BookingDtoEntry;
import ru.practicum.shareit.booking.model.BookingShort;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

@Component
public class BookingMapper {

    public static Booking toBooking(User booker, Item item, BookingDtoEntry bookingDtoEntry) {
        Booking booking = new Booking();
        booking.setStart(bookingDtoEntry.getStart());
        booking.setEnd(bookingDtoEntry.getEnd());
        booking.setBooker(booker);
        booking.setItem(item);
        return booking;
    }

    public static BookingDto toBookingDto(Booking booking) {
        return new BookingDto(booking.getId(), booking.getStart(), booking.getEnd(),
                new BookingDto.Item(booking.getItem().getId(), booking.getItem().getName()),
                booking.getBooker(), booking.getStatus());
    }

    public static BookingShort toBookingShort(Booking booking) {
        BookingShort bookingShort = new BookingShort();
        bookingShort.setId(booking.getId());
        bookingShort.setBookerId(booking.getBooker().getId());
        return bookingShort;
    }
}
