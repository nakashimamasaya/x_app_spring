rootProject.name = "x-app-spring"

include("backend")

// Java 25 (LTS) をツールチェーンで使う。ホストに JDK 25 が無くても
// Foojay の解決プラグイン経由で自動取得される。
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
