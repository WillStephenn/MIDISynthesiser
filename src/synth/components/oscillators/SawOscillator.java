package synth.components.oscillators;

public class SawOscillator extends Oscillator{

    public SawOscillator(double sampleRate){
        super(sampleRate);
        this.phaseIncrement = 0;
    }

    @Override
    protected double calculateAmplitude(){
        // Map phase from range 0.0 -> 1.0 to -1.0 -> 1.0
        return (phase * 2.0) - 1.0;
    }

    @Override
    protected void advancePhase(){
        this.phase += phaseIncrement;
        if (phase >= 1.0) { phase -= 1.0; }
    }
}
