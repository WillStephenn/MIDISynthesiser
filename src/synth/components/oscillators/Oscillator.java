package synth.components.oscillators;

import synth.core.AudioComponent;

public abstract class Oscillator implements AudioComponent {

    // Instance Variables
    protected final double sampleRate;
    protected double phase;
    protected double frequency;

    // Constructor
    public Oscillator(double sampleRate){
        this.sampleRate = sampleRate;
        this.phase = 0.0;
    }

    // Methods
    public abstract void setFrequency(double frequency);

    protected abstract double calculateAmplitude(double input);

    protected abstract void advancePhase();

    public double processSample(double input){
        double currentAmplitude = calculateAmplitude(input);
        advancePhase();
        return currentAmplitude;
    }
}
