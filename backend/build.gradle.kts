plugins {
    java
    alias(libs.plugins.spring.boot)
}

group = "com.example.xapp"
version = "0.0.1-SNAPSHOT"
description = "x_app_spring backend API"

java {
    toolchain {
        // ホストに JDK 25 が無くても Foojay 経由で自動取得される（settings.gradle.kts 参照）
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

dependencies {
    // BOM。starter 群のバージョンはここで揃う
    implementation(platform(libs.spring.boot.bom))
    testImplementation(platform(libs.spring.boot.bom))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.actuator)

    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.bucket4j.core)

    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.swagger.request.validator.mockmvc)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // Testcontainers はホストの Docker ソケット経由で PostgreSQL を起動する。
    // compose.yaml の db とはポートが衝突しない（Testcontainers はランダムポート）。
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = false
    }
}
