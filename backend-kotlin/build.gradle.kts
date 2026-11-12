plugins {
    kotlin("jvm") version "2.2.10"
    application
}
kotlin { jvmToolchain(17) }
application { mainClass.set("com.smartcleaner.backend.ApplicationKt") }
dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.3.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.3.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.3.1")
    implementation("io.ktor:ktor-serialization-gson-jvm:3.3.1")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation(kotlin("test"))
}
tasks.test { useJUnitPlatform() }
