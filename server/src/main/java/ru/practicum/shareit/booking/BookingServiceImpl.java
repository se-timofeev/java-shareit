package ru.practicum.shareit.booking;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.BookingDtoEntry;
import ru.practicum.shareit.booking.strategy.BookingState;
import ru.practicum.shareit.booking.model.BookingDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.booking.strategy.StrategyFactory;
import ru.practicum.shareit.booking.strategy.strategyimpl.Strategy;
import ru.practicum.shareit.exceptions.*;
import ru.practicum.shareit.exceptions.BookingNotFoundException;
import ru.practicum.shareit.exceptions.ItemNotFoundException;
import ru.practicum.shareit.exceptions.UserNotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.shareit.booking.BookingMapper.toBookingDto;

@Service
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    private final StrategyFactory strategyFactory;

    @Override
    public BookingDto addBooking(long userId, BookingDtoEntry bookingDtoEntry) {
        User booker = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Item item = itemRepository.findById(bookingDtoEntry.getItemId())
                .orElseThrow(() -> new ItemNotFoundException(bookingDtoEntry.getItemId()));
        if (item.getOwner().getId() == userId) {
            throw new BookingCreationException("cant rent by himself");
        }
        if (item.getAvailable().equals(false)) {
            throw new ItemAvailableException(item.getId());
        }
        Booking booking = BookingMapper.toBooking(booker, item, bookingDtoEntry);
        booking = bookingRepository.save(booking);
        log.info("add booking " + booking);
        return toBookingDto(booking);
    }

    @Override
    public BookingDto approveStatus(long userId, long bookingId, boolean approved) {
        Booking booking = bookingRepository.findByIdAndItem_Owner_Id(bookingId, userId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new BookingStatusException("can't change status");
        }
        BookingStatus bookingStatus = approved ? BookingStatus.APPROVED : BookingStatus.REJECTED;
        booking.setStatus(bookingStatus);
        bookingRepository.save(booking);
        log.warn("booking updates " + booking);
        return toBookingDto(booking);
    }

    @Override
    public BookingDto getBookingByUserIdAndBookingId(long userId, long bookingId) {
        return bookingRepository.findBooking(userId, bookingId).map(BookingMapper::toBookingDto)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    @Override
    public List<BookingDto> getUserBookingByState(long bookerId, String state, int from, int size) {
        userRepository.findById(bookerId).orElseThrow(() -> new UserNotFoundException(bookerId));
        return filterByStateForBookerId(bookerId, state.toUpperCase(), paginationParameters(from, size));
    }

    public static Pageable paginationParameters(int from, int size) {
        if (from < 0 || size < 1) {
            throw new PaginationParametersException("error");
        }
        int page = from / size;
        Sort sort = Sort.by("id").ascending();
        return PageRequest.of(page, size, sort);
    }

    @Override
    public List<BookingDto> getBookingForUsersItem(long ownerId, String state, int from, int size) {
        userRepository.findById(ownerId).orElseThrow(() -> new UserNotFoundException(ownerId));
        return filterByStateForItemOwnerId(ownerId, state.toUpperCase(), paginationParameters(from, size));
    }

    private List<BookingDto> filterByStateForBookerId(long bookerId, String state, Pageable pageable) {
        BookingState bookingState = getBookingState(state);
        Strategy strategy = strategyFactory.findStrategy(bookingState);
        return strategy.findBookingByStrategy(bookerId, pageable);
    }

    private List<BookingDto> filterByStateForItemOwnerId(Long ownerId, String state, Pageable page) {
        LocalDateTime date = LocalDateTime.now();
        BookingState bookingState = getBookingState(state);
        List<Booking> list;
        switch (bookingState) {
            case CURRENT:
                list = bookingRepository.findCurrentBookingsByItemOwnerIdOrderByStartDesc(ownerId, date, page);
                break;
            case PAST:
                list = bookingRepository.findBookingsByItem_Owner_IdAndEndBeforeOrderByStartDesc(ownerId, date, page);
                break;
            case FUTURE:
                list = bookingRepository.findBookingsByItem_Owner_IdAndStartAfterOrderByStartDesc(ownerId, date, page);
                break;
            case WAITING:
            case REJECTED:
                list = bookingRepository.findBookingsByItem_Owner_IdAndStatusOrderByStartDesc(ownerId,
                        BookingStatus.valueOf(state), page);
                break;
            default:
                list = bookingRepository.findBookingsByItem_Owner_IdOrderByStartDesc(ownerId, page);
        }
        return list.stream().map(BookingMapper::toBookingDto).collect(Collectors.toList());
    }

    private BookingState getBookingState(String state) {
        try {
            return BookingState.valueOf(state);
        } catch (Throwable e) {
            throw new UnknownBookingStateException(state);
        }
    }

}
