package synth.components.oscillators;

import synth.core.AudioComponent;
import synth.utils.LookupTables;

/**
 * Represents an oscillator, which generates a periodic waveform at a specified frequency.
 * This is an abstract class that provides the basic structure for different oscillator types.
 */
public abstract class Oscillator implements AudioComponent {

    // Instance Variables
    protected final double sampleRate;
    protected double phase;
    protected double frequency;
    protected double phaseIncrement;

    /**
     * Constructs an Oscillator with a given sample rate.
     * @param sampleRate The sample rate of the audio system. Must be a positive value.
     */
    public Oscillator(double sampleRate){
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be positive.");
        }
        this.sampleRate = sampleRate;
        this.phase = 0.0;
    }

    // Methods
    /**
     * Calculates the current amplitude of the waveform based on the current phase.
     * @return The calculated amplitude.
     */
    protected abstract double calculateAmplitude();

    /**
     * Advances the phase of the oscillator for the next sample.
     */
    protected void advancePhase(){
        this.phase += phaseIncrement;

        // Wrap phase increment
        if (this.phase >= LookupTables.TABLE_SIZE) {
            this.phase -= LookupTables.TABLE_SIZE;
        }
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
        this.phaseIncrement = (LookupTables.TABLE_SIZE * frequency) / sampleRate; // Default Phase Increment Equation, override for non-linear oscillators
    }

    /**
     * Processes one sample of audio.
     * It calculates the current amplitude and advances the oscillator's phase.
     * @param input The input sample, ignored by oscillators. Here for interface consistency.
     * @return The generated sample.
     */
    @Override
    public double processSample(double input){
        double currentAmplitude = calculateAmplitude();
        advancePhase();
        return currentAmplitude;
    }
}
