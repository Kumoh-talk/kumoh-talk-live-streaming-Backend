package com.kumoh_talk.streaming.global.file.service;

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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

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

        putObjectRequest(objectName, filePath);
    }

    private void putObjectRequest(String objectName, Path filePath) {
        try (
                S3Client s3Client = createS3Client();
        ) {
            PutObjectRequest putObjectsRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectName)
                    .build();

            s3Client.putObject(putObjectsRequest, RequestBody.fromFile(filePath));
            log.info("파일 업로드 완료: {}", filePath);
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
