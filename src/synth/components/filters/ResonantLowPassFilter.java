package synth.components.filters;

public class ResonantLowPassFilter extends Filter{
    // This implementation of a Resonant Low Pass filter uses Vadim Zavalishin's
    // Topology Preserving Transform State Variable Filter (TPT SVF)

    // The filters internal memory variables
    private double integrator1;
    private double integrator2;

    // TPT Coefficients
    private double a1, a2, a3;

    public ResonantLowPassFilter(double sampleRate){
        super(sampleRate);
        this.integrator1 = 0.0;
        this.integrator2 = 0.0;
    }

    public void setParameters(double cutoffFrequency, double resonanceQ){
        double g = Math.tan(Math.PI * cutoffFrequency / this.sampleRate);
        double k = 1.0 / resonanceQ;

        // Pre-calculate TPT coefficients
        this.a1 = 1.0 / (1.0 + g * (g + k));
        this.a2 = g * a1;
        this.a3 = g * a2;
    }

    @Override
    public double processSample(double input){
        // --- The TPT State-Variable Filter Algorithm ---

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
