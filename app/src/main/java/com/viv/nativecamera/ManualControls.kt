package com.viv.nativecamera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import org.json.JSONArray
import org.json.JSONObject

@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
class ManualControls(private val camera: Camera) {

    private val controlX = Camera2CameraControl.from(camera.cameraControl)
    private val info = Camera2CameraInfo.from(camera.cameraInfo)

    private var autoExposure = true
    private var currentIso: Int? = null
    private var currentShutter: Long? = null
    private var currentFocus: Float? = null

    fun setAutoExposure(enabled: Boolean) {
        autoExposure = enabled
        apply()
    }

    fun setIso(iso: Int) {
        autoExposure = false
        currentIso = iso
        apply()
    }

    fun setExposureTime(nanos: Long) {
        autoExposure = false
        currentShutter = nanos
        apply()
    }

    fun setFocusDistance(diopter: Float) {
        currentFocus = diopter
        apply()
    }

    fun setAutoFocus() {
        currentFocus = null
        apply()
    }

    fun setTorchEnabled(enabled: Boolean) {
        camera.cameraControl.enableTorch(enabled)
    }

    fun setZoomRatio(ratio: Float) {
        camera.cameraControl.setZoomRatio(ratio)
    }

    private fun apply() {
        val builder = CaptureRequestOptions.Builder()
        if (autoExposure) {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON)
        } else {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF)
            currentIso?.let {
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, it)
            }
            currentShutter?.let {
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, it)
            }
        }
        currentFocus?.let {
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF)
            builder.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, it)
        }
        controlX.captureRequestOptions = builder.build()
    }

    fun capabilitiesJson(): String {
        val isoRange = info.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val expRange = info.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val minFocus = info.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val hwLevel = info.getCameraCharacteristic(
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val map = info.getCameraCharacteristic(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val jpegSizes = map?.getOutputSizes(android.graphics.ImageFormat.JPEG) ?: emptyArray()

        return JSONObject().apply {
            put("isoMin", isoRange?.lower ?: 0)
            put("isoMax", isoRange?.upper ?: 0)
            put("exposureMinNs", expRange?.lower ?: 0)
            put("exposureMaxNs", expRange?.upper ?: 0)
            put("minFocusDiopter", minFocus ?: 0f)
            put("hardwareLevel", hwLevel ?: -1)

            val resolutions = JSONArray()
            jpegSizes.forEach { size ->
                resolutions.put(JSONObject().apply {
                    put("width", size.width)
                    put("height", size.height)
                })
            }
            put("resolutions", resolutions)
        }.toString()
    }
}
