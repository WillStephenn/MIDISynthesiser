package synth.components.oscillators;

public class SquareOscillator extends Oscillator{

    public SquareOscillator(double sampleRate){
        super(sampleRate);
        this.phaseIncrement = 0;
    }

    @Override
    protected double calculateAmplitude() {
        if(phase < 0.5){
            return -1;
        }
        else {
            return 1;
        }
    }
}
