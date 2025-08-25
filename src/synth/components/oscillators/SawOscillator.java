package synth.components.oscillators;

public class SawOscillator extends Oscillator{

    private double phaseIncrement;

    public SawOscillator(double sampleRate){
        super(sampleRate);
        this.phaseIncrement = 0;
    }

    public void setFrequency(double frequency){
        this.frequency = frequency;
        this.phaseIncrement = (frequency/sampleRate);
    }

    protected double calculateAmplitude(double input){
        // Map phase from range 0.0 -> 1.0 to -1.0 -> 1.0
        return (phase * 2.0) - 1.0;
    }

    protected void advancePhase(){
        this.phase += phaseIncrement;
        if (phase >= 1.0) { phase -= 1.0; }
    }
}
