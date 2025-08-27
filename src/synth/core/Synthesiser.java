package synth.core;

import synth.components.oscillators.Oscillator;
import synth.utils.AudioConstants;
import synth.core.Voice;
import java.util.ArrayList;

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
    private double filterModAmount;

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
    private Waveform LFO;
    private double LFOFrequency;

    // Panning
    private double panDepth;

    // Constructor
    public Synthesiser(double noVoices) {
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
                2000.0,  // filterModAmount (Hz)
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
                Waveform.SINE, //LFO Waveform
                0.5,     // LFO Frequency
                1        // Pan Depth
        );
    }

    public void loadPatch(Waveform waveform,
                          double filterCutoff,
                          double filterResonance,
                          double filterModAmount,
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
                          Waveform LFO,
                          double LFOFrequency,
                          double panDepth) {

        // Store the master settings for the synth
        updateWaveForm(waveform, LFO); // Clears and re-populates voice bank with new waveform
        this.filterCutoff = filterCutoff;
        this.filterResonance = filterResonance;
        this.filterModAmount = filterModAmount;
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

    public void updateWaveForm(Waveform waveform, Waveform LFO){
        // Scrap all voices and repopulate the voice bank
        if (this.waveform != waveform || this.LFO != LFO){
            this.waveform = waveform;
            this.LFO = LFO;
            voices.clear();
            for (int i = 0; i < this.noVoices; i++) {
                voices.add(new Voice(this.waveform, 0, this.sampleRate, this.LFO, this.controlRate));
            }
        }
    }

    public void applyPatch(){
        for(Voice voice : voices){
            voice.setAmpEnvelope(this.ampAttackTime, this.ampDecayTime, this.ampSustainLevel, this.ampReleaseTime);
            voice.setFilterEnvelope(this.filterAttackTime, this.filterDecayTime, this.filterSustainLevel, this.filterReleaseTime);
            voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModAmount);
            voice.setFilterGainStaging(this.preFilterGainDB, this.postFilterGainDB);
            voice.setPannDepth(this.panDepth);
            voice.setLFOFreq(this.LFOFrequency);
        }
    }

    public void noteOn(byte pitchMIDI, double velocity) {
        // Check if note is already being played and switch it off if it is
        noteOff(pitchMIDI);

        for (Voice voice : voices) {
            if (!voice.isActive()) { // Find first inactive voice
                // Apply Patch
                voice.setOscillatorPitch(pitchMIDI);
                voice.setVelocity(velocity);
                voice.setAmpEnvelope(this.ampAttackTime, this.ampDecayTime, this.ampSustainLevel, this.ampReleaseTime);
                voice.setFilterEnvelope(this.filterAttackTime, this.filterDecayTime, this.filterSustainLevel, this.filterReleaseTime);
                voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModAmount);
                voice.setFilterGainStaging(this.preFilterGainDB, this.postFilterGainDB);
                voice.setPannDepth(this.panDepth);
                voice.setLFOFreq(this.LFOFrequency);
                voice.noteOn();
                return;
            }
        }
    }

    public void noteOff(byte pitchMIDI){
        for (Voice voice : voices){
            if(voice.isActive() && (voice.getPitchMIDI() == pitchMIDI)){
                voice.noteOff();
                return;
            }
        }
    }

    public double[] processSample(){
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

