package ru.practicum.shareit.request;

import ru.practicum.shareit.request.model.ItemRequestDto;
import ru.practicum.shareit.request.model.ItemRequestDtoEntry;

import java.util.*;

public interface RequestService {

    ItemRequestDto addRequest(long userId, ItemRequestDtoEntry itemRequestDto);

    List<ItemRequestDto> getUserRequests(long userId);

    ItemRequestDto getRequest(long userId, long requestId);

    List<ItemRequestDto> getRequests(long userId, Integer from, Integer size);
}