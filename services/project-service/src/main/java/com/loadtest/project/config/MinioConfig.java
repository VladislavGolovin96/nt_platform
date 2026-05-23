package com.loadtest.project.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.bucket-artifacts}")
    private String artifactsBucket;

    @Bean
    public AmazonS3 amazonS3(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey) {

        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1"))
                .withCredentials(
                        new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .withPathStyleAccessEnabled(true)
                .build();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void ensureBucketsExist(ContextRefreshedEvent event) {
        AmazonS3 s3 = event.getApplicationContext().getBean(AmazonS3.class);
        if (!s3.doesBucketExistV2(artifactsBucket)) {
            s3.createBucket(artifactsBucket);
            log.info("Created MinIO bucket: {}", artifactsBucket);
        }
    }
}
