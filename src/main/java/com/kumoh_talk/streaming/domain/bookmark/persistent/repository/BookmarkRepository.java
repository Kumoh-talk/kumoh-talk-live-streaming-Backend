package com.kumoh_talk.streaming.domain.bookmark.persistent.repository;

import com.kumoh_talk.streaming.domain.bookmark.persistent.entity.Bookmark;
import com.kumoh_talk.streaming.domain.stream.persistent.entity.Vod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
	List<Bookmark> findByUserIdAndVod(Long userId, Vod vod);
}
