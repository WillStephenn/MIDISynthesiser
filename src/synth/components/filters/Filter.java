package synth.components.filters;

import synth.core.AudioComponent;

/**
 * Represents an abstract filter that can process an audio signal.
 * This class provides the basic structure for different filter types.
 */
public abstract class Filter implements AudioComponent {
    protected final double sampleRate;

    /**
     * Constructs a Filter with a given sample rate.
     * @param sampleRate The sample rate of the audio system. Must be a positive value.
     */
    public Filter(double sampleRate){
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive.");
        }
        this.sampleRate = sampleRate;
    }

    /**
     * Processes one sample of audio through the filter.
     * @param input The input sample.
     * @return The filtered sample.
     */
    @Override
    public abstract double processSample(double input);
}
