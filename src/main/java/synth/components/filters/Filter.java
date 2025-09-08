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
     * Processes a block of audio, applying the envelope to each sample.
     * @param inputBuffer The buffer containing the audio signal to be modulated.
     * @param outputBuffer The buffer where the modulated audio will be written.
     * @param blockSize The number of samples to process.
     */
    @Override
    public abstract void processBlock(double[] inputBuffer, double[] outputBuffer, int blockSize);
}
