package com.example.elephantfinancelab_be.domain.watchlist.service.command;

import com.example.elephantfinancelab_be.domain.user.entity.User;
import com.example.elephantfinancelab_be.domain.user.exception.UserException;
import com.example.elephantfinancelab_be.domain.user.exception.code.UserErrorCode;
import com.example.elephantfinancelab_be.domain.user.repository.UserRepository;
import com.example.elephantfinancelab_be.domain.watchlist.dto.req.WatchlistReqDTO;
import com.example.elephantfinancelab_be.domain.watchlist.entity.WatchlistGroup;
import com.example.elephantfinancelab_be.domain.watchlist.exception.WatchlistException;
import com.example.elephantfinancelab_be.domain.watchlist.exception.code.WatchlistErrorCode;
import com.example.elephantfinancelab_be.domain.watchlist.repository.WatchlistGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchlistCommandServiceImpl implements WatchlistCommandService {

  private final UserRepository userRepository;
  private final WatchlistGroupRepository watchlistGroupRepository;

  @Override
  public void saveGroup(Long userId, WatchlistReqDTO.CreateGroup request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
    WatchlistGroup group = WatchlistGroup.builder().user(user).name(request.getName()).build();
    watchlistGroupRepository.save(group);
  }

  @Override
  public void updateGroup(Long userId, Long groupId, WatchlistReqDTO.UpdateGroup request) {
    WatchlistGroup group = findGroupWithAuth(userId, groupId);
    group.updateName(request.getName());
  }

  @Override
  public void deleteGroup(Long userId, Long groupId) {
    WatchlistGroup group = findGroupWithAuth(userId, groupId);
    watchlistGroupRepository.delete(group);
  }

  private WatchlistGroup findGroupWithAuth(Long userId, Long groupId) {
    WatchlistGroup group =
        watchlistGroupRepository
            .findById(groupId)
            .orElseThrow(
                () -> new WatchlistException(WatchlistErrorCode.WATCHLIST_GROUP_NOT_FOUND));
    if (!group.getUser().getId().equals(userId)) {
      throw new WatchlistException(WatchlistErrorCode.WATCHLIST_GROUP_FORBIDDEN);
    }
    return group;
  }
}
