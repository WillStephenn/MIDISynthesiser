package synth.components.oscillators;
import synth.utils.LookupTables;

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
        this.phaseIncrement = (LookupTables.TABLE_SIZE * frequency) * this.sampleRateReciprocal;
    }

    @Override
    protected double calculateAmplitude(){
        int index = (int)phase;
        return LookupTables.SINE[index];
    }

}
