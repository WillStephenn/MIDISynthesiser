package synth.components.oscillators;

import synth.utils.LookupTables;

public class TriangleOscillator extends Oscillator{

    public TriangleOscillator(double sampleRate){
        super(sampleRate);
        this.phaseIncrement = 0;
    }

    @Override
    protected double calculateAmplitude(){
        int index = (int) phase;
        return LookupTables.TRIANGLE[index];
    }
}
