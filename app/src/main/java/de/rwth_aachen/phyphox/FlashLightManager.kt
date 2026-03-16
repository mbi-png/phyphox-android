package de.rwth_aachen.phyphox

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraControl
import kotlin.math.max
import kotlin.math.min

class FlashLightManager(private var cameraManager: CameraManager?, private var cameraControl: CameraControl? = null) {

    private var camera: Camera? = null // For API 21/22
    private val cameraId: String? = try { cameraManager?.cameraIdList?.getOrNull(0) } catch (e: Exception) { null }
    private val handler = Handler(Looper.getMainLooper())
    private var isFlashOn = false
    private var isStrobeRunning = false
    private var currentStrobeRate: Double = 0.0
    private var lastStrobeRate : Double = 0.0
    private var currentIntensity: Int = 1
    private var maxStrobeRate : Double = 30.0 //Normally after 30 strobe rate there is not visible strobe as blinking becomes fast. But the number depends upon devices

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
                cameraId?.let { cameraManager?.setTorchMode(it, isEnabled) }
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

    fun startStrobe(rateHz: Double) {
        this.currentStrobeRate = rateHz.coerceIn(0.1, maxStrobeRate)
        if (!isStrobeRunning) {
            isStrobeRunning = true
            lastStrobeRate = this.currentStrobeRate
            handler.post(strobeRunnable)
        } else {
            if((currentStrobeRate - lastStrobeRate) >= 0.1){
                handler.removeCallbacks(strobeRunnable)
                lastStrobeRate = this.currentStrobeRate
                handler.post(strobeRunnable)
            }

        }
    }

    fun stopStrobe() {
        isStrobeRunning = false
        handler.removeCallbacks(strobeRunnable)
    }

    fun setIntensity(level: Int) {
        val id = cameraId ?: return

        // when camera in use, person cannot control intensity
        if(cameraControl != null){ return }

        this.currentIntensity = level.coerceIn(1, maxIntensityLevel)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && maxIntensityLevel > 1) {
            cameraManager?.turnOnTorchWithStrengthLevel(id, this.currentIntensity)
        } else {
            performToggle(true)
        }
    }

    fun turnOfFlashLight(){
        performToggle(false)
    }

}
