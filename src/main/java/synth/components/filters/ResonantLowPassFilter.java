package synth.components.filters;

import synth.utils.LookupTables;

/**
 * Implements a resonant low-pass filter using the Topology-Preserving Transform (TPT)
 * State-Variable Filter (SVF) design by Vadim Zavalishin.
 */
public class ResonantLowPassFilter extends Filter{

    // Filters internal memory variables
    private double integrator1;
    private double integrator2;

    // Constant Pre-Computed Constants
    private final double cutoffScalar;
    private final double resonanceScalar;
    private final double nyquistLimit;

    // Cached coefficients
    private double a1, a2, a3;
    private int prevCutoffIndex = -1;
    private int prevResonanceIndex = -1;

    /**
     * Constructs a ResonantLowPassFilter with a given sample rate.
     * @param sampleRate The sample rate of the audio system.
     */
    public ResonantLowPassFilter(double sampleRate){
        super(sampleRate);
        this.cutoffScalar = LookupTables.TABLE_SIZE / this.sampleRate;
        this.resonanceScalar = (LookupTables.RESONANCE_STEPS - 1) / 19.0; // Resonance ranges from 1 to 20
        this.nyquistLimit = (sampleRate / 2.0) - 1.0;
        setParameters(1000, 1);
    }

    /**
     * Sets the cutoff frequency and resonance (Q) of the filter.
     * This method checks if the parameters have changed enough to warrant
     * fetching new coefficients from the lookup tables.
     * @param cutoffFrequency The cutoff frequency in Hz. Must be positive and below the Nyquist frequency.
     * @param resonanceQ The resonance factor (Q). Must be a positive value.
     */
    public void setParameters(double cutoffFrequency, double resonanceQ){
        if (cutoffFrequency <= 0 || cutoffFrequency >= this.nyquistLimit) {
            throw new IllegalArgumentException("Cutoff frequency must be positive and below the Nyquist frequency.");
        }
        if (resonanceQ <= 0) {
            throw new IllegalArgumentException("Resonance (Q) must be positive.");
        }

        // Calculate the index for the cutoff & resonance
        int targetCutoffIndex = (int) (cutoffFrequency * this.cutoffScalar);
        int targetResonanceIndex = (int) ((resonanceQ - 1.0) * this.resonanceScalar);


        // Compare to cached values and fetch new coefficients from the LUTs
        if (targetCutoffIndex != this.prevCutoffIndex || targetResonanceIndex != this.prevResonanceIndex){
            this.prevCutoffIndex = targetCutoffIndex;
            this.prevResonanceIndex = targetResonanceIndex;
            this.a1 = LookupTables.A1_TABLE[targetCutoffIndex][targetResonanceIndex];
            this.a2 = LookupTables.A2_TABLE[targetCutoffIndex][targetResonanceIndex];
            this.a3 = LookupTables.A3_TABLE[targetCutoffIndex][targetResonanceIndex];
        }
    }

    /**
     * Processes a block of audio, applying the envelope to each sample.
     * @param inputBuffer The buffer containing the audio signal to be modulated.
     * @param outputBuffer The buffer where the modulated audio will be written.
     * @param blockSize The number of samples to process.
     */
    @Override
    public void processBlock(double[] inputBuffer, double[] outputBuffer, int blockSize) {
        for (int i = 0; i < blockSize; i++) {
            // The TPT State-Variable Filter Algorithm (unchanged)
            double v3 = inputBuffer[i] - integrator2;
            double v1 = this.a1 * integrator1 + this.a2 * v3;
            double v2 = integrator2 + this.a2 * integrator1 + this.a3 * v3;

            integrator1 = 2 * v1 - integrator1;
            integrator2 = 2 * v2 - integrator2;

            outputBuffer[i] = v2;
        }
    }
}
