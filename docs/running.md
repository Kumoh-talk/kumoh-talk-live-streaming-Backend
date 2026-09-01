# 🚀 실행 방법

## 📂 프로젝트 구조

```text
src/main/java/com/kumoh_talk/streaming
├─ domain
│  ├─ bookmark       # VOD 북마크
│  ├─ chat           # 실시간 채팅
│  ├─ qna            # 실시간 Q&A
│  ├─ stream         # 라이브 스트리밍과 VOD
│  └─ vote           # 실시간 투표
└─ global
   ├─ auth           # JWT 인증과 역할
   ├─ config         # Security, Redis, WebSocket, 비동기 설정
   ├─ exception      # REST·WebSocket 공통 예외 처리
   ├─ file           # S3, CloudFront 연동
   ├─ socket         # STOMP 세션과 메시지 인증
   ├─ util           # FFmpeg, Audio API 연동
   └─ watchService   # HLS 파일 감시와 업로드
```

## ✅ 사전 요구 사항

- Docker 및 Docker Compose
- AWS S3 버킷과 접근 자격 증명
- CloudFront 배포, 공개 키 ID, 서명용 개인 키
- 인증 서버와 공유하는 JWT Secret Key
- `/start`, `/end` 요청을 처리하는 외부 Audio API

## 🔐 환경 변수

프로젝트 루트에 `.env` 파일을 생성합니다. CloudFront 개인 키와 `.env`는 Git에 커밋하지 않습니다.

```dotenv
MYSQL_ROOT_PASSWORD=<mysql-root-password>
MYSQL_DATABASE=<database-name>
MYSQL_USER=<database-user>
MYSQL_PASSWORD=<database-password>

REDIS_PASSWORD=<redis-password>

BUCKET_NAME=<s3-bucket-name>
BUCKET_REGION=<aws-region>
BUCKET_ACCESS_KEY=<aws-access-key>
BUCKET_SECRET_KEY=<aws-secret-key>

CLOUDFRONT_DOMAIN_NAME=<cloudfront-domain-with-scheme>
CLOUDFRONT_PUBLIC_KEY_ID=<cloudfront-public-key-id>
HOST_CLOUDFRONT_PRIVATE_KEY_PATH=<private-key-path-on-host>
CLOUDFRONT_PRIVATE_KEY_PATH=/run/secrets/cloudfront-private-key.pem
CLOUDFRONT_PRIVATE_KEY_FULL_PATH=/run/secrets/cloudfront-private-key.pem

JWT_SECRET_KEY=<jwt-secret-key>
HLS_URL=<hls-base-url>
AUDIO_API_URL=<audio-api-base-url>
HLS_PATH=<host-directory-for-hls-files>
```

`CLOUDFRONT_PRIVATE_KEY_PATH`와 `CLOUDFRONT_PRIVATE_KEY_FULL_PATH`는 컨테이너 내부의 같은 파일을 가리켜야 합니다. `HLS_URL`과 `AUDIO_API_URL`은 코드에서 하위 경로를 결합하므로 마지막 `/`를 포함한 형태로 설정합니다.

## 🐳 Docker 실행

Spring 런타임 베이스 이미지는 FFmpeg와 curl을 포함합니다. 최초 한 번 빌드합니다.

```bash
docker build -f dockerfile/Dockerfile.spring-base -t kumohtalk-streaming-openjdk .
docker compose up --build -d
```

| 서비스 | 호스트 포트 | 용도 |
| --- | ---: | --- |
| Spring Boot | 8081 | REST API, WebSocket |
| Nginx RTMP | 1935 | RTMP 송출 |
| Nginx HTTP | 9091 | HLS, RTMP 상태 페이지 |
| MySQL | 3308 | 영속 데이터 |
| Redis | 6380 | 라이브 상태 |

```bash
curl http://localhost:8081/actuator/health
docker compose ps
docker compose down
```

`docker compose down -v`는 MySQL과 Redis 볼륨까지 삭제합니다.
