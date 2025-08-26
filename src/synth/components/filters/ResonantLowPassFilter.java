package synth.components.filters;

public class ResonantLowPassFilter extends Filter{
    // This implementation of a Resonant Low Pass filter uses a State Variable Filter

    // The filters internal memory variables
    private double integrator1;
    private double integrator2;

    // Filter parameters
    private double cutoffCoefficient;
    private double dampingFactor;

    public ResonantLowPassFilter(double sampleRate){
        super(sampleRate);
    }


}
