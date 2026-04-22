package work.ctrl3d.camerax

import android.app.Activity
import androidx.annotation.Keep
import com.unity3d.player.UnityPlayer

@Keep
object NativeCameraPlugin {

    private var session: CameraXSession? = null
    private val configured = java.util.concurrent.atomic.AtomicBoolean(false)

    private var useSingleCameraWorkaround = false
    internal var callbackObjectName: String = "NativeCameraCallback"
        private set

    @JvmStatic @Keep
    fun setSingleCameraWorkaround(enable: Boolean) {
        useSingleCameraWorkaround = enable
    }

    @JvmStatic @Keep
    fun setCallbackObjectName(name: String) {
        callbackObjectName = name
    }

    @androidx.annotation.OptIn(
        androidx.camera.lifecycle.ExperimentalCameraProviderConfiguration::class
    )
    private fun ensureConfigured() {
        if (!configured.compareAndSet(false, true)) return

        val config = androidx.camera.core.CameraXConfig.Builder
            .fromConfig(androidx.camera.camera2.Camera2Config.defaultConfig())
            .setCameraExecutor(java.util.concurrent.Executors.newSingleThreadExecutor())
            .setMinimumLoggingLevel(android.util.Log.WARN)
            .build()

        try {
            androidx.camera.lifecycle.ProcessCameraProvider.configureInstance(config)
        } catch (e: IllegalStateException) {
            android.util.Log.w("NativeCameraPlugin",
                "CameraX already configured: ${e.message}")
        }
    }

    /** Unity가 호출. 프리뷰 시작. 반환: OpenGL 텍스처 ID (0이면 실패) */
    @JvmStatic
    @Keep
    fun startPreview(width: Int, height: Int, useFrontCamera: Boolean): Int {
        if (session != null) stopPreview()
        val activity: Activity = UnityPlayer.currentActivity
        val bridge = UnityTextureBridge(width, height)
        val textureId = bridge.createExternalTexture()

        activity.runOnUiThread {
            ensureConfigured()
            session = CameraXSession(activity, bridge, useFrontCamera).also {
                it.start(width, height)
            }
        }
        return textureId
    }

    @JvmStatic @Keep
    fun stopPreview() {
        UnityPlayer.currentActivity.runOnUiThread {
            session?.stop()
            session = null
        }
    }

    /** 매 프레임 Unity가 호출. 새 프레임이 있으면 true */
    @JvmStatic @Keep
    fun updateTexture(): Boolean = session?.updateTexture() ?: false

    // ─── 수동 제어 API ───────────────────────────────────────

    @JvmStatic @Keep
    fun setIso(iso: Int) {
        session?.controls?.let { it.setIso(iso) }
    }

    @JvmStatic @Keep
    fun setExposureTimeNs(nanos: Long) {
        session?.controls?.let { it.setExposureTime(nanos) }
    }

    @JvmStatic @Keep
    fun setFocusDistance(diopter: Float) {
        session?.controls?.let { it.setFocusDistance(diopter) }
    }

    @JvmStatic @Keep
    fun setAutoExposure(enabled: Boolean) {
        session?.controls?.let { it.setAutoExposure(enabled) }
    }

    @JvmStatic @Keep
    fun setAutoFocus() {
        session?.controls?.let { it.setAutoFocus() }
    }

    @JvmStatic @Keep
    fun setTorchEnabled(enabled: Boolean) {
        session?.controls?.let { it.setTorchEnabled(enabled) }
    }

    @JvmStatic @Keep
    fun setZoomRatio(ratio: Float) {
        session?.controls?.let { it.setZoomRatio(ratio) }
    }

    @JvmStatic @Keep
    fun takePhoto(savePath: String) {
        session?.takePhoto(savePath)
    }

    /** 디바이스 지원 범위 JSON 반환 */
    @JvmStatic @Keep
    fun getCapabilitiesJson(): String = session?.controls?.capabilitiesJson() ?: "{}"
}
