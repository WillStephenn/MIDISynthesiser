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

    // TPT Coefficients
    private double a1, a2, a3;

    /**
     * Constructs a ResonantLowPassFilter with a given sample rate.
     * @param sampleRate The sample rate of the audio system.
     */
    public ResonantLowPassFilter(double sampleRate){
        super(sampleRate);
        this.integrator1 = 0.0;
        this.integrator2 = 0.0;
        // Initialise with default parameters
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

        int index = (int) (LookupTables.TABLE_SIZE * cutoffFrequency/this.sampleRate);
        if(index >= LookupTables.TABLE_SIZE) {
            index = LookupTables.TABLE_SIZE -1;
        }

        double g = LookupTables.TAN_TABLE[index];
        double k = 1.0 / resonanceQ;

        // Pre-calculate TPT coefficients
        this.a1 = 1.0 / (1.0 + g * (g + k));
        this.a2 = g * a1;
        this.a3 = g * a2;
    }

    /**
     * Processes one sample of audio through the filter.
     * @param input The input sample.
     * @return The filtered (low-pass) sample.
     */
    @Override
    public double processSample(double input){
        // The TPT State-Variable Filter Algorithm

        // Calculate the intermediate values based on input and previous state
        double v3 = input - integrator2;
        double v1 = a1 * integrator1 + a2 * v3;
        double v2 = integrator2 + a2 * integrator1 + a3 * v3;

        // Update the state variables for the next sample tick.
        integrator1 = 2 * v1 - integrator1;
        integrator2 = 2 * v2 - integrator2;

        // The low-pass output is the final v2 value
        return v2;
    }


}
