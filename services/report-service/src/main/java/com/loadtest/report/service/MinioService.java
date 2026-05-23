package com.loadtest.report.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Date;

@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    @Value("${minio.bucket-reports}")
    private String reportsBucket;

    private final AmazonS3 amazonS3;

    public MinioService(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public String uploadReport(String executionId, byte[] pdfBytes) {
        String key = "reports/" + executionId + ".pdf";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(pdfBytes.length);
        metadata.setContentType("application/pdf");

        amazonS3.putObject(reportsBucket, key, new ByteArrayInputStream(pdfBytes), metadata);
        log.info("Uploaded PDF report for execution {} to {}/{}", executionId, reportsBucket, key);
        return key;
    }

    public String generatePresignedUrl(String key) {
        Date expiry = new Date(System.currentTimeMillis() + 3600_000L); // 1 hour
        GeneratePresignedUrlRequest urlRequest =
                new GeneratePresignedUrlRequest(reportsBucket, key)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiry);
        return amazonS3.generatePresignedUrl(urlRequest).toString();
    }
}
