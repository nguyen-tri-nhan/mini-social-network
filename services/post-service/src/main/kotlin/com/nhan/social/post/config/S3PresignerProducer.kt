package com.nhan.social.post.config

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import org.eclipse.microprofile.config.inject.ConfigProperty
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.util.Optional

@ApplicationScoped
class S3PresignerProducer {

    @ConfigProperty(name = "quarkus.s3.aws.region", defaultValue = "us-east-1")
    lateinit var region: String

    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.access-key-id", defaultValue = "test")
    lateinit var accessKeyId: String

    @ConfigProperty(name = "quarkus.s3.aws.credentials.static-provider.secret-access-key", defaultValue = "test")
    lateinit var secretAccessKey: String

    @ConfigProperty(name = "quarkus.s3.endpoint-override")
    var endpointOverride: Optional<String> = Optional.empty()

    @Produces
    @ApplicationScoped
    fun presigner(): S3Presigner {
        val builder = S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                )
            )

        endpointOverride.ifPresent { builder.endpointOverride(URI.create(it)) }

        return builder.build()
    }
}
