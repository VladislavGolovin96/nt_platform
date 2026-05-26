package com.loadtest.project.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private final AmazonS3 s3;
    private final String bucket;

    public MinioService(AmazonS3 s3,
                        @Value("${minio.bucket-artifacts}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    /**
     * Packages classesDir as tar.gz and uploads to MinIO.
     * Key pattern: {userId}/{projectId}/test-classes-{timestamp}.tar.gz
     *
     * @return the MinIO object key (artifactPath)
     */
    public String uploadArtifact(String userId, String projectId, Path classesDir)
            throws IOException, InterruptedException {

        String key = userId + "/" + projectId + "/test-classes-"
                + System.currentTimeMillis() + ".tar.gz";

        Path tarFile = Files.createTempFile("artifact-", ".tar.gz");
        try {
            createTarGz(classesDir, tarFile);
            log.info("Uploading artifact {} → s3://{}/{}", tarFile, bucket, key);
            s3.putObject(new PutObjectRequest(bucket, key, tarFile.toFile()));
            log.info("Upload complete: {}", key);
            return key;
        } finally {
            Files.deleteIfExists(tarFile);
        }
    }

    /**
     * Downloads artifact from MinIO and extracts it into targetDir.
     */
    public void downloadArtifact(String artifactPath, Path targetDir)
            throws IOException, InterruptedException {

        log.info("Downloading artifact s3://{}/{} → {}", bucket, artifactPath, targetDir);
        Files.createDirectories(targetDir);

        Path tarFile = Files.createTempFile("artifact-download-", ".tar.gz");
        try {
            S3Object s3Object = s3.getObject(new GetObjectRequest(bucket, artifactPath));
            try (InputStream in = s3Object.getObjectContent()) {
                Files.copy(in, tarFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            extractTarGz(tarFile, targetDir);
            log.info("Artifact extracted to {}", targetDir);
        } finally {
            Files.deleteIfExists(tarFile);
        }
    }

    private void createTarGz(Path sourceDir, Path outputFile) throws IOException, InterruptedException {
        // tar -czf <output> -C <sourceDir> . — archives contents directly (no wrapper dir)
        // so on extraction: targetDir/demo/ApiSimulation.class (not targetDir/test-classes/demo/...)
        ProcessBuilder pb = new ProcessBuilder(
                "tar", "-czf", outputFile.toAbsolutePath().toString(),
                "-C", sourceDir.toAbsolutePath().toString(),
                "."
        ).redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String err = new String(process.getInputStream().readAllBytes());
            throw new IOException("tar failed (exit=" + exitCode + "): " + err);
        }
    }

    private void extractTarGz(Path tarFile, Path targetDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "tar", "-xzf", tarFile.toAbsolutePath().toString(),
                "-C", targetDir.toAbsolutePath().toString()
        ).redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String err = new String(process.getInputStream().readAllBytes());
            throw new IOException("tar extract failed (exit=" + exitCode + "): " + err);
        }
    }
}
