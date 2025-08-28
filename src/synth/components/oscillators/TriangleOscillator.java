package synth.components.oscillators;

public class TriangleOscillator extends Oscillator{

    public TriangleOscillator(double sampleRate){
        super(sampleRate);
        this.phaseIncrement = 0;
    }

    @Override
    protected double calculateAmplitude(){
        if(phase < 0.5){
            // Ascending phase: Ramp amplitude from -1.0 to 1.0
            return (phase * 4.0) - 1.0;
        }
        else {
            // Descending phase: Ramp amplitude from 1.0 to -1.0
            return ((1.0 - phase) * 4.0) - 1.0;
        }
    }

    @Override
    protected void advancePhase(){
        this.phase += phaseIncrement;
        if (phase >= 1.0) { phase -= 1.0; }
    }
}
