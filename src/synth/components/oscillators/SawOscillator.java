package synth.components.oscillators;

import synth.utils.LookupTables;

public class SawOscillator extends Oscillator{

    public SawOscillator(double sampleRate){
        super(sampleRate);
        this.phaseIncrement = 0;
    }

    /**
     * Fills the output buffer with a block of generated samples.
     * The input buffer is ignored as oscillators are sound generators.
     *
     * @param inputBuffer The input buffer (ignored in this case).
     * @param outputBuffer The buffer to fill with the oscillator's waveform.
     * @param blockSize The number of samples to generate.
     */
    @Override
    public void processBlock(double[] inputBuffer, double[] outputBuffer, int blockSize){
        for (int i = 0; i < blockSize; i++){
            int index = (int)this.phase & phaseMask; // The phaseMask wraps the phase increment to prevent overflow
            outputBuffer[i] = LookupTables.SAW[index];
            advancePhase();
        }
    }
}
