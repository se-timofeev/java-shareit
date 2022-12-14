package ru.practicum.shareit.booking.strategy.strategyimpl;

import org.springframework.data.domain.Pageable;
import ru.practicum.shareit.booking.model.BookingDto;
import ru.practicum.shareit.booking.strategy.BookingState;

import java.util.List;

public interface Strategy {

    BookingState getState();

    List<BookingDto> findBookingByStrategy(Long bookerId, Pageable pageable);
}
