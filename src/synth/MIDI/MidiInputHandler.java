package synth.MIDI;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import synth.core.Synthesiser;

/**
 * A MIDI receiver that processes incoming MIDI messages and controls a synthesiser.
 * It handles Note On and Note Off events to trigger and release voices.
 */
public class MidiInputHandler implements Receiver{
    private final Synthesiser synth;

    /**
     * Constructs a MidiInputHandler.
     * @param synth The synthesiser to be controlled. Must not be null.
     */
    public MidiInputHandler(Synthesiser synth) {
        if (synth == null) {
            throw new IllegalArgumentException("Synthesiser cannot be null.");
        }
        this.synth = synth;
    }

    /**
     * Processes an incoming MIDI message, sending the control signals to the Synthesiser.
     * @param message The MIDI message to process.
     * @param timeStamp The timestamp of the message.
     */
    @Override
    public void send(MidiMessage message, long timeStamp){
        if(message instanceof ShortMessage sm){

            byte pitch = (byte) sm.getData1();  // Grabs the pitch byte from the midi message
            double velocity = sm.getData2() /127.00; // Grabs the velocity from the midi message and converts it to a scalar

            // Get the command data from the short message and route it appropriately
            if (sm.getCommand() == ShortMessage.NOTE_ON && velocity > 0) {
                // NOTE_ON
                synth.noteOn(pitch, velocity);
            } else if (sm.getCommand() == ShortMessage.NOTE_OFF || (sm.getCommand() == ShortMessage.NOTE_ON && velocity == 0)) {
                // NOTE_OFF
                synth.noteOff(pitch);
            } else if (sm.getCommand() == ShortMessage.CONTROL_CHANGE){
                // CONTROL CHANGE
                int controller = sm.getData1();
                int value = sm.getData2();

                double scaledValue = value / 127.0;

                // Parameter control switch:
                switch (controller) {
                    // --- OSCILLATOR CONTROLS ---
                    case 1: // CC 1: Modulation Wheel, LFO Frequency
                        // Map 0-127 to a useful LFO frequency range (e.g., 0.1Hz to 10Hz)
                        synth.setLFOFrequency(0.1 + (scaledValue * 9.9));
                        break;
                    case 14: // CC 14:Oscillator Waveform
                        // A simple mapping: 0-42 = Sine, 43-85 = Saw, 86-127 = Triangle
                        if (value <= 42) {
                            synth.setWaveform(Synthesiser.Waveform.SINE);
                        } else if (value <= 85) {
                            synth.setWaveform(Synthesiser.Waveform.SAW);
                        } else {
                            synth.setWaveform(Synthesiser.Waveform.TRIANGLE);
                        }
                        break;

                    case 15: // CC 14: LFO Waveform
                        // A simple mapping: 0-42 = Sine, 43-85 = Saw, 86-127 = Triangle
                        if (value <= 42) {
                            synth.setLFOWaveform(Synthesiser.Waveform.SINE);
                        } else if (value <= 85) {
                            synth.setLFOWaveform(Synthesiser.Waveform.SAW);
                        } else {
                            synth.setLFOWaveform(Synthesiser.Waveform.TRIANGLE);
                        }
                        break;

                    // --- FILTER CONTROLS ---
                    case 74: // CC 74: Freq Cutoff  
                        // Logarithmic mapping
                        double minFreq = 20.0;
                        double maxFreq = 20000.0;
                        double newCutoff = minFreq * Math.pow(maxFreq / minFreq, scaledValue);
                        synth.setFilterCutoff(newCutoff);
                        break;
                    case 71: // CC 71: Resonance
                        synth.setFilterResonance(1.0 + (scaledValue * 14.0));
                        break;
                    case 75: // CC 75: Filter Mod Range, from 0 to 10KHz
                        synth.setFilterModRange(scaledValue * 10000.0);
                        break;

                    // --- FILTER ENVELOPE ---
                    case 76: // CC 76:  Filter Attack
                        synth.setFilterAttackTime(scaledValue * 10.0);
                        break;
                    case 77: // CC 77:  Filter Decay
                        synth.setFilterDecayTime(scaledValue * 10.0);
                        break;
                    case 78: // CC 78:  Filter Sustain
                        synth.setFilterSustainLevel(scaledValue);
                        break;
                    case 79: // CC 79:  Filter Release
                        synth.setFilterReleaseTime(scaledValue * 10.0);
                        break;

                    // --- AMPLITUDE ENVELOPE ---
                    case 80: // CC 73: Attack Time
                        synth.setAmpAttackTime(scaledValue * 10.0);
                        break;
                    case 81: // CC 72: Release Time
                        synth.setAmpReleaseTime(scaledValue * 10.0);
                        break;
                    case 82: // CC 81:  Amp Sustain
                        synth.setAmpSustainLevel(scaledValue);
                        break;
                    case 83: // CC 80:  Amp Decay
                        synth.setAmpDecayTime(scaledValue * 10.0);
                        break;

                    // --- GAIN & PANNING ---
                    case 7:  // Maser Volume
                        synth.setMasterVolume(scaledValue);
                        break;
                    case 8: // Pre-Filter Gain)
                        // Map 0-127 to a dB range, e.g., -24dB to +24dB
                        synth.setPreFilterGainDB((scaledValue * 48.0) - 24.0);
                        break;
                    case 9: // Post-Filter Gain)
                        // Map 0-127 to a dB range, e.g., -24dB to +24dB
                        synth.setPreFilterGainDB((scaledValue * 48.0) - 24.0);
                        break;
                    case 10: // Pan Depth
                        // Map 0-127 to pan depth 0.0 to 1.0
                        synth.setPanDepth(scaledValue);
                        break;
                }
            }
        }
    }

    /**
     * Closes the receiver. This implementation does nothing.
     */
    @Override
    public void close() {
        // No resources to release
    }
}
