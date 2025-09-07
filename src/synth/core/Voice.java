package synth.core;

import synth.components.Envelope;
import synth.components.filters.ResonantLowPassFilter;
import synth.components.oscillators.*;
import synth.utils.LookupTables;
import java.util.Map;


/**
 * Represents a single voice in the synthesiser, encapsulating all audio components
 * required to generate a sound, including an oscillator, filter, and envelopes.
 * This class acts as a facade to simplify control over its internal components.
 */
public class Voice implements AudioComponent{

    // Audio Component Objects
    private Oscillator oscillator;
    private final Oscillator sine;
    private final Oscillator saw;
    private final Oscillator triangle;
    private final Oscillator square;

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
    private int controlRateCounter;

    // Gain Staging
    private double velocityMult;
    private double preFilterMult;
    private double postFilterMult;

    // LFO
    private double LFOFreq;

    // Panning
    private double panDepth;
    private double panPosition;
    private double leftGain;
    private double rightGain;

    // Pre Computed Constants
    private static final double panIndexScalar = LookupTables.TABLE_SIZE / (2.0 * Math.PI) * (Math.PI/4);

    // Output Buffers
    private final double[] oscillatorOutputBuffer;
    private final double[] filterOutputBuffer;
    private final double[] filterEnvelopeOutputBuffer;
    private final double[] ampEnvelopeOutputBuffer;
    private final double[] stereoOutputBuffer;

    /**
     * Constructs a new Voice with the specified waveform, pitch, sample rate, and pan position.
     *
     * @param waveform       The oscillator waveform.
     * @param pitchFrequency The initial pitch frequency of the oscillator. Must not be negative.
     * @param sampleRate     The audio sample rate. Must be positive.
     * @param panPosition    The initial stereo pan position (-1.0 to 1.0).
     */
    public Voice (Synthesiser.Waveform waveform, double pitchFrequency, double sampleRate, int blockSize){
        if (pitchFrequency < 0) {
            throw new IllegalArgumentException("Initial pitch frequency cannot be negative.");
        }
        // Audio Components
        this.oscillator = new SineOscillator(sampleRate);
        this.sine = new SineOscillator(sampleRate);
        this.saw = new SawOscillator(sampleRate);
        this.triangle = new TriangleOscillator(sampleRate);
        this.square = new SquareOscillator(sampleRate);
        setOscillatorWaveform(waveform);

        this.filter = new ResonantLowPassFilter(sampleRate);
        this.ampEnvelope = new Envelope(sampleRate);
        this.filterEnvelope = new Envelope(sampleRate);

        setPanPosition(0);

        // Filter Defaults
        this.filterCutoff = 20000;
        this.filterResonance = 1;
        this.filterModRange = 2000;
        this.filter.setParameters(this.filterCutoff, this.filterResonance);
        this.controlRateCounter = 0;

        // Set Oscillator starting pitch
        this.pitchFrequency = pitchFrequency;
        this.oscillator.setFrequency(this.pitchFrequency);

        // Set Default Velocity
        this.velocityMult = 1.0;

        // Panning Defaults
        this.panDepth = 1.0;

        // Output Buffers
        this.oscillatorOutputBuffer = new double[blockSize];
        this.filterOutputBuffer = new double[blockSize];
        this.filterEnvelopeOutputBuffer  = new double[blockSize];
        this.ampEnvelopeOutputBuffer  = new double[blockSize];
        this.stereoOutputBuffer = new double[blockSize * 2];
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
        this.oscillator.setFrequency(LookupTables.MIDI_TO_HZ[pitchMIDI]);
    }

    public void setOscillatorWaveform(Synthesiser.Waveform waveform){
        switch (waveform) {
            case SINE -> this.oscillator = this.sine;
            case SQUARE -> this.oscillator = this.square;
            case TRIANGLE -> this.oscillator = this.triangle;
            case SAW -> this.oscillator = this.saw;
            default -> throw new IllegalArgumentException("Unsupported waveform: " + waveform);
        }
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
        int index = (int) ((panPosition + 1.0) * panIndexScalar);
        this.leftGain  = LookupTables.COSINE[index];
        this.rightGain = LookupTables.SINE[index];
    }

    public void setPanDepth(double panDepth) {
        this.panDepth = panDepth;
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
     * Processes a block of audio, applying the envelope to each sample.
     * @param inputBuffer not used. Here for interface consistency.
     * @param outputBuffer The buffer where the modulated audio will be written.
     * @param blockSize The number of samples to process.
     */
    @Override
    public void processBlock(double[] nullBuffer, double[] stereoOutputBuffer, int blockSize) {
        // Populate base audio component buffers
        oscillator.processBlock(null, this.oscillatorOutputBuffer, blockSize);
        filterEnvelope.processBlock(null, this.filterEnvelopeOutputBuffer, blockSize);

        // Apply Pre-Filter Gain Staging:
        for (int i = 0; i < blockSize; i++) {
            this.oscillatorOutputBuffer[i] *= this.preFilterMult;
        }

        // Set Filter Parameters
        double filterEnvValue = this.filterEnvelopeOutputBuffer[0];
        double finalCutoff = filterCutoff + (filterEnvValue * filterModRange);
        filter.setParameters(finalCutoff, this.filterResonance);

        // Apply Filter then Amp Env Processing
        filter.processBlock(this.oscillatorOutputBuffer, this.filterOutputBuffer, blockSize);
        ampEnvelope.processBlock(this.filterOutputBuffer, this.ampEnvelopeOutputBuffer, blockSize);

        // Conversion from Mono sample to Stereo, applies panning modulated by LFO
        double monoSample = 0.0;

        for (int i = 0; i < blockSize; i++){
            monoSample = this.ampEnvelopeOutputBuffer[i] * this.velocityMult * this.postFilterMult;
            stereoOutputBuffer[i * 2] = monoSample * leftGain;
            stereoOutputBuffer[i * 2 + 1] = monoSample * rightGain;
        }
    }

    /**
     * Processes a block of audio, applying the envelope to each sample, and records performance metrics.
     * @param lfoBuffer The LFO signal for modulation.
     * @param stereoOutputBuffer The buffer where the modulated audio will be written.
     * @param blockSize The number of samples to process.
     * @param timings A map to store the execution time of each processing stage.
     */
    public void processBlockInstrumented(double[] lfoBuffer, double[] stereoOutputBuffer, int blockSize, Map<String, Long> timings) {
        long startTime, endTime;

        // Oscillator
        startTime = System.nanoTime();
        oscillator.processBlock(null, this.oscillatorOutputBuffer, blockSize);
        endTime = System.nanoTime();
        timings.merge("Oscillator", endTime - startTime, Long::sum);

        // Filter Envelope
        startTime = System.nanoTime();
        filterEnvelope.processBlock(null, this.filterEnvelopeOutputBuffer, blockSize);
        endTime = System.nanoTime();
        timings.merge("Filter Envelope", endTime - startTime, Long::sum);

        // Pre-Filter Gain
        startTime = System.nanoTime();
        for (int i = 0; i < blockSize; i++) {
            this.oscillatorOutputBuffer[i] *= this.preFilterMult;
        }
        endTime = System.nanoTime();
        timings.merge("Pre-Filter Gain", endTime - startTime, Long::sum);

        // Filter Parameter Calculation
        startTime = System.nanoTime();
        double filterEnvValue = this.filterEnvelopeOutputBuffer[0];
        double finalCutoff = filterCutoff + (filterEnvValue * filterModRange);
        filter.setParameters(finalCutoff, this.filterResonance);
        endTime = System.nanoTime();
        timings.merge("Filter Params", endTime - startTime, Long::sum);

        // Filtering
        startTime = System.nanoTime();
        filter.processBlock(this.oscillatorOutputBuffer, this.filterOutputBuffer, blockSize);
        endTime = System.nanoTime();
        timings.merge("Filter", endTime - startTime, Long::sum);

        // Amplitude Envelope Processing
        startTime = System.nanoTime();
        ampEnvelope.processBlock(this.filterOutputBuffer, this.ampEnvelopeOutputBuffer, blockSize);
        endTime = System.nanoTime();
        timings.merge("Amp Envelope", endTime - startTime, Long::sum);

        // Stereo Panning & Output
        startTime = System.nanoTime();
        double monoSample = 0.0;

        for (int i = 0; i < blockSize; i++){
            monoSample = this.ampEnvelopeOutputBuffer[i] * this.velocityMult * this.postFilterMult;
            stereoOutputBuffer[i * 2] = monoSample * leftGain;
            stereoOutputBuffer[i * 2 + 1] = monoSample * rightGain;
        }
        endTime = System.nanoTime();
        timings.merge("Panning", endTime - startTime, Long::sum);
    }
}
