package com.auralis.storage.s3;

import com.auralis.storage.StorageProperties;
import com.auralis.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final StorageProperties props;

    @Override
    public PresignedUpload presignUpload(String objectKey, String contentType) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(props.bucket())
                .key(objectKey)
                .contentType(contentType)
                .build();
        var presigned = presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(props.presignTtl())
                .putObjectRequest(put)
                .build());
        return new PresignedUpload(
                presigned.url().toString(),
                "PUT",
                Map.of("Content-Type", contentType),
                objectKey,
                Instant.now().plus(props.presignTtl()));
    }

    @Override
    public PresignedDownload presignDownload(String objectKey, String downloadFilename) {
        GetObjectRequest.Builder get = GetObjectRequest.builder()
                .bucket(props.bucket())
                .key(objectKey);
        if (downloadFilename != null) {
            get.responseContentDisposition("attachment; filename=\"" + downloadFilename + "\"");
        }
        var presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(props.presignTtl())
                .getObjectRequest(get.build())
                .build());
        return new PresignedDownload(presigned.url().toString(), Instant.now().plus(props.presignTtl()));
    }

    @Override
    public void upload(String objectKey, Path source, String contentType) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(props.bucket())
                        .key(objectKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromFile(source));
    }

    @Override
    public void download(String objectKey, Path destination) {
        s3.getObject(GetObjectRequest.builder().bucket(props.bucket()).key(objectKey).build(),
                ResponseTransformer.toFile(destination));
    }

    @Override
    public void delete(String objectKey) {
        s3.deleteObject(b -> b.bucket(props.bucket()).key(objectKey));
    }

    @Override
    public boolean exists(String objectKey) {
        return size(objectKey) >= 0;
    }

    @Override
    public long size(String objectKey) {
        try {
            return s3.headObject(HeadObjectRequest.builder().bucket(props.bucket()).key(objectKey).build())
                    .contentLength();
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            // headObject 404s as S3Exception (not NoSuchKeyException) on many S3-compatible stores.
            return -1;
        }
    }
}
