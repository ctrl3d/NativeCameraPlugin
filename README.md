# NativeCameraPlugin

Unity Android 앱을 위한 CameraX 기반 네이티브 카메라 플러그인입니다.
Unity(C#) 측에서 JNI 및 Reflection을 통해 이 플러그인의 기능을 직접 제어할 수 있습니다.

## 주요 기능
- **프리뷰 및 캡처**: 백/프론트 카메라 프리뷰 지원 및 고해상도 사진 캡처
- **수동 카메라 제어**: ISO, 노출 시간(Exposure Time), 초점 거리(Focus Distance) 조절
- **오토 포커스(AF) 복귀**: 수동 초점 조절 후 다시 AF 모드로 전환
- **부가 기능**: 플래시(Torch) 켜기/끄기, 화면 줌(Zoom) 제어
- **기기 호환성 우회 지원**: 카메라가 1개인 특수 기기(산업용 태블릿 등)에서의 오류를 우회하는 옵션 제공

## 빌드 방법
프로젝트 루트에서 다음 명령어를 실행하여 AAR 파일을 생성합니다.
```bash
./gradlew :app:assembleRelease
```
빌드된 산출물은 `app/build/outputs/aar/NativeCameraPlugin-release.aar` 경로에 생성됩니다.

## Unity 연동 가이드
1. 빌드된 `NativeCameraPlugin-release.aar` 파일을 Unity 프로젝트의 `Assets/Plugins/Android/` 경로에 복사합니다.
2. Unity(C#) 스크립트에서 `AndroidJavaObject` 또는 JNI 리플렉션을 사용하여 `com.viv.nativecamera.NativeCameraPlugin` 클래스의 static 메서드를 호출합니다.

### 제공되는 핵심 API (Kotlin)
```kotlin
// 프리뷰 시작/종료
@JvmStatic fun startPreview(width: Int, height: Int, useFront: Boolean): Int
@JvmStatic fun stopPreview()
@JvmStatic fun updateTexture(): Boolean

// 수동 제어
@JvmStatic fun setIso(iso: Int)
@JvmStatic fun setExposureTimeNs(nanos: Long)
@JvmStatic fun setFocusDistance(diopter: Float)
@JvmStatic fun setAutoExposure(enabled: Boolean)
@JvmStatic fun setAutoFocus()
@JvmStatic fun setTorchEnabled(enabled: Boolean)
@JvmStatic fun setZoomRatio(ratio: Float)

// 사진 촬영 및 지원 정보
@JvmStatic fun takePhoto(savePath: String)
@JvmStatic fun getCapabilitiesJson(): String

// 설정
@JvmStatic fun setSingleCameraWorkaround(enable: Boolean)
@JvmStatic fun setCallbackObjectName(name: String)
```

## 사진 촬영 콜백 (UnitySendMessage)
`takePhoto(savePath)` 호출 시, 처리가 완료되면 Unity 측의 `NativeCameraCallback` 게임 오브젝트(기본값)로 성공 또는 실패 콜백이 전달됩니다.
- 성공 시: `OnPhotoSaved(String savePath)`
- 실패 시: `OnPhotoError(String errorMessage)`
