package com.kumoh_talk.streaming.domain.bookmark.service;

import com.kumoh_talk.streaming.domain.bookmark.dto.request.CreateBookmarkRequest;
import com.kumoh_talk.streaming.domain.bookmark.dto.response.BookmarkListResponse;
import com.kumoh_talk.streaming.domain.bookmark.persistent.entity.Bookmark;
import com.kumoh_talk.streaming.domain.bookmark.persistent.repository.BookmarkRepository;
import com.kumoh_talk.streaming.domain.stream.persistent.entity.Vod;
import com.kumoh_talk.streaming.domain.stream.persistent.repository.VodRepository;
import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final VodRepository vodRepository;

    public BookmarkListResponse getBookmarkList(Long userId, Long vodId) {
        Vod vod = vodRepository.findById(vodId)
                .orElseThrow(() -> ServiceException.from(ExceptionCode.VOD_NOT_FOUND));

        List<BookmarkListResponse.BookmarkInfo> bookmarkInfoList = bookmarkRepository.findByUserIdAndVod(userId, vod).stream()
                .map(b -> BookmarkListResponse.BookmarkInfo.builder()
                        .bookmarkId(b.getId())
                        .title(b.getTitle())
                        .time(b.getTime())
                        .build()
                ).toList();

        return BookmarkListResponse.builder()
                .bookmarkList(bookmarkInfoList)
                .build();
    }

    @Transactional
    public void createBookmark(Long userId, Long vodId, CreateBookmarkRequest request) {
        Vod vod = vodRepository.findById(vodId)
                .orElseThrow(() -> ServiceException.from(ExceptionCode.VOD_NOT_FOUND));

        Bookmark newBookmark = Bookmark.builder()
                .userId(userId)
                .vod(vod)
                .title(request.title())
                .time(request.time())
                .build();

        bookmarkRepository.save(newBookmark);
    }

    @Transactional
    public void deleteBookmark(Long userId, Long vodId, Long bookmarkId) {
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> ServiceException.from(ExceptionCode.BOOKMARK_NOT_FOUND));

        if (!userId.equals(bookmark.getUserId())) {
            throw ServiceException.from(ExceptionCode.FORBIDDEN);
        }

        if (!vodId.equals(bookmark.getVod().getId())) {
            throw ServiceException.from(ExceptionCode.INVALID_VOD);
        }

        bookmarkRepository.delete(bookmark);
    }
}
