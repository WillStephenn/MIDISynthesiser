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
            outputBuffer[i] = LookupTables.SINE[index];
            advancePhase();
        }
    }

}
