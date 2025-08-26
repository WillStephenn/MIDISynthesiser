package synth.components.oscillators;

import synth.core.AudioComponent;

public abstract class Oscillator implements AudioComponent {

    // Instance Variables
    protected final double sampleRate;
    protected double phase;
    protected double frequency;
    protected double phaseIncrement;

    // Constructor
    public Oscillator(double sampleRate){
        this.sampleRate = sampleRate;
        this.phase = 0.0;
    }

    // Methods
    protected abstract double calculateAmplitude(double input);

    protected abstract void advancePhase();

    public void setFrequency(double frequency){
        this.frequency = frequency;
        this.phaseIncrement = (frequency/sampleRate); // Default Phase Increment Equation, override for non-linear oscillators
    }

    @Override
    public double processSample(double input){
        double currentAmplitude = calculateAmplitude(input);
        advancePhase();
        return currentAmplitude;
    }
}
