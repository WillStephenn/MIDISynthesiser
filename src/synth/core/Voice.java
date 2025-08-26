package synth.core;

import synth.components.Envelope;
import synth.components.filters.ResonantLowPassFilter;
import synth.components.oscillators.Oscillator;
import synth.components.oscillators.SawOscillator;
import synth.components.oscillators.SineOscillator;
import synth.components.oscillators.TriangleOscillator;

public class Voice {
    // The voice object encapsulates all of the audio components into one:
    // Oscillator -> Amp Envelope -> Resonant Low Pass Filter
    // It provides setters for all encapsulated elements, acting as a facade.

    // Audio Component Objects
    private final Oscillator oscillator;
    private final ResonantLowPassFilter filter;
    public final Envelope ampEnvelope;
    private final Envelope filterEnvelope;

    // Oscillator Settings
    public enum Waveform{
        SINE, SAW, TRIANGLE
    }
    private double pitchFrequency;

    // Filter settings
    private double filterCutoff;
    private double filterResonance;
    private double filterModAmount;

    //Constructor
    public Voice (Waveform waveform, double pitchFrequency, double sampleRate){
        // Filter Defaults
        this.filterCutoff = 20000;
        this.filterResonance = 1;
        this.filterModAmount = 500;

        // Audio Components
        this.filter = new ResonantLowPassFilter(sampleRate);
        this.ampEnvelope = new Envelope(sampleRate);
        this.filterEnvelope = new Envelope(sampleRate);

        // Oscillator switch
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

        // Set Oscillator starting pitch
        this.pitchFrequency = pitchFrequency;
        this.oscillator.setFrequency(this.pitchFrequency);
    }

    // Facade Methods
    // Oscillators:
    public void setOscillatorFrequency(double frequency){
        this.oscillator.setFrequency(frequency);
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

    // Filter:
    public void setFilterParameters(double frequency, double resonance){
        this.filter.setParameters(frequency, resonance);
    }

    public void noteOn(){
        ampEnvelope.noteOn();
        filterEnvelope.noteOn();
    }

    public void noteOff(){
        ampEnvelope.noteOff();
        filterEnvelope.noteOff();
    }

    public double processSample(double input) {
            // Generate next oscillator sample
            double sample = oscillator.processSample(input);

            // Calculate filter modulation based on the filter env values
            double filterEnvValue = filterEnvelope.processSample(1.0);
            double finalCutoff = filterCutoff + (filterEnvValue * filterModAmount);
            filter.setParameters(finalCutoff, this.filterResonance);

            // Process sample through filter
            sample = filter.processSample(sample);

            // Calculate Amp Envelope Value
            return ampEnvelope.processSample(sample);
    }
}
