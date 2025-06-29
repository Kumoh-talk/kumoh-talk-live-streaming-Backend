package com.kumoh_talk.streaming.global.file.service;

import com.kumoh_talk.streaming.global.exception.ExceptionCode;
import com.kumoh_talk.streaming.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontUtilities;
import software.amazon.awssdk.services.cloudfront.model.CustomSignerRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.kumoh_talk.streaming.domain.stream.constant.StreamingConstants.*;

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

    @Value("${aws.cloudfront.domainName}")
    private String distributionDomainName;

    @Value("${aws.cloudfront.credentials.publicKey}")
    private String publicKeyId;

    @Value("${aws.cloudfront.credentials.privateKeyPath}")
    private Path privateKeyPath;

    private final static int GET_REQUEST_DURATION_OF_MINUTES = 60;

    public void uploadHlsFile(String filename, String streamWatchKey) {
        String objectName = String.join("/", VOD_PATH, streamWatchKey, filename);

        this.putObjectRequest(objectName, Path.of(HLS_OUTPUT_DIR, filename));
    }

    public void uploadThumbnail(String streamWatchKey, Path filePath) {
        String objectName = String.join("/", VOD_PATH, streamWatchKey, THUMBNAIL_NAME);

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
        } catch (Exception e) {
            if (!isRetry) {
                log.warn("업로드 실패, 재시도 중...: {}", filePath);
                putObjectRequest(objectName, filePath, true);
            } else {
                log.error("업로드 재시도 실패: {}: {}", filePath, e.getMessage());
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

    public String generateThumbnailUrl(String streamWatchKey) {
        return this.generatePreSignedUrl(getThumbnailUrl(streamWatchKey));
    }

    private String getThumbnailUrl(String streamWatchKey) {
        return String.join("/", VOD_PATH, streamWatchKey, THUMBNAIL_NAME);
    }

    private String generatePreSignedUrl(String resourcePath) {
        try (
                S3Presigner s3Presigner = createS3Presigner();
        ) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(resourcePath)
                    .build();

            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(GET_REQUEST_DURATION_OF_MINUTES))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedGetObjectRequest = s3Presigner.presignGetObject(getObjectPresignRequest);

            return presignedGetObjectRequest.url().toString();
        }
    }

    public String generateSignedUrl(String resourcePath) {
        try {
            CustomSignerRequest signerRequest = createCustomSignerRequest(resourcePath);
            CloudFrontUtilities cloudFrontUtilities = CloudFrontUtilities.create();
            return cloudFrontUtilities.getSignedUrlWithCustomPolicy(signerRequest).url();
        } catch (Exception e) {
            log.error("cloudfront signed-url 발급 중 오류 발생: {}", e.getMessage());
            throw ServiceException.from(ExceptionCode.SIGNED_URL_GENERATION_FAILED);
        }
    }

    private CustomSignerRequest createCustomSignerRequest(String resourcePath) throws Exception {
        String cloudFrontUrl = distributionDomainName + "/" + resourcePath;
        String resourceUrl = cloudFrontUrl + "/" + M3U8_NAME;
        String resourceUrlPattern = cloudFrontUrl + "/*";
        Instant expireDate = Instant.now().plus(GET_REQUEST_DURATION_OF_MINUTES, ChronoUnit.MINUTES);

        return CustomSignerRequest.builder()
                .resourceUrl(resourceUrl)
                .resourceUrlPattern(resourceUrlPattern)
                .privateKey(privateKeyPath)
                .keyPairId(publicKeyId)
                .expirationDate(expireDate)
                .build();
    }

    private S3Client createS3Client() {
        return S3Client.builder()
                .credentialsProvider(getCredentialsProvider())
                .region(region)
                .build();
    }

    private S3Presigner createS3Presigner() {
        return S3Presigner.builder()
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
