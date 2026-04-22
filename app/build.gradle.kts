plugins {
    id("com.android.library")
}

base {
    archivesName.set("NativeCameraPlugin")
}

android {
    namespace = "work.ctrl3d.camerax"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}


dependencies {
    val cameraX = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraX")
    implementation("androidx.camera:camera-camera2:$cameraX")
    implementation("androidx.camera:camera-lifecycle:$cameraX")
    implementation("androidx.camera:camera-view:$cameraX")
    implementation("androidx.camera:camera-extensions:$cameraX")

    // Unity가 제공 — 빌드 시엔 필요, aar 배포 시 제외
    compileOnly(project(":unity-stub"))
}
