package synth.components.oscillators;

import synth.core.AudioComponent;
import synth.core.AudioConstants;

public abstract class Oscillator implements AudioComponent {

    // Instance Variables
    protected final double sampleRate;
    protected double phase;
    protected double frequency;

    // Constructor
    public Oscillator(){
        this.sampleRate = AudioConstants.SAMPLE_RATE;
        this.phase = 0.0;
    }

    // Methods
    protected abstract double calculateWaveform(double input);

    protected abstract double advancePhase();

    public void setFrequency(double frequency){
        this.frequency = frequency;
    }

    public double processSample(double input){
        double currentAmplitude = calculateWaveform(input);
        advancePhase();
        return currentAmplitude;
    }
}
