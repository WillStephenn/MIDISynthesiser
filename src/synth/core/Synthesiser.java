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
        SINE, SAW, TRIANGLE, SQUARE
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
    public Synthesiser(int noVoices) {
        if (noVoices <= 0) {
            throw new IllegalArgumentException("Number of voices must be positive.");
        }
        this.noVoices = noVoices;
        this.mixStageAttenuation = 1.0 / Math.sqrt(this.noVoices);
        this.sampleRate = AudioConstants.SAMPLE_RATE; // Point of dependency injection for entire voice stack
        this.controlRate = AudioConstants.CONTROL_RATE;
        this.voices = new ArrayList<Voice>();

        // Initialise the filter parameters to 1 so voice.setFilterParameters doesn't throw an error on construction.
        this.filterCutoff = 1;
        this.filterResonance = 1;
        this.filterModRange = 1;

        // Default Synth Patch
        loadPatch( // Applies default patch and populates the voice bank
                Waveform.SQUARE, // Synth Waveform
                1000,     // filterCutoff
                3,       // filterResonance
                2000.0,  // filterModRange (Hz)
                0.01,    // filterAttackTime
                0.3,     // filterDecayTime
                0.5,     // filterSustainLevel
                0.1,     // filterReleaseTime
                0.005,   // ampAttackTime
                0.1,     // ampDecayTime
                0.4,     // ampSustainLevel
                0.4,     // ampReleaseTime
                -3.0,    // Pre Filter Gain (db)
                0.0,     // Post Filter Gain (db)
                Waveform.SINE, //LFO Waveform
                1,     // LFO Frequency
                0.4        // Pan Depth
        );
    }


    //  --- Setters ---
    /**
     * Updates the waveform for the Low-Frequency Oscillator (LFO).
     * @param LFOWaveForm The new waveform for the LFO.
     */
    public void updateLFOWaveform(Waveform LFOWaveForm){
        if(this.LFOWaveForm != LFOWaveForm){
            this.LFOWaveForm = LFOWaveForm;
            // LFO Oscillator switch
            switch (LFOWaveForm){
                case SINE -> this.LFO = new SineOscillator(this.sampleRate);
                case SAW -> this.LFO = new SawOscillator(this.sampleRate);
                case TRIANGLE -> this.LFO = new TriangleOscillator(this.sampleRate);
                case SQUARE -> this.LFO = new TriangleOscillator(this.sampleRate);
                default -> throw new IllegalArgumentException("Unsupported waveform: " + waveform);
            }
        }
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
            synchronized (voices) {
                voices.clear();
                for (int i = 0; i < this.noVoices; i++) {
                    voices.add(new Voice(this.waveform, 0, this.sampleRate, this.controlRate, 0));
                }
            }
        }
    }

    public void updateVoiceParams(Voice voice){
        voice.setAmpEnvelope(this.ampAttackTime, this.ampDecayTime, this.ampSustainLevel, this.ampReleaseTime);
        voice.setFilterEnvelope(this.filterAttackTime, this.filterDecayTime, this.filterSustainLevel, this.filterReleaseTime);
        voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModRange);
        voice.setFilterGainStaging(this.preFilterGainDB, this.postFilterGainDB);
    }

    // --- Real-time Parameter Control Setter Methods ---

    public void setFilterCutoff(double cutoff) {
        this.filterCutoff = Math.max(20.0, Math.min(20000.0, cutoff)); // Clamp to audible range
        for (Voice voice : voices) {
            voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModRange);
        }
    }

    public void setFilterResonance(double resonance) {
        this.filterResonance = Math.max(1.0, Math.min(20.0, resonance)); // Clamp from 1 - 20
        for (Voice voice : voices) {
            voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModRange);
        }
    }

    public void setFilterModRange(double modRange) {
        this.filterModRange = Math.max(0.0, modRange);
        for (Voice voice : voices) {
            voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModRange);
        }
    }

    public void setFilterAttackTime(double seconds) {
        this.filterAttackTime = Math.max(0.0, seconds);
        for (Voice voice : voices) {
            voice.setFilterEnvelopeAttackTime(this.filterAttackTime);
        }
    }

    public void setFilterDecayTime(double seconds) {
        this.filterDecayTime = Math.max(0.0, seconds);
        for (Voice voice : voices) {
            voice.setFilterEnvelopeDecayTime(this.filterDecayTime);
        }
    }

    public void setFilterSustainLevel(double level) {
        this.filterSustainLevel = Math.max(0.0, Math.min(1.0, level));
        for (Voice voice : voices) {
            voice.setFilterEnvelopeSustainLevel(this.filterSustainLevel);
        }
    }

    public void setFilterReleaseTime(double seconds) {
        this.filterReleaseTime = Math.max(0.0, seconds);
        for (Voice voice : voices) {
            voice.setFilterEnvelopeReleaseTime(this.filterReleaseTime);
        }
    }

    public void setAmpAttackTime(double seconds) {
        this.ampAttackTime = Math.max(0.0, seconds);
        for (Voice voice : voices) {
            voice.setAmpEnvelopeAttackTime(this.ampAttackTime);
        }
    }

    public void setAmpDecayTime(double seconds) {
        this.ampDecayTime = Math.max(0.0, seconds);
        for (Voice voice : voices) {
            voice.setAmpEnvelopeDecayTime(this.ampDecayTime);
        }
    }

    public void setAmpSustainLevel(double level) {
        this.ampSustainLevel = Math.max(0.0, Math.min(1.0, level));
        for (Voice voice : voices) {
            voice.setAmpEnvelopeSustainLevel(this.ampSustainLevel);
        }
    }

    public void setAmpReleaseTime(double seconds) {
        this.ampReleaseTime = Math.max(0.0, seconds);
        for (Voice voice : voices) {
            voice.setAmpEnvelopeReleaseTime(this.ampReleaseTime);
        }
    }

    public void setPreFilterGainDB(double db) {
        this.preFilterGainDB = db;
        for (Voice voice : voices) {
            voice.setFilterGainStaging(this.preFilterGainDB, this.postFilterGainDB);
        }
    }

    public void setPostFilterGainDB(double db) {
        this.postFilterGainDB = db;
        for (Voice voice : voices) {
            voice.setFilterGainStaging(this.preFilterGainDB, this.postFilterGainDB);
        }
    }

    public void setLFOFrequency(double frequency) {
        this.LFOFrequency = Math.max(0.0, frequency);
        this.LFO.setFrequency(this.LFOFrequency);
    }

    public void setPanDepth(double depth) {
        this.panDepth = Math.max(0.0, Math.min(1.0, depth));
    }

    public void setWaveform(Waveform newWaveform) {
        if (this.waveform != newWaveform) {
            updateWaveForm(newWaveform);
            applyPatch();
        }
    }

    public void setLFOWaveform(Waveform LFOWaveForm) {
        if (this.LFOWaveForm != LFOWaveForm) {
            updateWaveForm(LFOWaveForm);
        }
    }

    public void setMasterVolume(double volumeScalar){
        this.mixStageAttenuation *= volumeScalar;
  ;  }

    /**
     * Applies all current patch settings to all voices.
     */
    public void applyPatch(){
        updateWaveForm(this.waveform);
        updateLFOWaveform(this.LFOWaveForm);
        for(Voice voice : voices){
            updateVoiceParams(voice);
        }
        setLFOFrequency(this.LFOFrequency);
    }

    //  --- Getters ---
    public Waveform getWaveform() { return waveform; }
    public double getAmpAttackTime() { return ampAttackTime; }
    public double getAmpDecayTime() { return ampDecayTime; }
    public double getAmpSustainLevel() { return ampSustainLevel; }
    public double getAmpReleaseTime() { return ampReleaseTime; }
    public double getFilterCutoff() { return filterCutoff; }
    public double getFilterResonance() { return filterResonance; }
    public double getFilterModRange() { return filterModRange; }
    public double getFilterAttackTime() { return filterAttackTime; }
    public double getFilterDecayTime() { return filterDecayTime; }
    public double getFilterSustainLevel() { return filterSustainLevel; }
    public double getFilterReleaseTime() { return filterReleaseTime; }
    public double getPreFilterGainDB() { return preFilterGainDB; }
    public double getPostFilterGainDB() { return postFilterGainDB; }
    public Waveform getLFOWaveform() { return LFOWaveForm; }
    public double getLFOFrequency() { return LFOFrequency; }
    public double getPanDepth() { return panDepth; }

    /**
     * Gets the current stereo pan position based on the LFO.
     * @return The pan position, ranging from -1.0 (left) to 1.0 (right).
     */
    public double getPanPosition(){
        return (this.LFOPosition * this.panDepth);
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

    // --- Synth Control/Processing Methods ---

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
        setFilterCutoff(filterCutoff);
        setFilterResonance(filterResonance);
        setFilterModRange(filterModRange);
        setFilterAttackTime(filterAttackTime);
        setFilterDecayTime(filterDecayTime);
        setFilterSustainLevel(filterSustainLevel);
        setFilterReleaseTime(filterReleaseTime);
        setAmpAttackTime(ampAttackTime);
        setAmpDecayTime(ampDecayTime);
        setAmpSustainLevel(ampSustainLevel);
        setAmpReleaseTime(ampReleaseTime);
        setPreFilterGainDB(preFilterGainDB);
        setPostFilterGainDB(postFilterGainDB);
        setLFOFrequency(LFOFrequency);
        setPanDepth(panDepth);
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
                updateVoiceParams(voice);
                voice.setPanPosition(getPanPosition());
                voice.noteOn();
                return;
            }
        }
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

        synchronized (voices) {
            for (Voice voice : voices) {
                if (voice.isActive()) {
                    double[] stereoSampleMixed = voice.processSampleStereo(0.0);
                    sampleMixedL += stereoSampleMixed[0] * this.mixStageAttenuation;
                    sampleMixedR += stereoSampleMixed[1] * this.mixStageAttenuation;
                }
            }
        }

        // Hard Clipping
        sampleMixedL = Math.max(-1.0, Math.min(1.0, sampleMixedL));
        sampleMixedR = Math.max(-1.0, Math.min(1.0, sampleMixedR));

        return new double[]{sampleMixedL, sampleMixedR};
    }

}

