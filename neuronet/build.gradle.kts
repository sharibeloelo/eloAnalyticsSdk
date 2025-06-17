plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.greenhorn.neuronet"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    publishing {
        singleVariant("release") {
            // This option automatically includes the AAR from the 'release' build.
            // You can also easily include the source code and Javadoc:
            withSourcesJar()
            // withJavadocJar() // Uncomment if you have Javadoc to publish
        }
    }
}

publishing {
    publications {
        // Creates a publication called "release" (you can name it anything)
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
                // Use the group, name, and version from your error log or your desired coordinates
                groupId = "com.github.greenhorn-eloelo-event"
                artifactId = "analytics" // The name of your library module
                version = "1.0.3"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Ktor
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)


    implementation(libs.kotlinx.serialization.json)

    // Work-Manager
    implementation(libs.androidx.work.runtime.ktx)

    //Room-Db
    implementation(libs.androidx.room.ktx)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    implementation(libs.gson)
}