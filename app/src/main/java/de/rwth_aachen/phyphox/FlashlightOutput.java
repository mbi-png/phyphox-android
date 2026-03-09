package de.rwth_aachen.phyphox;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import androidx.camera.core.CameraControl;

public class FlashlightOutput {
    private String parameter;
    private DataInput dataInput;
    private Integer intensity;
    private double frequency;
    private FlashLightManager flashLightManager;
    private CameraManager cameraManager;

    public FlashlightOutput(CameraManager cameraManager) {
        this.cameraManager = cameraManager;

    }

    // This is called later by the Activity/Experiment controller
    public void initHardware(CameraControl cameraControl) {
        this.flashLightManager = new FlashLightManager(cameraManager, cameraControl);
    }

    public void setParameter(String parameter, DataInput input) throws PhyphoxFile.phyphoxFileException {
        this.parameter = parameter;
        this.dataInput = input;

        final var inputValue = (input.isBuffer) ? input.buffer.value : input.value;

        switch (parameter){
            case "frequency": frequency = inputValue; break;
            case "intensity": intensity = (int) inputValue; break;
            default: throw new PhyphoxFile.phyphoxFileException("Unexpected flashlight input parameter.");
        }
    }

    public FlashLightManager getManager() {
        if(flashLightManager != null){
            return flashLightManager;
        }
        return null;
    }

    public void start() {
        if (flashLightManager != null) {
            if(intensity == 0) return;
            var turnOnFlashWithIntensity = intensity > 1;
            if (frequency > 0) {
                flashLightManager.startStrobe(frequency);
            } else {
                if(turnOnFlashWithIntensity){
                    flashLightManager.setIntensity(intensity);
                } else {
                    flashLightManager.performToggle(true);
                }
            }
        }
    }

    public void stop() {
        if (flashLightManager != null) {
            flashLightManager.turnOfFlashLight();
        }
    }

    public double getStrobeRate() { return frequency; }
}
