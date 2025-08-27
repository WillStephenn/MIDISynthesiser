package synth.core;

import synth.utils.AudioConstants;
import synth.core.Voice;
import java.util.ArrayList;

public class Synthesiser implements AudioComponent{
    // Control all the voices. Bundles them up in an arraylist ready to be shipped to the buffer.
    private ArrayList<Voice> voices;
    private final double noVoices;
    private final double sampleRate;

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
    protected double filterAttackTime;
    protected double filterDecayTime;
    protected double filterSustainLevel;
    protected double filterReleaseTime;

    // Amp Envelope
    protected double ampAttackTime;
    protected double ampDecayTime;
    protected double ampSustainLevel;
    protected double ampReleaseTime;

    // Constructor
    public Synthesiser(double noVoices) {
        this.noVoices = noVoices;
        this.sampleRate = AudioConstants.SAMPLE_RATE; // Point of dependency injection for entire voice stack
        this.voices = new ArrayList<Voice>();

        // Default Synth Patch
        loadPatch( // Applies default patch and populates the voice bank
                Waveform.SAW, // Waveform
                500, // filterCutoff
                5,     // filterResonance
                2000.0,     // filterModAmount (Hz)
                0.01,    // filterAttackTime
                0.4,     // filterDecayTime
                0.5,     // filterSustainLevel
                0.4,     // filterReleaseTime
                0.005,   // ampAttackTime
                0.3,     // ampDecayTime
                0.5,     // ampSustainLevel
                0.5      // ampReleaseTime
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
                          double ampReleaseTime) {

        // Store the master settings for the synth
        updateWaveForm(waveform); // Clears and re-populates voice bank with new waveform
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
        applyPatch();
    }

    public void updateWaveForm(Waveform waveform){
        // Scrap all voices and repopulate the voice bank
        if (this.waveform != waveform){
            this.waveform = waveform;
            voices.clear();
            for (int i = 0; i < this.noVoices; i++) {
                voices.add(new Voice(this.waveform, 0, this.sampleRate));
            }
        }
    }

    public void applyPatch(){
        for(Voice voice : voices){
            voice.setAmpEnvelope(this.ampAttackTime, this.ampDecayTime, this.ampSustainLevel, this.ampReleaseTime);
            voice.setFilterEnvelope(this.filterAttackTime, this.filterDecayTime, this.filterSustainLevel, this.filterReleaseTime);
            voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModAmount);
        }
    }

    public void noteOn(byte pitchMIDI, double velocity) {
        for (Voice voice : voices) {
            if (!voice.isActive()) { // Find first inactive voice
                // Apply Patch
                voice.setOscillatorPitch(pitchMIDI);
                voice.setVelocity(velocity);
                voice.setAmpEnvelope(this.ampAttackTime, this.ampDecayTime, this.ampSustainLevel, this.ampReleaseTime);
                voice.setFilterEnvelope(this.filterAttackTime, this.filterDecayTime, this.filterSustainLevel, this.filterReleaseTime);
                voice.setFilterParameters(this.filterCutoff, this.filterResonance, this.filterModAmount);
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

    public double processSample(double input){
        double sampleMixedSum = 0.0;
        double voiceVolume = 0.5;

        for(Voice voice:voices){
            if(voice.isActive()){
                sampleMixedSum += (voiceVolume * voice.processSample(0.0));
            }
        }

        // Hard Clipping
        if (sampleMixedSum > 1.0) {
            sampleMixedSum = 1.0;
        } else if (sampleMixedSum < -1.0) {
            sampleMixedSum = -1.0;
        }

        return sampleMixedSum;
    }

}

