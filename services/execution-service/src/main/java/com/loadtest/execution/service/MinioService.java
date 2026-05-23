package com.loadtest.execution.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    @Value("${minio.bucket-artifacts}")
    private String artifactsBucket;

    private final AmazonS3 amazonS3;

    public MinioService(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public Path downloadArtifact(String artifactPath, Path targetDir) throws IOException, InterruptedException {
        log.info("Downloading artifact {} from MinIO bucket {}", artifactPath, artifactsBucket);

        S3Object s3Object = amazonS3.getObject(artifactsBucket, artifactPath);
        Path tarGz = Files.createTempFile("artifact-", ".tar.gz");

        try (InputStream in = s3Object.getObjectContent()) {
            Files.copy(in, tarGz, StandardCopyOption.REPLACE_EXISTING);
        }

        Files.createDirectories(targetDir);
        extract(tarGz, targetDir);
        Files.deleteIfExists(tarGz);

        log.info("Artifact extracted to {}", targetDir);
        return targetDir;
    }

    private void extract(Path tarGz, Path targetDir) throws IOException, InterruptedException {
        Process process = new ProcessBuilder("tar", "-xzf", tarGz.toString(), "-C", targetDir.toString())
                .redirectErrorStream(true)
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("tar extraction failed with exit code " + exitCode);
        }
    }
}
