package com.footwork.api.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import net.coobird.thumbnailator.Thumbnails;

@Service
public class S3StorageService {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final String bucketName;
  private final Region region;

  public S3StorageService(
      @Value("${aws.region}") String awsRegion,
      @Value("${aws.s3.bucket}") String bucketName
  ) {
    this.region = Region.of(awsRegion);
    this.bucketName = bucketName;
    this.s3Client = S3Client.builder()
        .region(this.region)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
    this.s3Presigner = S3Presigner.builder()
        .region(this.region)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();
  }

  // Uploads object to a private bucket (no public ACL). Returns the S3 object key stored under profile-pictures/...
  public String uploadProfileImage(byte[] bytes, String contentType, String keyPrefix) {
    String objectKey = buildObjectKey(keyPrefix);
    
    // Compress/resize: max dimension 1024px, JPEG quality ~0.8
    byte[] processed = processImage(bytes);

    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .contentType("image/jpeg")
        .build();

    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(processed));

    return objectKey;
  }

  public String generatePresignedGetUrl(String objectKey, Duration ttl) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .build();

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(ttl)
        .getObjectRequest(getObjectRequest)
        .build();

    return s3Presigner.presignGetObject(presignRequest).url().toString();
  }

  private String buildObjectKey(String keyPrefix) {
    String safePrefix = URLEncoder.encode(keyPrefix, StandardCharsets.UTF_8);
    String uuid = UUID.randomUUID().toString();
    long ts = Instant.now().toEpochMilli();
    return String.format("profile-pictures/%s/%d-%s.jpg", safePrefix, ts, uuid);
  }

  public void deleteObject(String objectKey) {
    if (objectKey == null || objectKey.isEmpty()) {
      return;
    }
    // If a URL was stored instead of a raw key, normalize it to an object key
    objectKey = normalizeKey(objectKey);
    DeleteObjectRequest req = DeleteObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .build();
    s3Client.deleteObject(req);
  }

  /**
   * Accepts either a raw S3 object key (e.g., "profile-pictures/user/x.jpg") or a URL
   * (including presigned). Returns the object key suitable for S3 API calls.
   */
  public String normalizeKey(String maybeKeyOrUrl) {
    if (maybeKeyOrUrl == null || maybeKeyOrUrl.isEmpty()) {
      return maybeKeyOrUrl;
    }
    if (maybeKeyOrUrl.startsWith("http://") || maybeKeyOrUrl.startsWith("https://")) {
      try {
        URI uri = URI.create(maybeKeyOrUrl);
        String path = uri.getPath();
        if (path != null && path.startsWith("/")) {
          return path.substring(1); // drop leading '/'
        }
      } catch (Exception ignored) {
        // fall through
      }
    }
    return maybeKeyOrUrl;
  }

  private byte[] processImage(byte[] inputBytes) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      // auto-orient, scale longest side to 1024, output JPEG with quality 0.8
      Thumbnails.of(new ByteArrayInputStream(inputBytes))
          .size(1024, 1024)
          .outputFormat("jpeg")
          .outputQuality(0.8f)
          .toOutputStream(baos);
      return baos.toByteArray();
    } catch (Exception e) {
      // Fallback: return original bytes if processing fails
      return inputBytes;
    }
  }
}


