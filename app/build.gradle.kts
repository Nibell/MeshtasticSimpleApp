import org.gradle.kotlin.dsl.android

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinAndroidKsp)
}

android {
    namespace = "com.example.meshtasticapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.meshtasticapp"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("no.nordicsemi.android:ble:2.7.2")
    implementation("no.nordicsemi.android:ble-common:2.7.2")
    implementation("no.nordicsemi.android.support.v18:scanner:1.6.0")
    implementation("com.google.protobuf:protobuf-java:3.21.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.21.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.plrapps.mapcompose)
    implementation(libs.play.services.location)
	implementation(libs.androidx.runtime.livedata)
	implementation(libs.androidx.activity.ktx)
	testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.datastore.prefs)

    implementation(libs.androidx.hilt.nav)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.meshtastic.protobuf)

    implementation(libs.okhttp)
    implementation(libs.okio)

    implementation(libs.usb.serial)

    implementation(libs.nordicsemi.ble.scanner)
    implementation(libs.nordicsemi.ble.client)
}