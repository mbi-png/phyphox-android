package de.rwth_aachen.phyphox;

import android.content.Context;

import androidx.camera.core.CameraControl;

public class FlashlightOutput {
    private final int intensity;
    private final int strobeRate;
    private FlashLightManager flashLightManager;

    public FlashlightOutput(int intensity, int strobeRate) {
        this.intensity = intensity;
        this.strobeRate = strobeRate;
    }

    // This is called later by the Activity/Experiment controller
    public void initHardware(Context context, CameraControl cameraControl) {
        this.flashLightManager = new FlashLightManager(context, cameraControl);
    }

    public FlashLightManager getManager() {
        if(flashLightManager != null){
            return flashLightManager;
        }
        return null;
    }

    public void start() {
        if (flashLightManager != null) {
            if (strobeRate > 0) {
                flashLightManager.startStrobe(strobeRate);
            } else {
                flashLightManager.performToggle(true);
            }
        }
    }

    public void stop() {
        if (flashLightManager != null) {
            flashLightManager.turnOfFlashLight();
        }
    }

    public int getStrobeRate() { return strobeRate; }
}
