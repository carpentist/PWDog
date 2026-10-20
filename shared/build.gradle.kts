plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    jvmToolchain(21)

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.bouncycastle:bcprov-jdk18on:1.86")
                implementation("io.github.willena:sqlite-jdbc:3.53.4.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter:5.12.2")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
