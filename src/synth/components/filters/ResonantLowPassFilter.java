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

    // LUT Indices
    private int cutoffIndex = 0;
    private int resonanceIndex = 0;

    /**
     * Constructs a ResonantLowPassFilter with a given sample rate.
     * @param sampleRate The sample rate of the audio system.
     */
    public ResonantLowPassFilter(double sampleRate){
        super(sampleRate);
        setParameters(1000, 1);
    }

    /**
     * Sets the cutoff frequency and resonance (Q) of the filter.
     * @param cutoffFrequency The cutoff frequency in Hz. Must be positive and below the Nyquist frequency.
     * @param resonanceQ The resonance factor (Q). Must be a positive value.
     */
    public void setParameters(double cutoffFrequency, double resonanceQ){
        if (cutoffFrequency <= 0 || cutoffFrequency >= sampleRate / 2) {
            throw new IllegalArgumentException("Cutoff frequency must be positive and below the Nyquist frequency.");
        }
        if (resonanceQ <= 0) {
            throw new IllegalArgumentException("Resonance (Q) must be positive.");
        }

        // Calculate the index for the cutoff & resonance
        this.cutoffIndex = (int) (LookupTables.TABLE_SIZE * cutoffFrequency / this.sampleRate);
        this.resonanceIndex = (int) (((resonanceQ - 1.0) / 19.0) * (LookupTables.RESONANCE_STEPS - 1));
    }

    /**
     * Processes a block of audio, applying the envelope to each sample.
     * @param inputBuffer The buffer containing the audio signal to be modulated.
     * @param outputBuffer The buffer where the modulated audio will be written.
     * @param blockSize The number of samples to process.
     */
    @Override
    public void processBlock(double[] inputBuffer, double[] outputBuffer, int blockSize) {
        // Fetch the coefficients ONCE from the giant lookup table
        double a1 = LookupTables.A1_TABLE[cutoffIndex][resonanceIndex];
        double a2 = LookupTables.A2_TABLE[cutoffIndex][resonanceIndex];
        double a3 = LookupTables.A3_TABLE[cutoffIndex][resonanceIndex];

        for (int i = 0; i < blockSize; i++) {
            // The TPT State-Variable Filter Algorithm (unchanged)
            double v3 = inputBuffer[i] - integrator2;
            double v1 = a1 * integrator1 + a2 * v3;
            double v2 = integrator2 + a2 * integrator1 + a3 * v3;

            integrator1 = 2 * v1 - integrator1;
            integrator2 = 2 * v2 - integrator2;

            outputBuffer[i] = v2;
        }
    }
}
