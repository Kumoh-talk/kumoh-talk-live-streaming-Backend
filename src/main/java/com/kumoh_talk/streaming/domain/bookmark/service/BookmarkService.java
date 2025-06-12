package com.kumoh_talk.streaming.domain.bookmark.service;

import com.kumoh_talk.streaming.domain.bookmark.dto.BookmarkListResponse;
import com.kumoh_talk.streaming.domain.bookmark.dto.CreateBookmarkRequest;
import com.kumoh_talk.streaming.domain.bookmark.persistent.entity.Bookmark;
import com.kumoh_talk.streaming.domain.bookmark.persistent.repository.BookmarkRepository;
import com.kumoh_talk.streaming.domain.stream.persistent.entity.Vod;
import com.kumoh_talk.streaming.domain.stream.persistent.repository.VodRepository;
import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import com.kumoh_talk.streaming.domain.bookmark.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
		private final VodRepository vodRepository;

    @Override
    public BookmarkListResponse getBookmarkList(Long userId, Long vodId) {
				Vod vod = vodRepository.findById(vodId)
					.orElseThrow(() -> ServiceException.from(ExceptionCode.VOD_NOT_FOUND));

				List<Bookmark> bookmarkList = bookmarkRepository.findByUserIdAndVod(userId, vod).stream()
            .map(b -> BookmarkListResponse.BookmarkInfo.builder()
								.bookmarkId(b.getId())
								.
						
						.build()(
                    b.getId(), b.getVodId(), b.getTitle(), b.getTime()
            ))
            .collect(Collectors.toList());
        return new BookmarkListResponse(list);
    }

    @Override
    public void createBookmark(Long userId, Long vodId, CreateBookmarkRequest req) {
        Bookmark b = new Bookmark(userId.intValue(), vodId, req.getTitle(), req.getTime());
        bookmarkRepository.save(b);
    }

    @Override
    public void deleteBookmark(Long userId, Long vodId, Long bookmarkId) {
        Bookmark b = bookmarkRepository
            .findByUserIdAndVodIdAndId(userId, vodId, bookmarkId)
            .orElseThrow(() -> new EntityNotFoundException("Bookmark not found"));
        bookmarkRepository.delete(b);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }
}
