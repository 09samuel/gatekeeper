package com.sastudios.gatekeeper

import org.springframework.boot.test.autoconfigure.data.cassandra.DataCassandraTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.CassandraContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test

@DataCassandraTest
@Testcontainers
class CassandraRepositoryTest {

    @Container
    private val cassandra = CassandraContainer("cassandra:4.1")

    @DynamicPropertySource
    fun setProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.cassandra.contact-points") { cassandra.contactPoint }
        registry.add("spring.cassandra.port") { cassandra.getMappedPort(9042) }
        registry.add("spring.cassandra.local-datacenter") { cassandra.localDatacenter }
    }

    @Test
    fun testConnection() {
        // Your test here
    }
}