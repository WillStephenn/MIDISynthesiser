package synth.components.oscillators;

import synth.utils.LookupTables;

public class SquareOscillator extends Oscillator{

    public SquareOscillator(double sampleRate){
        super(sampleRate);
        this.phaseIncrement = 0;
    }

    @Override
    protected double calculateAmplitude() {
        int index = (int) phase;
        return LookupTables.SQUARE[index];
    }
}
