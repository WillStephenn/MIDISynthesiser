package synth.components.filters;

import synth.core.AudioComponent;

public abstract class Filter implements AudioComponent {
    protected final double sampleRate;

    public Filter(double sampleRate){
        this.sampleRate = sampleRate;
    }

    @Override
    public double processSample(double input) {
        return 0;
    }
}
