# 🏗️ 시스템 아키텍처

## 🎥 송출과 저장

<p align="center">
  <img src="images/rtmp_ingest_flow.png" width="100%" alt="RTMP 송출부터 HLS 변환과 저장까지의 흐름" />
</p>

1. 송출 프로그램이 `{streamKey}_desktop`, `{streamKey}_webcam` 두 RTMP 스트림을 Nginx-RTMP로 전송합니다.
2. Nginx의 `on_publish` 콜백이 Spring 서버에 송출 시작을 알립니다.
3. Spring은 스트림 키를 검증하고 FFmpeg를 실행해 각 입력을 1초 단위 HLS로 변환합니다.
4. 파일 감시기가 새 TS 세그먼트를 감지해 S3에 업로드합니다.
5. `on_done` 콜백을 받으면 VOD 플레이리스트를 생성하고 메타데이터를 MySQL에 저장합니다.

## 📡 시청과 실시간 상호작용

<p align="center">
  <img src="images/hls_delivery_flow.png" width="100%" alt="HLS 재생, WebSocket 상호작용, AI 자막과 VOD 전달 흐름" />
</p>

- 라이브 영상은 Nginx HTTP 서버의 HLS 경로를 통해 웹 클라이언트에 전달됩니다.
- 채팅·Q&A·투표와 자막·요약은 STOMP WebSocket으로 전달됩니다.
- 데스크톱 HLS에서 분리한 오디오는 외부 Audio API가 처리하고, 결과는 Spring을 거쳐 구독자에게 전파됩니다.
- VOD는 MySQL에서 메타데이터를 조회한 뒤 CloudFront Signed URL로 접근 권한을 제한합니다.

## 🗄️ 데이터 저장 기준

데이터의 수명과 접근 패턴에 따라 저장소를 분리했습니다.

| 데이터 | 저장소 | 선택 이유 |
| --- | --- | --- |
| 라이브 방송 정보·Q&A·투표·접속 세션 | Redis | 방송 중 빈번하게 변경되고 종료 후 수명이 짧은 상태 |
| 채팅·VOD 메타데이터·북마크 | MySQL | 방송 이후에도 조회해야 하는 관계형 데이터 |
| TS 세그먼트·플레이리스트·썸네일 | Amazon S3 | 용량이 큰 정적 미디어의 내구성 있는 저장 |
| VOD 전송 | Amazon CloudFront | CDN 전송과 Signed URL을 통한 제한된 접근 |

## 🗂️ 데이터 모델

영속 데이터는 VOD·북마크·채팅으로 한정하고, 방송 중에만 필요한 상태는 Redis에 분리했습니다.

<p align="center">
  <img src="images/erd.png" width="100%" alt="Kumoh Talk 데이터 모델 ERD" />
</p>

Redis에는 `Streaming`과 `Vote` 객체를 저장하고, Q&A·좋아요·투표 선택·접속 세션은 Hash와 Set으로 관리합니다. 외부 인증 서버의 사용자는 이 서비스에서 `userId`로만 참조해 사용자 데이터의 소유 경계를 분리했습니다.
