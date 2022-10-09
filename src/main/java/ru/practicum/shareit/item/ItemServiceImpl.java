package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.dto.BookingShort;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exceptions.*;
import ru.practicum.shareit.comments.Comment;
import ru.practicum.shareit.comments.CommentDto;
import ru.practicum.shareit.comments.CommentRepository;
import ru.practicum.shareit.item.dto.ItemDtoShort;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.shareit.item.ItemMapper.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;

    private final UserRepository userRepository;

    @Lazy
    private final BookingRepository bookingRepository;

    private final CommentRepository commentRepository;


    @Override
    @Transactional
    public ItemDtoShort addItem(Long userId, ItemDtoShort itemDtoShort) {
        log.info("'addItem'", userId, itemDtoShort);
        User owner = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Item item = itemRepository.save(toItem(owner, itemDtoShort));
        log.info(item.toString());
        return toItemDto(item);
    }

    @Override
    @Transactional
    public ItemDtoShort updateItem(long userId, long itemId, ItemDtoShort itemDtoShort) {
        log.info("'getItemById'", userId, itemId);
        return itemRepository.findByOwner_IdAndId(userId, itemId).map(item -> {
            Item item1 = updateFromDto(item, itemDtoShort);
            itemRepository.save(item1);
            return toItemDto(item1);
        }).orElseThrow(() -> new ItemNotFoundException(itemId));
    }

    @Override
    public ItemDto getItemById(Long userId, long itemId) {
        log.info("'getItemById'", userId);
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
        return toResponseItem(item, getLastBooking(itemId, userId),
                getNextBooking(itemId, userId), getComments(itemId));
    }

    @Override
    public List<ItemDto> getUserItems(long userId) {
        List<Item> items = itemRepository.findAllByOwner_IdOrderById(userId);
        return items.stream().map(i -> toResponseItem(i, getLastBooking(i.getId(), i.getOwner().getId()),
                getNextBooking(i.getId(), i.getOwner().getId()), getComments(i.getId()))).collect(Collectors.toList());
    }

    @Override
    public List<ItemDtoShort> findItemByName(String text) {
        log.info("'findItemByName'", text);

        return itemRepository.search(text).stream().map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto addComment(long authorId, long itemId, CommentDto commentDto) {
        User author = userRepository.findById(authorId).orElseThrow(() -> new UserNotFoundException(authorId));
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
        bookingRepository.findFirstByItem_IdAndBooker_IdAndEndBefore(itemId, authorId, LocalDateTime.now()).orElseThrow(() ->
                new CommentCreationException(String.format("this user can't comment to this item")));
        Comment comment = toComment(author, item, commentDto);
        log.info("add comment", comment);
        return toCommentDto(commentRepository.save(comment));
    }

    private BookingShort getLastBooking(long itemId, long ownerId) {
        return bookingRepository.findFirstByItem_IdAndItem_Owner_IdAndEndBeforeOrderByEndAsc(itemId, ownerId,
                LocalDateTime.now()).map(BookingMapper::toBookingShort).orElse(null);
    }

    private BookingShort getNextBooking(long itemId, long ownerId) {
        return bookingRepository.findFirstByItem_IdAndItem_Owner_IdAndStartAfterOrderByStartDesc(itemId, ownerId,
                LocalDateTime.now()).map(BookingMapper::toBookingShort).orElse(null);
    }

    private List<CommentDto> getComments(long itemId) {
        return commentRepository.findAllByItem_Id(itemId).stream().map(ItemMapper::toCommentDto)
                .collect(Collectors.toList());
    }


}
