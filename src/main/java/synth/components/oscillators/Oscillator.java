package synth.components.oscillators;

import synth.core.AudioComponent;
import synth.utils.LookupTables;

/**
 * Represents an oscillator, which generates a periodic waveform at a specified frequency.
 * This is an abstract class that provides the basic structure for different oscillator types.
 */
public abstract class Oscillator implements AudioComponent {

    // Instance Variables
    protected double phase;
    protected double frequency;
    protected double phaseIncrement;

    // Pre Computed Constant
    protected final double sampleRateReciprocal;
    protected static final int phaseMask = LookupTables.TABLE_SIZE - 1;

    /**
     * Constructs an Oscillator with a given sample rate.
     * @param sampleRate The sample rate of the audio system. Must be a positive value.
     */
    public Oscillator(double sampleRate){
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive.");
        }
        this.sampleRateReciprocal = 1.0/ sampleRate;
        this.phase = 0.0;
    }

    // Methods

    /**
     * Advances the phase of the oscillator for the next sample.
     */
    protected void advancePhase(){
        this.phase += phaseIncrement;
    }
    /**
     * Sets the frequency of the oscillator.
     * @param frequency The frequency in Hz. Must not be negative.
     */
    public void setFrequency(double frequency){
        if (frequency < 0) {
            throw new IllegalArgumentException("Frequency cannot be negative.");
        }
        this.frequency = frequency;
        this.phaseIncrement = (LookupTables.TABLE_SIZE * frequency) * this.sampleRateReciprocal; // Default Phase Increment Equation, override for non-linear oscillators
    }

    /**
     * Fills the output buffer with a block of generated samples.
     * The input buffer is ignored as oscillators are sound generators.
     *
     * @param inputBuffer The input buffer (ignored in this case).
     * @param outputBuffer The buffer to fill with the oscillator's waveform.
     * @param blockSize The number of samples to generate.
     */
    @Override
    public abstract void processBlock(double[] inputBuffer, double[] outputBuffer, int blockSize);
}
