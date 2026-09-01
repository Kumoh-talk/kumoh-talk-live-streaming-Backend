# 📡 API 요약

모든 REST 응답은 공통 `ResponseBody` 형식이며, 인증 요청은 `Authorization: Bearer <JWT>` 헤더를 사용합니다.

## REST API

| 기능 | Method | Endpoint | 인증 |
| --- | --- | --- | --- |
| 스트림 키 발급 | POST | `/stream/streamKey` | USER, ADMIN |
| 스트림 키 조회 | GET | `/stream/streamKey` | USER, ADMIN |
| 스트리밍 제목 변경 | PATCH | `/stream/title` | USER, ADMIN |
| 스트리밍 목록 조회 | GET | `/stream/list` | 공개 |
| 스트리밍 상세 조회 | GET | `/stream/{streamId}` | 공개 |
| 투표 생성 | POST | `/vote/{streamId}` | USER, ADMIN |
| 투표 결과 조회 | GET | `/vote/{streamId}/{voteId}` | USER, ADMIN |
| 투표 종료 | DELETE | `/vote/{streamId}/{voteId}` | USER, ADMIN |
| VOD 목록 조회 | GET | `/vod` | USER |
| VOD 상세 조회 | GET | `/vod/{vodId}` | USER |
| 북마크 목록 조회 | GET | `/bookmark/{vodId}` | USER |
| 북마크 생성 | POST | `/bookmark/{vodId}` | USER |
| 북마크 삭제 | DELETE | `/bookmark/{vodId}/{bookmarkId}` | USER |
| 송출 시작 알림 | POST | `/stream/start` | Nginx-RTMP 연동 |
| 송출 종료 알림 | POST | `/stream/stop` | Nginx-RTMP 연동 |
| 자막 수신 | POST | `/stream/caption` | Audio API 연동 |
| 요약 수신 | POST | `/stream/summary` | Audio API 연동 |

## WebSocket

- 연결 endpoint: `/ws-stomp` (WebSocket, SockJS)
- 클라이언트 SEND prefix: `/app`
- 인증이 필요한 SEND: native header `Authorization: Bearer <JWT>`

### SEND destination

| 기능 | Destination | 인증 |
| --- | --- | --- |
| 투표 목록 요청 | `/app/streaming/{streamId}/vote-list` | 공개 |
| 투표 참여 | `/app/streaming/{streamId}/submit-vote/{voteId}` | USER |
| Q&A 목록 요청 | `/app/streaming/{streamId}/qna-list` | 공개 |
| Q&A 등록 | `/app/streaming/{streamId}/add-qna` | USER |
| Q&A 좋아요 | `/app/streaming/{streamId}/liked-qna/{qnaId}` | USER |
| Q&A 삭제 | `/app/streaming/{streamId}/delete-qna/{qnaId}` | USER |
| 채팅 전송 | `/app/streaming/{streamId}/add-chat` | USER |

### SUBSCRIBE destination

| 기능 | Destination |
| --- | --- |
| 투표 목록 | `/streaming/vote-list/{sessionId}` |
| 투표 생성·종료 | `/streaming/vote/{streamId}/add-vote`, `/streaming/vote/{streamId}/close` |
| Q&A 목록 | `/streaming/qna-list/{sessionId}` |
| Q&A 등록·좋아요·삭제 | `/streaming/qna/{streamId}/add`, `/liked`, `/delete` |
| 채팅 | `/streaming/chat/{streamId}/add` |
| 제목 변경 | `/streaming/title/{streamId}` |
| 자막·요약 | `/streaming/caption`, `/streaming/summary` |
| 세션 전용 오류 | `/user/{sessionId}/queue/errors` |
