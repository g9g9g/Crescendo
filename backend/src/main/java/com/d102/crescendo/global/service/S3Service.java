package com.d102.crescendo.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    public URL generatePresignedUrl(String key, String contentType, long expiresInSeconds) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
//                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expiresInSeconds))
                .putObjectRequest(objectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url();
    }

    public String buildFileUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    /**
     * InputStream을 S3에 직접 업로드
     * @param key S3 객체 키 (경로 포함)
     * @param inputStream 업로드할 파일의 InputStream
     * @param contentLength 파일 크기 (bytes)
     * @param contentType MIME 타입
     * @return 업로드된 파일의 URL
     */
    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) {
        return uploadFile(key, inputStream, contentLength, contentType, null);
    }

    /**
     * InputStream을 S3에 직접 업로드 (ACL 설정 가능)
     * @param key S3 객체 키 (경로 포함)
     * @param inputStream 업로드할 파일의 InputStream
     * @param contentLength 파일 크기 (bytes)
     * @param contentType MIME 타입
     * @param acl ACL 설정 (null이면 기본값 사용)
     * @return 업로드된 파일의 URL
     */
    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType, ObjectCannedACL acl) {
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType);

        if (acl != null) {
            requestBuilder.acl(acl);
        }

        s3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(inputStream, contentLength));

        return buildFileUrl(key);
    }
}
