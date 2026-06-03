package com.nhan.social.post.service

import com.nhan.social.post.dto.PresignResponse
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration
import java.util.UUID

@ApplicationScoped
class S3Service(
    private val s3Presigner: S3Presigner,
) {
    @ConfigProperty(name = "app.s3.bucket", defaultValue = "social-images")
    lateinit var bucket: String

    @ConfigProperty(name = "app.s3.public-url", defaultValue = "http://localhost:4566/social-images")
    lateinit var publicUrl: String

    fun presignUpload(filename: String, contentType: String): PresignResponse {
        val key = "images/${UUID.randomUUID()}-${filename}"
        val putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .putObjectRequest(putRequest)
            .build()

        val presigned = s3Presigner.presignPutObject(presignRequest)
        return PresignResponse(
            uploadUrl = presigned.url().toString(),
            imageUrl = "$publicUrl/$key",
        )
    }
}
