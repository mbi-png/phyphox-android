package de.rwth_aachen.phyphox;

import android.content.Context;

public class FlashlightOutput {
    private final int intensity;
    private final int strobeRate;
    private FlashLightManager flashLightManager;

    public FlashlightOutput(int intensity, int strobeRate) {
        this.intensity = intensity;
        this.strobeRate = strobeRate;
    }

    // This is called later by the Activity/Experiment controller
    public void initHardware(Context context) {
        this.flashLightManager = new FlashLightManager(context);
    }

    public FlashLightManager getManager() {
        if(flashLightManager != null){
            return flashLightManager;
        }
        return null;
    }

    public int getStrobeRate() { return strobeRate; }
}
