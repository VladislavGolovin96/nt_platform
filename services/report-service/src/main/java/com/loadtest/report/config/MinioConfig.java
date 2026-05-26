package com.loadtest.report.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.public-endpoint}")
    private String publicEndpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-reports}")
    private String reportsBucket;

    /** Internal client — used for upload/download inside Docker network. */
    @Bean
    public AmazonS3 amazonS3() {
        return buildClient(endpoint);
    }

    /**
     * Public client — used ONLY for presigned URL generation.
     * Configured with the browser-reachable endpoint so the AWS Signature V4
     * is computed for the correct Host header (localhost:9000, not minio:9000).
     */
    @Bean
    public AmazonS3 amazonS3Public() {
        return buildClient(publicEndpoint);
    }

    private AmazonS3 buildClient(String ep) {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(ep, "us-east-1"))
                .withCredentials(
                        new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withPathStyleAccessEnabled(true)
                .build();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void ensureBucketsExist() {
        if (!amazonS3().doesBucketExistV2(reportsBucket)) {
            amazonS3().createBucket(reportsBucket);
        }
    }
}
