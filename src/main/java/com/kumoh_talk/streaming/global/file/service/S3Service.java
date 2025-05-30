package com.kumoh_talk.streaming.global.file.service;

import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.file.Path;
import java.util.List;

import static com.kumoh_talk.streaming.global.constant.StreamingConstants.M3U8_NAME;
import static com.kumoh_talk.streaming.global.constant.StreamingConstants.VOD_PATH;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    @Value("${aws.s3.credentials.accessKey}")
    private String accessKey;

    @Value("${aws.s3.credentials.secretKey}")
    private String secretKey;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private Region region;

    public void uploadHlsFile(Path filePath) {
        int dirCount = filePath.getNameCount();
        Path streamKeyAndFileName = filePath.subpath(dirCount - 2, dirCount);

        String objectName = VOD_PATH + "/" + streamKeyAndFileName;

        this.putObjectRequest(objectName, filePath);
    }

    private void putObjectRequest(String objectName, Path filePath) {
        this.putObjectRequest(objectName, filePath, false);
    }

    private void putObjectRequest(String objectName, Path filePath, boolean isRetry) {
        try (
                S3Client s3Client = createS3Client();
        ) {
            PutObjectRequest putObjectsRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectName)
                    .build();

            s3Client.putObject(putObjectsRequest, RequestBody.fromFile(filePath));
            log.info("파일 업로드 완료: {}", filePath);
        } catch (Exception e) {
            if (!isRetry) {
                log.warn("업로드 실패, 재시도 중...: {}", filePath);
                putObjectRequest(objectName, filePath, true);
            } else {
                log.error("업로드 재시도 실패: {}", filePath, e);
            }
        }
    }

    public void uploadM3U8File(String streamKey, byte[] bytes) {
        String objectName = String.join("/", VOD_PATH, streamKey, M3U8_NAME);
        this.putObjectRequestFromStream(objectName, bytes);
    }

    private void putObjectRequestFromStream(String objectName, byte[] bytes) {
        try (
                S3Client s3Client = createS3Client();
        ) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
            log.info("m3u8 파일 업로드 완료: {}", objectName);
        }
    }

    public List<String> getFileList(String streamKey) {
        try (
                S3Client s3Client = createS3Client();
        ) {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(VOD_PATH + "/" + streamKey)
                    .build();

            return s3Client.listObjectsV2(request)
                    .contents()
                    .stream()
                    .map(S3Object::key)
                    .toList();
        }
    }

    private S3Client createS3Client() {
        return S3Client.builder()
                .credentialsProvider(getCredentialsProvider())
                .region(region)
                .build();
    }

    private AwsCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(getAwsCredentials());
    }

    private AwsBasicCredentials getAwsCredentials() {
        return AwsBasicCredentials.create(accessKey, secretKey);
    }
}
