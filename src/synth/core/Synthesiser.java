package synth.core;

import synth.components.oscillators.Oscillator;
import synth.components.oscillators.SawOscillator;
import synth.components.oscillators.SineOscillator;
import synth.components.oscillators.TriangleOscillator;
import synth.utils.AudioConstants;

import java.util.ArrayList;

/**
 * The main synthesiser class that manages and processes multiple voices.
 * It acts as a facade for controlling all voice parameters and generating the final audio output.
 */
public class Synthesiser{
    // Control all the voices. Bundles them up in an arraylist ready to be shipped to the buffer.
    private ArrayList<Voice> voices;
    private final double noVoices;
    private final double sampleRate;
    private final double controlRate;

    // Master Configs (synth-wide settings)
    // Oscillator
    public enum Waveform {
        SINE, SAW, TRIANGLE
    }
    private Waveform waveform;

    // Filter
    private double filterCutoff;
    private double filterResonance;
    private double filterModRange;

    // Filter Envelope
    private double filterAttackTime;
    private double filterDecayTime;
    private double filterSustainLevel;
    private double filterReleaseTime;

    // Amp Envelope
    private double ampAttackTime;
    private double ampDecayTime;
    private double ampSustainLevel;
    private double ampReleaseTime;
    
    // Gain Staging
    private double preFilterGainDB;
    private double postFilterGainDB;
    private double mixStageAttenuation;

    // LFO
    private Oscillator LFO;
    private Waveform LFOWaveForm;
    private double LFOFrequency;
    private double LFOPosition;

    // Panning
    private double panDepth;

    /**
     * Constructs a new Synthesiser with a specified number of voices.
     * @param noVoices The number of voices for the synthesiser. Must be a positive number.
     */
    public Synthesiser(double noVoices) {
        if (noVoices <= 0) {
            throw new IllegalArgumentException("Number of voices must be positive.");
        }
        this.noVoices = noVoices;
        this.mixStageAttenuation = 1.0 / Math.sqrt(this.noVoices);
        this.sampleRate = AudioConstants.SAMPLE_RATE; // Point of dependency injection for entire voice stack
        this.controlRate = AudioConstants.CONTROL_RATE;
        this.voices = new ArrayList<Voice>();

        // Default Synth Patch
        loadPatch( // Applies default patch and populates the voice bank
                Waveform.SAW, // Synth Waveform
                500,     // filterCutoff
                5,       // filterResonance
                2000.0,  // filterModRange (Hz)
                0.01,    // filterAttackTime
                0.4,     // filterDecayTime
                0.5,     // filterSustainLevel
                0.4,     // filterReleaseTime
                0.005,   // ampAttackTime
                0.3,     // ampDecayTime
                0.5,     // ampSustainLevel
                0.5,     // ampReleaseTime
                -5.0,    // Pre Filter Gain (db)
                0.0,     // Post Filter Gain (db)
                Waveform.TRIANGLE, //LFO Waveform
                0.5,     // LFO Frequency
                1        // Pan Depth
        );
    }

    /**
     * Loads a new patch with the specified parameters, reconfiguring the entire synthesiser.
     * @param waveform The oscillator waveform for all voices.
     * @param filterCutoff The base cutoff frequency for the filter.
     * @param filterResonance The resonance (Q) of the filter.
     * @param filterModRange The amount of modulation applied to the filter cutoff by the filter envelope.
     * @param filterAttackTime The attack time for the filter envelope.
     * @param filterDecayTime The decay time for the filter envelope.
     * @param filterSustainLevel The sustain level for the filter envelope.
     * @param filterReleaseTime The release time for the filter envelope.
     * @param ampAttackTime The attack time for the amplitude envelope.
     * @param ampDecayTime The decay time for the amplitude envelope.
     * @param ampSustainLevel The sustain level for the amplitude envelope.
     * @param ampReleaseTime The release time for the amplitude envelope.
     * @param preFilterGainDB The gain applied before the filter, in decibels.
     * @param postFilterGainDB The gain applied after the filter, in decibels.
     * @param LFOWaveForm The waveform for the Low-Frequency Oscillator (LFO).
     * @param LFOFrequency The frequency of the LFO.
     * @param panDepth The depth of the stereo panning effect.
     */
    public void loadPatch(Waveform waveform,
                          double filterCutoff,
                          double filterResonance,
                          double filterModRange,
                          double filterAttackTime,
                          double filterDecayTime,
                          double filterSustainLevel,
                          double filterReleaseTime,
                          double ampAttackTime,
                          double ampDecayTime,
                          double ampSustainLevel,
                          double ampReleaseTime,
                          double preFilterGainDB,
                          double postFilterGainDB,
                          Waveform LFOWaveForm,
                          double LFOFrequency,
                          double panDepth) {

        // Store the master settings for the synth
        updateWaveForm(waveform); // Clears and re-populates voice bank with new waveform
        updateLFOWaveform(LFOWaveForm);
        this.filterCutoff = filterCutoff;
        this.filterResonance = filterResonance;
        this.filterModRange = filterModRange;
        this.filterAttackTime = filterAttackTime;
        this.filterDecayTime = filterDecayTime;
        this.filterSustainLevel = filterSustainLevel;
        this.filterReleaseTime = filterReleaseTime;
        this.ampAttackTime = ampAttackTime;
        this.ampDecayTime = ampDecayTime;
        this.ampSustainLevel = ampSustainLevel;
        this.ampReleaseTime = ampReleaseTime;
        this.preFilterGainDB = preFilterGainDB;
        this.postFilterGainDB = postFilterGainDB;
        this.LFOFrequency = LFOFrequency;
        this.panDepth = panDepth;
        applyPatch();
    }

    /**
     * Updates the waveform for the Low-Frequency Oscillator (LFO).
     * @param LFOWaveForm The new waveform for the LFO.
     */
    public void updateLFOWaveform(Waveform LFOWaveForm){
        if(this.LFOWaveForm != LFOWaveForm){
            // LFO Oscillator switch
            switch (LFOWaveForm){
                case SINE -> this.LFO = new SineOscillator(this.sampleRate);
                case SAW -> this.LFO = new SawOscillator(this.sampleRate);
                case TRIANGLE -> this.LFO = new TriangleOscillator(this.sampleRate);
                default -> throw new IllegalArgumentException("Unsupported waveform: " + waveform);
            }
        }
    }

    /**
     * Sets the frequency of the Low-Frequency Oscillator (LFO).
     * @param LFOFrequency The new frequency in Hz. Must not be negative.
     */
    public void setLFOFrequency(double LFOFrequency){
        if (LFOFrequency < 0) {
            throw new IllegalArgumentException("LFO frequency cannot be negative.");
        }
        this.LFOFrequency = LFOFrequency;
        LFO.setFrequency(LFOFrequency);
    }

    /**
     * Updates the main oscillator waveform for all voices.
     * This will clear and repopulate the voice bank with new oscillators.
     * @param waveform The new waveform to use.
     */
    public void updateWaveForm(Waveform waveform){
        // Scrap all voices and repopulate the voice bank
        if (this.waveform != waveform){
            this.waveform = waveform;
            voices.clear();
            for (int i = 0; i < this.noVoices; i++) {
                voices.add(new Voice(this.waveform, 0, this.sampleRate, this.controlRate, 0));
            }
        }
    }

    /**
     * Applies the current patch settings to all voices.
     */
    public void applyPatch(){
        updateWaveForm(this.waveform); // Clears and re-populates voice bank with new waveform
        updateLFOWaveform(this.LFOWaveForm);
        for(Voice voice : voices){
            voice.setAmpEnvelope(this.ampAttackTime, this.ampDecayTime, this.ampSustainLevel, this.ampReleaseTime);
            voice.setFilterEnvelope(this.filterAttackTime, this.filterDecayTime, this.filterSustainLevel, this.filterReleaseTime);
            voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModRange);
            voice.setFilterGainStaging(this.preFilterGainDB, this.postFilterGainDB);
            setLFOFrequency(this.LFOFrequency);
        }
    }

    /**
     * Checks if any voice is currently active (i.e., not in the IDLE stage).
     * @return true if at least one voice is active, false otherwise.
     */
    public boolean anyVoicesActive(){
        for (Voice voice:voices){
            if (voice.isActive()){
                return true;
            }
        }
        return false;
    }

    /**
     * Triggers a note-on event for a given MIDI pitch and velocity.
     * It finds an available voice and assigns it to play the note.
     * @param pitchMIDI The MIDI pitch of the note.
     * @param velocity The velocity of the note (0.0 to 1.0).
     */
    public void noteOn(byte pitchMIDI, double velocity) {
        if (velocity < 0.0 || velocity > 1.0) {
            throw new IllegalArgumentException("Velocity must be between 0.0 and 1.0.");
        }
        // Check if note is already being played and switch it off if it is
        noteOff(pitchMIDI);

        for (Voice voice : voices) {
            if (!voice.isActive()) { // Find first inactive voice
                // Apply Patch
                voice.setOscillatorPitch(pitchMIDI);
                voice.setVelocity(velocity);
                voice.setAmpEnvelope(this.ampAttackTime, this.ampDecayTime, this.ampSustainLevel, this.ampReleaseTime);
                voice.setFilterEnvelope(this.filterAttackTime, this.filterDecayTime, this.filterSustainLevel, this.filterReleaseTime);
                voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModRange);
                voice.setFilterGainStaging(this.preFilterGainDB, this.postFilterGainDB);
                voice.setPanPosition(getPanPosition());
                voice.noteOn();
                return;
            }
        }
    }

    /**
     * Gets the current stereo pan position based on the LFO.
     * @return The pan position, ranging from -1.0 (left) to 1.0 (right).
     */
    public double getPanPosition(){
        return (this.LFOPosition * this.panDepth);
    }

    /**
     * Triggers a note-off event for a given MIDI pitch.
     * @param pitchMIDI The MIDI pitch of the note to release.
     */
    public void noteOff(byte pitchMIDI){
        for (Voice voice : voices){
            if(voice.isActive() && (voice.getPitchMIDI() == pitchMIDI)){
                voice.noteOff();
            }
        }
    }

    /**
     * Processes one block of audio samples for all active voices.
     * @return A stereo audio frame (left and right channels).
     */
    public double[] processSample(){
        this.LFOPosition = LFO.processSample(0.0);
        double sampleMixedL = 0.0;
        double sampleMixedR = 0.0;

        for(Voice voice:voices){
            if(voice.isActive()){
                double[] stereoSampleMixed = voice.processSampleStereo(0.0);
                sampleMixedL += stereoSampleMixed[0] * this.mixStageAttenuation;
                sampleMixedR += stereoSampleMixed[1] * this.mixStageAttenuation;
            }
        }

        // Hard Clipping
        sampleMixedL = Math.max(-1.0, Math.min(1.0, sampleMixedL));
        sampleMixedR = Math.max(-1.0, Math.min(1.0, sampleMixedR));

        return new double[]{sampleMixedL, sampleMixedR};
    }

}

