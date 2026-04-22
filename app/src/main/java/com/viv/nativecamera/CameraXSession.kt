package com.viv.nativecamera

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Size
import android.view.Surface
import androidx.camera.core.*
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

    var controls: ManualControls? = null
        private set

    @SuppressLint("RestrictedApi")
    fun start(width: Int, height: Int) {
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            provider = future.get()

            val targetSize = Size(width, height)

            preview = Preview.Builder()
                .setTargetResolution(targetSize)
                .build().apply {
                    setSurfaceProvider { request ->
                        val surface = Surface(bridge.surfaceTexture)
                        request.provideSurface(surface,
                            { r -> r.run() }) { surface.release() }
                    }
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetResolution(targetSize)
                .build()

            val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA
                           else CameraSelector.DEFAULT_BACK_CAMERA

            provider!!.unbindAll()
            camera = provider!!.bindToLifecycle(
                activity as LifecycleOwner,
                selector,
                preview,
                imageCapture
            )

            controls = ManualControls(camera!!)
        }, ContextCompat.getMainExecutor(activity))
    }

    fun stop() {
        provider?.unbindAll()
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
