package com.kumoh_talk.streaming.domain.stream.service;

import com.kumoh_talk.streaming.domain.stream.dto.response.VodListResponse;
import com.kumoh_talk.streaming.domain.stream.dto.response.VodResponse;
import com.kumoh_talk.streaming.domain.stream.entity.Vod;
import com.kumoh_talk.streaming.domain.stream.repository.VodRepository;
import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import com.kumoh_talk.streaming.global.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VodService {
    private final VodRepository vodRepository;

    private final S3Service s3Service;

    public VodListResponse getVodList() {
        List<VodListResponse.VodInfo> vodList = vodRepository.findAll().stream()
                .map(vod -> VodListResponse.VodInfo.builder()
                        .vodId(vod.getId())
                        .thumbnailUrl(s3Service.generateThumbnailUrl(vod.getSlideUrl()))
                        .title(vod.getTitle())
                        .length(vod.getLength())
                        .views(vod.getViews())
                        .build()
                ).toList();

        return VodListResponse.builder()
                .vodList(vodList)
                .build();
    }

    public VodResponse getVod(Long vodId) {
        Vod vod = vodRepository.findById(vodId)
                .orElseThrow(() -> ServiceException.from(ExceptionCode.VOD_NOT_FOUND));

        String slideUrl = s3Service.generateSignedUrl(vod.getSlideUrl());
        String camUrl = s3Service.generateSignedUrl(vod.getCamUrl());

        return VodResponse.builder()
                .slideUrl(slideUrl)
                .slideTsQuery(slideUrl.split("\\?")[1])
                .camUrl(camUrl)
                .camTsQuery(camUrl.split("\\?")[1])
                // TODO. bookmark 추가
                .build();
    }
}
