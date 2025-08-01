package com.sastudios.gatekeeper.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class S3Config {
    @Bean
    fun s3Client(): S3Client {
        return S3Client.builder()
            .region(Region.AP_SOUTH_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()
    }
}
