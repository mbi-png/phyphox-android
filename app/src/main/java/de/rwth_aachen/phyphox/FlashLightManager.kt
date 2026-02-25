package de.rwth_aachen.phyphox

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraControl

class FlashLightManager(context: Context, private var cameraControl: CameraControl? = null) {

    private var camera: Camera? = null // For API 21/22

    private val cameraManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    } else null
    private val cameraId: String? = try { cameraManager?.cameraIdList?.getOrNull(0) } catch (e: Exception) { null }

    private val handler = Handler(Looper.getMainLooper())
    private var isFlashOn = false
    private var isStrobeRunning = false
    private var currentStrobeRate: Int = 0
    private var currentIntensity: Int = 1

    // Get the maximum strength level supported by the device
    private val maxIntensityLevel: Int by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && cameraId != null) {
            val chars = cameraManager?.getCameraCharacteristics(cameraId)
            chars?.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        } else {
            1
        }
    }

    fun performToggle(enabled: Boolean) {
        if (cameraControl != null) {
            cameraControl?.enableTorch(enabled)
        } else {
            toggleFlash(enabled)
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

    private val strobeRunnable = object : Runnable {
        override fun run() {
            if (!isStrobeRunning) return

            isFlashOn = !isFlashOn

            performToggle(isFlashOn)

            val delay = if (currentStrobeRate > 0) (1000 / (currentStrobeRate * 2)).toLong() else 100L

            handler.postDelayed(this, delay)
        }
    }

    fun startStrobe(rateHz: Int) {
        if (rateHz <= 0) {
            stopStrobe()
            return
        }
        this.currentStrobeRate = rateHz
        if (!isStrobeRunning) {
            isStrobeRunning = true
            handler.post(strobeRunnable)
        }
    }

    fun stopStrobe() {
        isStrobeRunning = false
        handler.removeCallbacks(strobeRunnable)
        performToggle(false)
        isFlashOn = false
    }

    fun setIntensity(level: Int) {
        Log.d("FlashLight", "maxintensity" +maxIntensityLevel)
        val id = cameraId ?: return

        // when camera in use, cannot person intensity control, not available yet
        if(cameraControl != null){ return }

        this.currentIntensity = level


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxIntensityLevel > 1) {
            // Ensure currentIntensity is within 1 to maxIntensityLevel
            val level = currentIntensity.coerceIn(1, maxIntensityLevel)
            cameraManager?.turnOnTorchWithStrengthLevel(id, level)
        } else {
            // controlling intensity not supported for lower version than Android 33
        }
    }

    fun turnOfFlashLight(){
        performToggle(false)
    }
}
