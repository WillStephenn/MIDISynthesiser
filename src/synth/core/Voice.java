package synth.core;

import synth.components.Envelope;
import synth.components.filters.ResonantLowPassFilter;
import synth.components.oscillators.*;

/**
 * Represents a single voice in the synthesiser, encapsulating all audio components
 * required to generate a sound, including an oscillator, filter, and envelopes.
 * This class acts as a facade to simplify control over its internal components.
 */
public class Voice implements AudioComponent{

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
    private double filterModRange;
    private final double controlRate;
    private double controlRateCounter;

    // Gain Staging
    private double velocityMult;
    private double preFilterMult;
    private double postFilterMult;

    // LFO
    private double LFOFreq;

    // Panning
    private double panPosition;
    private double leftGain;
    private double rightGain;

    /**
     * Constructs a new Voice with the specified waveform, pitch, sample rate, control rate, and pan position.
     * @param waveform The oscillator waveform.
     * @param pitchFrequency The initial pitch frequency of the oscillator. Must not be negative.
     * @param sampleRate The audio sample rate. Must be positive.
     * @param controlRate The rate at which control signals are updated. Must be positive.
     * @param panPosition The initial stereo pan position (-1.0 to 1.0).
     */
    public Voice (Synthesiser.Waveform waveform, double pitchFrequency, double sampleRate, double controlRate, double panPosition){
        if (pitchFrequency < 0) {
            throw new IllegalArgumentException("Initial pitch frequency cannot be negative.");
        }
        if (controlRate <= 0) {
            throw new IllegalArgumentException("Control rate must be positive.");
        }
        // Audio Components
        this.filter = new ResonantLowPassFilter(sampleRate);
        this.ampEnvelope = new Envelope(sampleRate);
        this.filterEnvelope = new Envelope(sampleRate);

        setPanPosition(panPosition);

        // Filter Defaults
        this.filterCutoff = 20000;
        this.filterResonance = 0.01;
        this.filterModRange = 2000;
        this.filter.setParameters(this.filterCutoff, this.filterResonance);
        this.controlRate = controlRate;
        this.controlRateCounter = 0;

        // Synth Oscillator switch
        switch (waveform){
            case SINE -> this.oscillator = new SineOscillator(sampleRate);
            case SAW -> this.oscillator = new SawOscillator(sampleRate);
            case TRIANGLE -> this.oscillator = new TriangleOscillator(sampleRate);
            case SQUARE -> this.oscillator = new SquareOscillator(sampleRate);
            default -> throw new IllegalArgumentException("Unsupported waveform: " + waveform);
        }

        // Set Oscillator starting pitch
        this.pitchFrequency = pitchFrequency;
        this.oscillator.setFrequency(this.pitchFrequency);

        // Set Default Velocity
        this.velocityMult = 1.0;
    }

    // Facade Setter Methods
    /**
     * Sets the oscillator's pitch based on a MIDI note number.
     * @param pitchMIDI The MIDI note number (0-127).
     */
    public void setOscillatorPitch(byte pitchMIDI){
        if (pitchMIDI < 0) {
            throw new IllegalArgumentException("MIDI pitch cannot be negative.");
        }
        this.pitchMIDI = pitchMIDI;
        this.oscillator.setFrequency(440.0 * Math.pow(2.0, (pitchMIDI - 69) / 12.0));
    }

    /**
     * Gets the current MIDI pitch of the voice.
     * @return The MIDI note number.
     */
    public byte getPitchMIDI(){
        return this.pitchMIDI;
    }

    /**
     * Gets the current frequency of the oscillator.
     * @return The frequency in Hz.
     */
    public double getOscillatorFrequency(){
        return this.pitchFrequency;
    }

    /**
     * Sets the stereo pan position.
     * @param panPosition A value from -1.0 (full left) to 1.0 (full right).
     */
    public void setPanPosition(double panPosition){
        if (panPosition < -1.0 || panPosition > 1.0) {
            throw new IllegalArgumentException("Pan position must be between -1.0 and 1.0.");
        }
        // Apply the Pan Law
        this.panPosition = panPosition;
        double panAngle = (this.panPosition + 1.0) * (Math.PI / 4.0);
        this.leftGain = Math.cos(panAngle);
        this.rightGain = Math.sin(panAngle);
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

    /**
     * Sets the parameters for the resonant low-pass filter.
     * @param frequency The cutoff frequency in Hz.
     * @param resonance The resonance (Q) factor.
     * @param filterModRange The range of cutoff modulation from the filter envelope in Hz.
     */
    public void setFilterParameters(double frequency, double resonance, double filterModRange){
        this.filterCutoff = frequency;
        this.filterResonance = resonance;
        this.filterModRange = filterModRange;
        this.filter.setParameters(frequency, resonance);
    }

    /**
     * Sets the pre- and post-filter gain levels.
     * @param preFilterGainDB Gain before the filter in decibels.
     * @param postFilterGainDB Gain after the filter in decibels.
     */
    public void setFilterGainStaging(double preFilterGainDB, double postFilterGainDB){
        this.preFilterMult = Math.pow(10, preFilterGainDB / 20.0);
        this.postFilterMult = Math.pow(10, postFilterGainDB / 20.0);
    }

    /**
     * Sets the velocity multiplier for the voice's amplitude.
     * @param velocityMult A value from 0.0 to 1.0.
     */
    public void setVelocity(double velocityMult){
        if (velocityMult < 0.0 || velocityMult > 1.0) {
            throw new IllegalArgumentException("Velocity multiplier must be between 0.0 and 1.0.");
        }
        this.velocityMult = velocityMult;
    }

    /**
     * Triggers the note-on phase for the voice's envelopes.
     */
    public void noteOn(){
        ampEnvelope.noteOn();
        filterEnvelope.noteOn();
    }

    /**
     * Triggers the note-off phase for the voice's envelopes.
     */
    public void noteOff(){
        ampEnvelope.noteOff();
        filterEnvelope.noteOff();
    }

    /**
     * Checks if the voice is currently active.
     * @return true if the amplitude envelope is not in the IDLE stage.
     */
    public boolean isActive() {
        return ampEnvelope.getStage() != Envelope.Stage.IDLE;
    }

    /**
     * Checks if the voice is in the attack/decay/sustain stage. Useful for rendering.
     * @return true if the amplitude envelope is not in the IDLE OR RELEASE stage.
     */
    public boolean isActiveNoRelease() {
        return (ampEnvelope.getStage() != Envelope.Stage.IDLE) & (ampEnvelope.getStage() != Envelope.Stage.RELEASE);
    }



    /**
     * Processes a single audio sample through the voice's signal chain.
     * @param input The input sample (typically 0.0 for an oscillator-driven voice).
     * @return The processed mono audio sample.
     */
    @Override
    public double processSample(double input) {
        double filterEnvValue = filterEnvelope.processSample(1.0); // Grab filter multiplier from filter envelope

        // Control Rate Logic, updates slow moving expensive elements every 'controlRate' samples
            if(controlRateCounter == 0) {

                // Calculate filter modulation based on the filter env values

                double finalCutoff = filterCutoff + (filterEnvValue * filterModRange); // Modulate cutoff based on the envelope multiplier
                filter.setParameters(finalCutoff, this.filterResonance); // Update filter params

            }
            this.controlRateCounter = (this.controlRateCounter + 1) % (int)controlRate;

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

    /**
     * Processes a single audio sample and returns a stereo frame.
     * @param input The input sample.
     * @return A two-element array containing the left and right channel samples.
     */
    public double[] processSampleStereo(double input){

        // Conversion from Mono sample to Stereo, applies panning modulated by LFO
        double monoSample = processSample(input);
        return new double[]{monoSample * leftGain, monoSample * rightGain};
    }
}
