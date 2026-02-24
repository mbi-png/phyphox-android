package de.rwth_aachen.phyphox

import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import android.graphics.SurfaceTexture

class FlashLightManager(context: Context) {

    private var camera: Camera? = null // For API 21/22
    private val cameraManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    } else null
    private var cameraId: String? = null

    init {
        try {
            cameraId = cameraManager?.cameraIdList?.getOrNull(0) // rear camera with flash is usually the first ID
        } catch (e: CameraAccessException){
            Log.e("FlashLightManager", "Could not access camera", e)
        }
    }

    fun toggleFlash(isEnabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Modern Way (API 23+)
            try {
                val cameraId = cameraManager?.cameraIdList?.getOrNull(0)
                cameraId?.let { cameraManager.setTorchMode(it, isEnabled) }
            } catch (e: Exception) { e.printStackTrace() }
        } else {
            // Legacy Way (API 21/22)
            handleLegacyFlash(isEnabled)
        }
    }

    // Before API 23, there was no setTorchMode method,
    // for older api, we actually have to "open" camera hardware and toggle the flash parameter manually.
    // this contains to setupPreviewTexture, so later the app can only allow to work it from Marshmallow.
    // this function is here, just for the reference.
    private fun handleLegacyFlash(isEnabled: Boolean) {
        try {
            if (isEnabled) {
                camera = Camera.open().apply {
                    val parameters = parameters
                    parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                    this.parameters = parameters
                    setPreviewTexture(SurfaceTexture(0))
                    startPreview()
                }
            } else {
                camera?.apply {
                    stopPreview()
                    release()
                }
                camera = null
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun turnOff() {
        toggleFlash(false)
    }
}
