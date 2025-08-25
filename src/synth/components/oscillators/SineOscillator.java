package synth.components.oscillators;

public class SineOscillator extends Oscillator{

    public SineOscillator(double sampleRate){
        super(sampleRate);
    }

    @Override
    // In the case of the sine oscillator, the phase increment is an angle in rads
    public void setFrequency(double frequency){
        this.frequency = frequency;
        this.phaseIncrement = (2 * Math.PI * frequency)/sampleRate;
    }

    protected double calculateAmplitude(double input){
        return Math.sin(phase);
    }

    protected void advancePhase(){
        phase += phaseIncrement;
        if (phase >= 2 * Math.PI) {
            phase -= 2 * Math.PI;
        }
    }
}
