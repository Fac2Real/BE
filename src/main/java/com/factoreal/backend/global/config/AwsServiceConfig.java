package com.factoreal.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class AwsServiceConfig {

    @Value("${aws.access-key}")
    private String accessKey;

    @Value("${aws.secret-key}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Bean
    public AwsBasicCredentials awsCredentials() {
        return AwsBasicCredentials.create(accessKey, secretKey);
    }

    @Bean
    public SecretsManagerClient awsSecretsManagerClient() {
        return  SecretsManagerClient.builder()
                .region(Region.of(region))
                .credentialsProvider(this::awsCredentials)
                .build();
    }

    @Bean
    public IotDataPlaneClient iotDataPlaneClient() {
        return IotDataPlaneClient.builder()
                .region(Region.of(region))
                .credentialsProvider(this::awsCredentials)
                .build();
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(this::awsCredentials)
                .build();
    }
}
