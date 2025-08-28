package synth.components.oscillators;

public class SineOscillator extends Oscillator{

    public SineOscillator(double sampleRate){
        super(sampleRate);
    }

    @Override
    // In the case of the sine oscillator, the phase increment is an angle in rads
    public void setFrequency(double frequency){
        if (frequency < 0) {
            throw new IllegalArgumentException("Frequency cannot be negative.");
        }
        this.frequency = frequency;
        this.phaseIncrement = (2 * Math.PI * frequency)/sampleRate;
    }

    @Override
    protected double calculateAmplitude(){
        return Math.sin(phase);
    }

    @Override
    protected void advancePhase(){
        phase += phaseIncrement;
        if (phase >= 2 * Math.PI) {
            phase -= 2 * Math.PI;
        }
    }
}
