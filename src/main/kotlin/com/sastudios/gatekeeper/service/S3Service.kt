package com.sastudios.gatekeeper.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI

@Service
class S3Service(
    @Value("\${aws.s3.bucket}") private val bucket: String,
    private val s3Client: S3Client
) {
    fun uploadPlainText(docId: Long, content: String): String {
        val key = "documents/$docId.txt"
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("text/plain")
                .build(),
            RequestBody.fromString(content)
        )
        return "https://$bucket.s3.amazonaws.com/$key"
    }

    fun getPlainText(fullUrl: String): String {
        val objectKey = extractKeyFromUrl(fullUrl)

        val request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey)
            .build()

        s3Client.getObject(request).use { s3Object ->
            BufferedReader(InputStreamReader(s3Object)).use { reader ->
                return reader.readText()
            }
        }
    }

    private fun extractKeyFromUrl(url: String): String {
        // Example: https://bucket-name.s3.amazonaws.com/documents/file.txt → documents/file.txt
        val uri = URI(url)
        return uri.path.removePrefix("/") // Removes the leading slash
    }

}

