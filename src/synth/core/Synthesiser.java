package synth.core;

import synth.utils.AudioConstants;

import java.util.ArrayList;

public class Synthesiser {
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
        this.noVoices = 16;
        this.sampleRate = AudioConstants.SAMPLE_RATE; // Point of dependency injection for entire voice stack
        this.voices = new ArrayList<Voice>();

        // Populate the synthesiser with inactive voices
        for (int i = 0; i < this.noVoices; i++) {
            voices.add(new Voice(Waveform.SAW, 0, this.sampleRate));
        }

        // Default Synth Patch
        loadPatch(
                Waveform.SAW, // Waveform
                20000.0, // filterCutoff: filter is wide open
                0.1,     // filterResonance: low resonance
                0.0,     // filterModAmount: no envelope modulation on the filter
                0.01,    // filterAttackTime: fast attack
                0.4,     // filterDecayTime: medium decay
                0.0,     // filterSustainLevel: no sustain
                0.4,     // filterReleaseTime: medium release
                0.005,   // ampAttackTime: very fast attack for a percussive start
                0.3,     // ampDecayTime: quick decay
                0.2,     // ampSustainLevel: low sustain level
                0.3      // ampReleaseTime: quick release
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
        this.waveform = waveform;
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
    }

    public void noteOn(double pitchFrequency, double velocity) {
        for (Voice voice : voices) {
            if (!voice.isActive()) { // Find first inactive voice
                // Apply Patch
                voice.setOscillatorFrequency(pitchFrequency);
                voice.setVelocity(velocity);
                voice.setAmpEnvelope(this.ampAttackTime, this.ampDecayTime, this.ampSustainLevel, this.ampReleaseTime);
                voice.setFilterEnvelope(this.filterAttackTime, this.filterDecayTime, this.filterSustainLevel, this.filterReleaseTime);
                voice.setFilterParameters(this.filterCutoff, this.filterResonance);
                voice.noteOn();
            }
        }
    }
/*
    public void noteOff(double pitchFrequency){
        for (Voice voice : voices){
            if(pitchFrequency == voice.getOscillatorFrequency()){
                voice.noteOff();
                boolean wasRemoved = voices.remove(voice);
                if (wasRemoved) {
                    System.out.println("Voice was successfully removed.");
                } else {
                    System.out.println("Voice was not found in the list.");
                }

            }

        }
    }
*/
}