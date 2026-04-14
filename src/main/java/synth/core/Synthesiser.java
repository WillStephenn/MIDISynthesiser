package synth.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import synth.components.oscillators.Oscillator;
import synth.components.oscillators.SawOscillator;
import synth.components.oscillators.SineOscillator;
import synth.components.oscillators.SquareOscillator;
import synth.components.oscillators.TriangleOscillator;

/**
 * The main synthesiser class that manages and processes multiple voices.
 * It acts as a facade for controlling all voice parameters and generating the final audio output.
 */
public class Synthesiser{
    // Control all the voices. Bundles them up in an arraylist ready to be shipped to the buffer.
    private final Voice[] voices;
    private final double sampleRate;

    // Master Configs (synth-wide settings)
    // Oscillator
    public enum Waveform {
        SINE, SAW, TRIANGLE, SQUARE
    }
    private volatile Waveform waveform;

    // Filter
    private volatile double filterCutoff;
    private volatile double filterResonance;
    private volatile double filterModRange;

    // Filter Envelope
    private volatile double filterAttackTime;
    private volatile double filterDecayTime;
    private volatile double filterSustainLevel;
    private volatile double filterReleaseTime;

    // Amp Envelope
    private volatile double ampAttackTime;
    private volatile double ampDecayTime;
    private volatile double ampSustainLevel;
    private volatile double ampReleaseTime;

    // Gain Staging
    private volatile double preFilterGainDB;
    private volatile double postFilterGainDB;
    private final double voiceSumAttenuation;
    private volatile double volumeAttenuation;
    private volatile double masterVolumeScalar = 1.0;

    // LFO
    private Oscillator LFO;
    private final Oscillator sineLFO;
    private final Oscillator sawLFO;
    private final Oscillator triangleLFO;
    private final Oscillator squareLFO;
    private volatile Waveform LFOWaveForm;
    private volatile double LFOFrequency;
    private volatile double LFOPosition;

    // Panning
    private volatile double panDepth;

    // Granular dirty flags: setters set per-group flag, audio thread clears after syncing to voices
    private final AtomicBoolean waveformDirty = new AtomicBoolean(false);
    private final AtomicBoolean filterDirty = new AtomicBoolean(false);
    private final AtomicBoolean filterEnvDirty = new AtomicBoolean(false);
    private final AtomicBoolean ampEnvDirty = new AtomicBoolean(false);
    private final AtomicBoolean gainDirty = new AtomicBoolean(false);
    private final AtomicBoolean panDirty = new AtomicBoolean(false);

    // Output Buffers
    int blockSize;
    private final double[] voiceOutputBuffer;
    private final double[] lfoOutputBuffer;

    /**
     * Constructs a new Synthesiser with a specified number of voices.
     * @param noVoices The number of voices for the synthesiser. Must be a positive number.
     */
    public Synthesiser(int noVoices, double sampleRate, int blockSize) {
        if (noVoices <= 0) {
            throw new IllegalArgumentException("Number of voices must be positive.");
        }
        if (sampleRate <= 40.0) {
            throw new IllegalArgumentException("Sample rate must be greater than 40 Hz.");
        }
        this.sampleRate = sampleRate;
        this.voiceSumAttenuation = 1.0 / Math.sqrt(noVoices);
        this.volumeAttenuation = this.voiceSumAttenuation;
        this.voices = new Voice[noVoices];

        // Populate voice bank
        for (int i = 0; i < noVoices; i++){
            voices[i] = new Voice(Waveform.SINE, 0, sampleRate, blockSize);
        }

        // Initialise the filter parameters to 1 so voice.setFilterParameters doesn't throw an error on construction.
        this.filterCutoff = 1;
        this.filterResonance = 1;
        this.filterModRange = 1;

        // Construct Buffers
        this.blockSize = blockSize;
        this.voiceOutputBuffer = new double[this.blockSize * 2];
        this.lfoOutputBuffer = new double[this.blockSize];

        // Construct LFO Oscillators
        this.sineLFO = new SineOscillator(sampleRate);
        this.sawLFO = new SawOscillator(sampleRate);
        this.triangleLFO = new TriangleOscillator(sampleRate);
        this.squareLFO = new SquareOscillator(sampleRate);
        this.LFO = this.sineLFO;

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
    public void setLFOWaveform(Waveform LFOWaveForm){
        if (LFOWaveForm == null) {
            throw new IllegalArgumentException("LFOWaveForm cannot be null");
        }
        if(this.LFOWaveForm != LFOWaveForm){
            this.LFOWaveForm = LFOWaveForm;
        }
    }

    /**
     * Sets the main oscillator waveform. The change is deferred and applied
     * to all voices by the audio thread at the start of the next processing block.
     * @param waveform The new waveform to use.
     */
    public void setOscillatorWaveform(Waveform waveform){
        if (waveform == null) {
            throw new IllegalArgumentException("waveform cannot be null");
        }
        if (this.waveform != waveform){
            this.waveform = waveform;
            this.waveformDirty.set(true);
        }
    }

    public void setVoiceParams(Voice voice){
        voice.setOscillatorWaveform(this.waveform);
        voice.setAmpEnvelope(this.ampAttackTime, this.ampDecayTime, this.ampSustainLevel, this.ampReleaseTime);
        voice.setFilterEnvelope(this.filterAttackTime, this.filterDecayTime, this.filterSustainLevel, this.filterReleaseTime);
        voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModRange);
        voice.setFilterGainStaging(this.preFilterGainDB, this.postFilterGainDB);
        voice.setPanDepth(this.panDepth);
    }

    public void setFilterCutoff(double cutoff) {
        double nyquistLimit = (this.sampleRate / 2.0) - 1.0;
        double maxCutoff = Math.nextDown(nyquistLimit);
        double clamped = Math.max(20.0, Math.min(maxCutoff, cutoff));
        if (Double.compare(this.filterCutoff, clamped) != 0) {
            this.filterCutoff = clamped;
            this.filterDirty.set(true);
        }
    }

    public void setFilterResonance(double resonance) {
        double clamped = Math.max(1.0, Math.min(20.0, resonance));
        if (Double.compare(this.filterResonance, clamped) != 0) {
            this.filterResonance = clamped;
            this.filterDirty.set(true);
        }
    }

    public void setFilterModRange(double modRange) {
        double nyquistLimit = (this.sampleRate / 2.0) - 1.0;
        double maxModRange = Math.max(0.0, Math.nextDown(nyquistLimit) - this.filterCutoff);
        double clamped = Math.max(0.0, Math.min(modRange, maxModRange));
        if (Double.compare(this.filterModRange, clamped) != 0) {
            this.filterModRange = clamped;
            this.filterDirty.set(true);
        }
    }

    public void setFilterAttackTime(double seconds) {
        double clamped = Math.max(0.0, seconds);
        if (Double.compare(this.filterAttackTime, clamped) != 0) {
            this.filterAttackTime = clamped;
            this.filterEnvDirty.set(true);
        }
    }

    public void setFilterDecayTime(double seconds) {
        double clamped = Math.max(0.0, seconds);
        if (Double.compare(this.filterDecayTime, clamped) != 0) {
            this.filterDecayTime = clamped;
            this.filterEnvDirty.set(true);
        }
    }

    public void setFilterSustainLevel(double level) {
        double clamped = Math.max(0.0, Math.min(1.0, level));
        if (Double.compare(this.filterSustainLevel, clamped) != 0) {
            this.filterSustainLevel = clamped;
            this.filterEnvDirty.set(true);
        }
    }

    public void setFilterReleaseTime(double seconds) {
        double clamped = Math.max(0.0, seconds);
        if (Double.compare(this.filterReleaseTime, clamped) != 0) {
            this.filterReleaseTime = clamped;
            this.filterEnvDirty.set(true);
        }
    }

    public void setAmpAttackTime(double seconds) {
        double clamped = Math.max(0.0, seconds);
        if (Double.compare(this.ampAttackTime, clamped) != 0) {
            this.ampAttackTime = clamped;
            this.ampEnvDirty.set(true);
        }
    }

    public void setAmpDecayTime(double seconds) {
        double clamped = Math.max(0.0, seconds);
        if (Double.compare(this.ampDecayTime, clamped) != 0) {
            this.ampDecayTime = clamped;
            this.ampEnvDirty.set(true);
        }
    }

    public void setAmpSustainLevel(double level) {
        double clamped = Math.max(0.0, Math.min(1.0, level));
        if (Double.compare(this.ampSustainLevel, clamped) != 0) {
            this.ampSustainLevel = clamped;
            this.ampEnvDirty.set(true);
        }
    }

    public void setAmpReleaseTime(double seconds) {
        double clamped = Math.max(0.0, seconds);
        if (Double.compare(this.ampReleaseTime, clamped) != 0) {
            this.ampReleaseTime = clamped;
            this.ampEnvDirty.set(true);
        }
    }

    public void setPreFilterGainDB(double db) {
        if (Double.compare(this.preFilterGainDB, db) != 0) {
            this.preFilterGainDB = db;
            this.gainDirty.set(true);
        }
    }

    public void setPostFilterGainDB(double db) {
        if (Double.compare(this.postFilterGainDB, db) != 0) {
            this.postFilterGainDB = db;
            this.gainDirty.set(true);
        }
    }

    public void setLFOFrequency(double frequency) {
        this.LFOFrequency = Math.max(0.0, frequency);
    }

    public void setPanDepth(double depth) {
        double clamped = Math.max(0.0, Math.min(1.0, depth));
        if (Double.compare(this.panDepth, clamped) != 0) {
            this.panDepth = clamped;
            this.panDirty.set(true);
        }
    }

    public void setMasterVolume(double volumeScalar){
        this.masterVolumeScalar = volumeScalar;
        this.volumeAttenuation = this.voiceSumAttenuation * volumeScalar;
    }

    /**
     * Applies all current patch settings to all voices. Hook for potential future patch loading system.
     */
    public void applyPatch(){
        synchronized (voices) {
            setOscillatorWaveform(this.waveform);
            setLFOWaveform(this.LFOWaveForm);
            for(int i = 0; i < voices.length; i++){
                setVoiceParams(voices[i]);
            }
            setLFOFrequency(this.LFOFrequency);
        }
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
    public double getMasterVolumeScalar() { return masterVolumeScalar; }

    /**
     * Fills the provided array with active notes.
     * @param activeNotes The array to fill.
     * @return The number of active notes written to the array.
     */
    public int getActiveNotes(byte[] activeNotes) {
        int count = 0;
        synchronized (voices) {
            for (int i = 0; i < voices.length; i++) {
                if (count >= activeNotes.length) {
                    break;
                }
                if (voices[i].isActiveNoRelease()) {
                    activeNotes[count++] = voices[i].getPitchMIDI();
                }
            }
        }
        return count;
    }

    /**
     * Gets the current stereo pan position based on the LFO.
     * @return The pan position, ranging from -1.0 (left) to 1.0 (right).
     */
    public double getPanPosition(){
        return (this.LFOPosition * this.panDepth);
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
        setOscillatorWaveform(waveform);
        setLFOWaveform(LFOWaveForm);
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

        synchronized (voices) {
            Voice targetVoice = null;

            // Find an inactive voice
            for (Voice voice : voices) {
                if (!voice.isActive()) {
                    targetVoice = voice;
                    break;
                }
            }

            // If all voices are active, find the oldest one to steal
            if (targetVoice == null) {
                targetVoice = voices[0];
                for (int i = 1; i < voices.length; i++) {
                    if (voices[i].getNoteOnTime() < targetVoice.getNoteOnTime()) {
                        targetVoice = voices[i];
                    }
                }
            }

            // Apply Settings to Target Voice
            targetVoice.setOscillatorPitch(pitchMIDI);
            targetVoice.setVelocity(velocity);
            setVoiceParams(targetVoice);
            targetVoice.setPanPosition(getPanPosition());
            targetVoice.setNoteOnTime(System.nanoTime());
            targetVoice.noteOn();
        }
    }

    /**
     * Triggers a note-off event for a given MIDI pitch.
     * @param pitchMIDI The MIDI pitch of the note to release.
     */
    public void noteOff(byte pitchMIDI){
        synchronized (voices){
            for (int i = 0; i < voices.length; i++){
                Voice voice = voices[i];
                if(voice.isActive() && (voice.getPitchMIDI() == pitchMIDI)){
                    voice.noteOff();
                }
            }
        }
    }

    /**
     * Syncs LFO oscillator selection and frequency from volatile fields.
     * Must only be called from the audio thread.
     */
    private void syncLfo() {
        switch (this.LFOWaveForm) {
            case SINE -> this.LFO = this.sineLFO;
            case SAW -> this.LFO = this.sawLFO;
            case TRIANGLE -> this.LFO = this.triangleLFO;
            case SQUARE -> this.LFO = this.squareLFO;
        }
        this.LFO.setFrequency(this.LFOFrequency);
    }

    /**
     * Atomically reads and clears dirty flags, then applies changed parameter
     * groups to all voices. Must be called while holding the voices lock.
     */
    private void syncDirtyParamsToVoices() {
        boolean wf = this.waveformDirty.getAndSet(false);
        boolean fi = this.filterDirty.getAndSet(false);
        boolean fe = this.filterEnvDirty.getAndSet(false);
        boolean ae = this.ampEnvDirty.getAndSet(false);
        boolean ga = this.gainDirty.getAndSet(false);
        boolean pa = this.panDirty.getAndSet(false);

        if (wf || fi || fe || ae || ga || pa) {
            Waveform wfSnap = wf ? this.waveform : null;
            double fcSnap = this.filterCutoff, frSnap = this.filterResonance, fmrSnap = this.filterModRange;
            double faSnap = this.filterAttackTime, fdSnap = this.filterDecayTime, fsSnap = this.filterSustainLevel, frTSnap = this.filterReleaseTime;
            double aaSnap = this.ampAttackTime, adSnap = this.ampDecayTime, asSnap = this.ampSustainLevel, arSnap = this.ampReleaseTime;
            double pfgSnap = this.preFilterGainDB, pfgPostSnap = this.postFilterGainDB;
            double pdSnap = this.panDepth;

            for (int i = 0; i < voices.length; i++) {
                if (wf) voices[i].setOscillatorWaveform(wfSnap);
                if (fi) voices[i].setFilterParameters(fcSnap, frSnap, fmrSnap);
                if (fe) voices[i].setFilterEnvelope(faSnap, fdSnap, fsSnap, frTSnap);
                if (ae) voices[i].setAmpEnvelope(aaSnap, adSnap, asSnap, arSnap);
                if (ga) voices[i].setFilterGainStaging(pfgSnap, pfgPostSnap);
                if (pa) voices[i].setPanDepth(pdSnap);
            }
        }
    }

    /**
     * Processes one block of audio samples for all active voices.
     */
    public void processBlock(double[] stereoOutputBuffer){
        // Clear the output buffer
        Arrays.fill(stereoOutputBuffer, 0.0);

        syncLfo();

        // Populate LFO buffer
        LFO.processBlock(null, this.lfoOutputBuffer, blockSize);

        double vol = this.volumeAttenuation;

        // Voice Processing and Mixing
        synchronized (voices) {
            syncDirtyParamsToVoices();

            for (int i = 0; i < voices.length; i++) {
                Voice voice = voices[i];
                if (voice.isActive()) {
                    // If the voice is active, process its block and sum it into the output buffer.
                    voice.processBlock(null, this.voiceOutputBuffer, this.blockSize);
                    for(int j = 0; j < this.blockSize * 2; j++){
                        stereoOutputBuffer[j] += this.voiceOutputBuffer[j] * vol;
                    }
                }
            }
        }

        // Update LFO position once per block (last sample)
        this.LFOPosition = lfoOutputBuffer[blockSize - 1];

        // Hard Clipping
        for (int i = 0; i < blockSize * 2; i++) {
            if (stereoOutputBuffer[i] > 1.0) {
                stereoOutputBuffer[i] = 1.0;
            } else if (stereoOutputBuffer[i] < -1.0) {
                stereoOutputBuffer[i] = -1.0;
            }
        }
    }
    /**
     * Processes one block of audio samples and returns performance timings.
     * @return A map containing the total time taken for each processing stage in nanoseconds.
     */
    public Map<String, Long> processBlockInstrumented(double[] stereoOutputBuffer){
        Map<String, Long> timings = new HashMap<>();
        long startTime, endTime;

        // Clear the output buffer and timings map
        timings.clear();
        Arrays.fill(stereoOutputBuffer, 0.0);

        syncLfo();

        // Populate LFO buffer
        startTime = System.nanoTime();
        LFO.processBlock(null, this.lfoOutputBuffer, blockSize);
        endTime = System.nanoTime();
        timings.merge("LFO", endTime - startTime, Long::sum);

        double vol = this.volumeAttenuation;

        // Voice Processing and Mixing
        startTime = System.nanoTime();
        synchronized (voices) {
            syncDirtyParamsToVoices();

            for (int i = 0; i < voices.length; i++) {
                Voice voice = voices[i];
                if (voice.isActive()) {
                    voice.processBlockInstrumented(this.lfoOutputBuffer, this.voiceOutputBuffer, this.blockSize, timings);
                    for(int j = 0; j < this.blockSize * 2; j++){
                        stereoOutputBuffer[j] += this.voiceOutputBuffer[j] * vol;
                    }
                }
            }
        }
        endTime = System.nanoTime();
        timings.merge("Voice Processing & Mix", endTime - startTime, Long::sum);

        // Update LFO position once per block (last sample)
        this.LFOPosition = lfoOutputBuffer[blockSize - 1];


        // Hard Clipping
        startTime = System.nanoTime();
        for (int i = 0; i < blockSize * 2; i++) {
            if (stereoOutputBuffer[i] > 1.0) {
                stereoOutputBuffer[i] = 1.0;
            } else if (stereoOutputBuffer[i] < -1.0) {
                stereoOutputBuffer[i] = -1.0;
            }
        }
        endTime = System.nanoTime();
        timings.merge("Hard Clipping", endTime - startTime, Long::sum);

        return timings;
    }
}