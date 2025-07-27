package com.sastudios.gatekeeper.config

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.auth.ProgrammaticPlainTextAuthProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.core.CassandraTemplate
import java.io.File

@Configuration
class CassandraConfig {

    @Value("\${cassandra.username}")
    private lateinit var username: String

    @Value("\${cassandra.password}")
    private lateinit var password: String

    @Value("\${cassandra.keyspace}")
    private lateinit var keyspace: String

    @Bean
    fun cqlSession(): CqlSession {
        val resource = Thread.currentThread().contextClassLoader.getResource("secure-connect-gatekeeper-astra-db.zip")
            ?: throw IllegalStateException("Secure connect bundle not found in resources")

        val file = File(resource.file)

        return CqlSession.builder()
            .withCloudSecureConnectBundle(file.toPath())
            .withAuthProvider(ProgrammaticPlainTextAuthProvider(username, password))
            .withKeyspace(keyspace)
            .build()
    }

    @Bean
    fun cassandraTemplate(cqlSession: CqlSession): CassandraTemplate {
        return CassandraTemplate(cqlSession)
    }
}