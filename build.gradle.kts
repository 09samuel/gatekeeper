plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "1.9.25"
}

group = "com.sastudios"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring & Web
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	// Security & JWT
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.security:spring-security-crypto")
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// PostgreSQL (JPA)
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.postgresql:postgresql")

	// Astra DB (Cassandra)
	implementation("com.datastax.astra:astra-spring-boot-starter:1.2.0")
	implementation("org.springframework.boot:spring-boot-starter-data-cassandra")

	testImplementation("org.testcontainers:cassandra:1.19.7")
	testImplementation("org.testcontainers:junit-jupiter:1.19.7")


	// Kafka (if needed)
	implementation("org.springframework.kafka:spring-kafka")

	// AWS S3 (if needed)
	implementation("software.amazon.awssdk:s3:2.25.29")

	// Dev & Test
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")

	//implementation("org.springframework.boot:spring-boot-starter")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	implementation("org.redisson:redisson-spring-boot-starter:3.23.5")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")


}


kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.1")
	}
}

repositories {
	mavenCentral()
	maven {
		url = uri("https://oss.sonatype.org/content/repositories/snapshots")
	}
	maven {
		url = uri("https://datastax.jfrog.io/artifactory/maven")
	}
}
