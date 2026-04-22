package work.ctrl3d.camerax

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.unity3d.player.UnityPlayer
import java.io.File

class CameraXSession(
    private val activity: Activity,
    private val bridge: UnityTextureBridge,
    private val useFront: Boolean
) {
    private var provider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var surface: Surface? = null

    var controls: ManualControls? = null
        private set

    @SuppressLint("RestrictedApi")
    fun start(width: Int, height: Int) {
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            try {
                provider = future.get()

                val targetSize = Size(width, height)

                val resolutionSelector = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            targetSize,
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build().apply {
                        setSurfaceProvider { request ->
                            surface?.release()
                            val newSurface = Surface(bridge.surfaceTexture)
                            surface = newSurface
                            request.provideSurface(newSurface,
                                { r -> r.run() }) { /* release는 stop()에서 관리 */ }
                        }
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setResolutionSelector(resolutionSelector)
                    .build()

                val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA
                               else CameraSelector.DEFAULT_BACK_CAMERA

                val p = provider ?: run {
                    android.util.Log.e("CameraXSession", "CameraProvider is null")
                    return@addListener
                }
                p.unbindAll()
                camera = p.bindToLifecycle(
                    activity as LifecycleOwner,
                    selector,
                    preview,
                    imageCapture
                )

                camera?.let { controls = ManualControls(it) }
            } catch (e: Exception) {
                android.util.Log.e("CameraXSession", "Camera init failed", e)
                UnityPlayer.UnitySendMessage(
                    NativeCameraPlugin.callbackObjectName,
                    "OnPhotoError",
                    "Camera init failed: ${e.message}"
                )
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    fun stop() {
        // 1. 카메라 파이프라인 먼저 정지 (프레임 전송 중단)
        try { provider?.unbindAll() } catch (_: Exception) {}
        provider = null
        camera = null

        // 2. Surface release (카메라가 정지된 후에 해야 BufferQueue abandoned 방지)
        try { surface?.release() } catch (_: Exception) {}
        surface = null

        // 3. SurfaceTexture / GL 리소스 해제
        bridge.release()
    }

    fun updateTexture(): Boolean = bridge.update()

    fun takePhoto(savePath: String) {
        val output = ImageCapture.OutputFileOptions
            .Builder(File(savePath)).build()

        imageCapture?.takePicture(
            output,
            ContextCompat.getMainExecutor(activity),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                    UnityPlayer.UnitySendMessage(
                        NativeCameraPlugin.callbackObjectName,
                        "OnPhotoSaved",
                        savePath
                    )
                }
                override fun onError(e: ImageCaptureException) {
                    UnityPlayer.UnitySendMessage(
                        NativeCameraPlugin.callbackObjectName,
                        "OnPhotoError",
                        e.message ?: "Unknown error"
                    )
                    e.printStackTrace()
                }
            })
    }
}
