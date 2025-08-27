package synth.core;

import synth.components.Envelope;
import synth.components.filters.ResonantLowPassFilter;
import synth.components.oscillators.Oscillator;
import synth.components.oscillators.SawOscillator;
import synth.components.oscillators.SineOscillator;
import synth.components.oscillators.TriangleOscillator;

public class Voice implements AudioComponent{
    // The voice object encapsulates all of the audio components into one:
    // Oscillator -> Amp Envelope -> Resonant Low Pass Filter
    // It provides setters for all encapsulated elements, acting as a facade.

    // Audio Component Objects
    private final Oscillator oscillator;
    private final ResonantLowPassFilter filter;
    private final Envelope ampEnvelope;
    private final Envelope filterEnvelope;

    // Oscillator Settings
    private byte pitchMIDI;
    private double pitchFrequency;

    // Filter settings
    private double filterCutoff;
    private double filterResonance;
    private double filterModAmount;
    private final double controlRate;
    private double controlRateCounter;

    // Gain Staging
    private double velocityMult;
    private double preFilterMult;
    private double postFilterMult;

    // LFO
    private final Oscillator LFO;
    private double LFOFreq;

    // Panning
    private double panPosition;
    private double panDepth;

    //Constructor
    public Voice (Synthesiser.Waveform waveform, double pitchFrequency, double sampleRate, Synthesiser.Waveform LFO, double controlRate){
        // Audio Components
        this.filter = new ResonantLowPassFilter(sampleRate);
        this.ampEnvelope = new Envelope(sampleRate);
        this.filterEnvelope = new Envelope(sampleRate);

        // Filter Defaults
        this.filterCutoff = 20000;
        this.filterResonance = 0.01;
        this.filterModAmount = 2000;
        this.filter.setParameters(this.filterCutoff, this.filterResonance);
        this.controlRate = controlRate;
        this.controlRateCounter = 0;

        // Synth Oscillator switch
        switch (waveform){
            case SINE:
                this.oscillator = new SineOscillator(sampleRate);
                break;
            case SAW:
                this.oscillator = new SawOscillator(sampleRate);
                break;
            case TRIANGLE:
                this.oscillator = new TriangleOscillator(sampleRate);
                break;
            default:
                throw new IllegalArgumentException("Unsupported waveform: " + waveform);
        }

        // LFO Oscillator switch
        switch (LFO){
            case SINE:
                this.LFO = new SineOscillator(sampleRate);
                break;
            case SAW:
                this.LFO = new SawOscillator(sampleRate);
                break;
            case TRIANGLE:
                this.LFO = new TriangleOscillator(sampleRate);
                break;
            default:
                throw new IllegalArgumentException("Unsupported waveform: " + waveform);
        }
        // LFO
        this.LFO.setFrequency(0.5);

        // Set Oscillator starting pitch
        this.pitchFrequency = pitchFrequency;
        this.oscillator.setFrequency(this.pitchFrequency);

        // Set Default Velocity
        this.velocityMult = 1.0;


    }

    // Facade Setter Methods
    // Oscillators:
    public void setOscillatorPitch(byte pitchMIDI){
        this.pitchMIDI = pitchMIDI;
        this.oscillator.setFrequency(440.0 * Math.pow(2.0, (pitchMIDI - 69) / 12.0));
    }

    public byte getPitchMIDI(){
        return this.pitchMIDI;
    }

    public double getOscillatorFrequency(){
        return this.pitchFrequency;
    }

    // LFO:
    public void setLFOFreq(double frequency){
        this.LFOFreq = frequency;
    }

    // Panning:
    public void setPannDepth(double panDepth){
        if(panDepth >= 1 || panDepth <= -1){this.panDepth = 1;} // ill think of a nicer way to this later
        else {this.panDepth = panDepth;}
    }

    // Amp Envelope:
    public void setAmpEnvelopeAttackTime(double seconds){
        this.ampEnvelope.setAttackTime(seconds);
    }
    public void setAmpEnvelopeDecayTime(double seconds){
        this.ampEnvelope.setDecayTime(seconds);
    }
    public void setAmpEnvelopeSustainLevel(double level){
        this.ampEnvelope.setSustainLevel(level);
    }
    public void setAmpEnvelopeReleaseTime(double seconds){
        this.ampEnvelope.setReleaseTime(seconds);
    }
    public void setAmpEnvelope(double attackTime, double decayTime, double sustainLevel, double releaseTime){
        this.ampEnvelope.setEnvelope(attackTime, decayTime, sustainLevel, releaseTime);
    }

    // Filter Envelope:
    public void setFilterEnvelopeAttackTime(double seconds){
        this.filterEnvelope.setAttackTime(seconds);
    }
    public void setFilterEnvelopeDecayTime(double seconds){
        this.filterEnvelope.setDecayTime(seconds);
    }
    public void setFilterEnvelopeSustainLevel(double level){
        this.filterEnvelope.setSustainLevel(level);
    }
    public void setFilterEnvelopeReleaseTime(double seconds){
        this.filterEnvelope.setReleaseTime(seconds);
    }
    public void setFilterEnvelope(double attackTime, double decayTime, double sustainLevel, double releaseTime){
        this.filterEnvelope.setEnvelope(attackTime, decayTime, sustainLevel, releaseTime);
    }

    // Filter:
    public void setFilterParameters(double frequency, double resonance, double filterModAmount){
        this.filterCutoff = frequency;
        this.filterResonance = resonance;
        this.filterModAmount = filterModAmount;
        this.filter.setParameters(frequency, resonance);
    }

    // Gain Staging
    public void setFilterGainStaging(double preFilterGainDB, double postFilterGainDB){
        this.preFilterMult = Math.pow(10, preFilterGainDB / 20.0);
        this.postFilterMult = Math.pow(10, postFilterGainDB / 20.0);
    }

    // Velocity Multiplier
    public void setVelocity(double velocityMult){
        this.velocityMult = velocityMult;
    }

    // Voice State Control
    public void noteOn(){
        ampEnvelope.noteOn();
        filterEnvelope.noteOn();
    }

    public void noteOff(){
        ampEnvelope.noteOff();
        filterEnvelope.noteOff();
    }

    public boolean isActive() {
        return ampEnvelope.getStage() != Envelope.Stage.IDLE;
    }

    public double processSample(double input) {
            // Control Rate Logic, updates slow moving expensive elements every 'controlRate' samples
            if(controlRateCounter == 0) {

                // Calculate filter modulation based on the filter env values
                double filterEnvValue = filterEnvelope.processSample(1.0); // Grab filter multiplier from filter envelope
                double finalCutoff = filterCutoff + (filterEnvValue * filterModAmount); // Modulate cutoff based on the envelope multiplier
                filter.setParameters(finalCutoff, this.filterResonance); // Update filter params



            }
            this.controlRateCounter = (this.controlRateCounter + 1) % this.controlRate;

            // Sample Rate Logic, updates every sample
            // Generate next oscillator sample
            double sample = oscillator.processSample(input);

            // Process sample through filter
            sample *= this.preFilterMult;
            sample = filter.processSample(sample);
            sample *= this.postFilterMult;

            // Calculate Amp Envelope Value
            return ampEnvelope.processSample(sample) * this.velocityMult;
    }

    public double[] processSampleStereo(double input){

        // Audio Rate Logic

        // Conversion from Mono sample to Stereo, applies panning modulated by LFO
        double monoSample = processSample(input);
        this.panPosition = LFO.processSample(0.0) * this.panDepth;

        // Apply the Pan Law
        double panAngle = (this.panPosition + 1.0) * (Math.PI / 4.0);
        double leftGain = Math.cos(panAngle);
        double rightGain = Math.sin(panAngle);

        // Return the final stereo sample pair
        return new double[]{monoSample * leftGain, monoSample * rightGain};
    }
}
