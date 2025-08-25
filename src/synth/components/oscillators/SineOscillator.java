package synth.components.oscillators;

import synth.core.AudioConstants;

public class SineOscillator extends Oscillator{

    private double angleDelta;

    public SineOscillator(){
        this.phase = 0;
        this.angleDelta = 0;
    }

    protected abstract double calculateWaveform(double input){

    }

    protected abstract double advancePhase(){

    }

    @Override
    public void setFrequency(double frequency){

        this.frequency = frequency;
        this.angleDelta = (2 * Math.PI * frequency)/AudioConstants.SAMPLE_RATE;
    }
}
