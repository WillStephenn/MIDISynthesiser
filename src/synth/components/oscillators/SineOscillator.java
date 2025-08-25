package synth.components.oscillators;

public class SineOscillator extends Oscillator{

    private double angleDelta;

    public SineOscillator(double sampleRate){
        super(sampleRate);
        this.angleDelta = 0;
    }

    public void setFrequency(double frequency){
        this.frequency = frequency;
        this.angleDelta = (2 * Math.PI * frequency)/sampleRate;
    }

    protected double calculateAmplitude(double input){
        return Math.sin(phase);
    }

    protected void advancePhase(){
        phase += angleDelta;
        if (phase >= 2 * Math.PI) {
            phase -= 2 * Math.PI;
        }
    }
}
