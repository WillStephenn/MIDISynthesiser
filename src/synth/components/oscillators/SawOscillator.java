package synth.components.oscillators;

import synth.utils.LookupTables;

public class SawOscillator extends Oscillator{

    public SawOscillator(double sampleRate){
        super(sampleRate);
        this.phaseIncrement = 0;
    }

    @Override
    protected double calculateAmplitude(){
        int index = (int)phase;
        return LookupTables.SAW[index];
    }
}
