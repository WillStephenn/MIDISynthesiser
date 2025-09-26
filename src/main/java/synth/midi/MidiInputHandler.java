package synth.midi;

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
                    case 32: // Modulation Wheel, LFO Frequency
                        synth.setLFOFrequency(0.1 + (scaledValue * 9.9));
                        break;
                    case 13: // Oscillator Waveform
                        if (value <= 31) {
                            synth.setOscillatorWaveform(Synthesiser.Waveform.SINE);
                        } else if (value <= 63) {
                            synth.setOscillatorWaveform(Synthesiser.Waveform.SAW);
                        } else if (value <= 95){
                            synth.setOscillatorWaveform(Synthesiser.Waveform.TRIANGLE);
                        } else{
                            synth.setOscillatorWaveform(Synthesiser.Waveform.SQUARE);
                        }
                        break;

                    case 17: // LFO Waveform
                        if (value <= 31) {
                            synth.setLFOWaveform(Synthesiser.Waveform.SINE);
                        } else if (value <= 63) {
                            synth.setLFOWaveform(Synthesiser.Waveform.SAW);
                        } else if (value <= 95){
                            synth.setLFOWaveform(Synthesiser.Waveform.TRIANGLE);
                        } else{
                            synth.setLFOWaveform(Synthesiser.Waveform.SQUARE);
                        }
                        break;

                    // --- FILTER CONTROLS ---
                    case 10: // Freq Cutoff
                        // Logarithmic mapping
                        double minFreq = 20.0;
                        double maxFreq = 20000.0;
                        double newCutoff = minFreq * Math.pow(maxFreq / minFreq, scaledValue);
                        synth.setFilterCutoff(newCutoff);
                        break;
                    case 11: // Resonance
                        synth.setFilterResonance(1.0 + (scaledValue * 14.0));
                        break;
                    case 12: // Filter Mod Range, from 0 to 10KHz
                        synth.setFilterModRange(scaledValue * 10000.0);
                        break;

                    // --- FILTER ENVELOPE ---
                    case 1: // Filter Attack
                        synth.setFilterAttackTime(scaledValue * 10.0);
                        break;
                    case 2: // Filter Decay
                        synth.setFilterDecayTime(scaledValue * 10.0);
                        break;
                    case 3: // Filter Sustain
                        synth.setFilterSustainLevel(scaledValue);
                        break;
                    case 4: // Filter Release
                        synth.setFilterReleaseTime(scaledValue * 10.0);
                        break;

                    // --- AMPLITUDE ENVELOPE ---
                    case 5: // Attack Time
                        synth.setAmpAttackTime(scaledValue * 10.0);
                        break;
                    case 6: // Release Time
                        synth.setAmpReleaseTime(scaledValue * 10.0);
                        break;
                    case 7: // Amp Sustain
                        synth.setAmpSustainLevel(scaledValue);
                        break;
                    case 8: // Amp Decay
                        synth.setAmpDecayTime(scaledValue * 10.0);
                        break;

                    // --- GAIN & PANNING ---
                    case 9:  // Maser Volume
                        synth.setMasterVolume(scaledValue);
                        break;
                    case 14: // Pre-Filter Gain)
                        // Ranges 24dB to +24dB
                        synth.setPreFilterGainDB((scaledValue * 48.0) - 24.0);
                        break;
                    case 15: // Post-Filter Gain)
                        // Ranges 24dB to +24dB
                        synth.setPreFilterGainDB((scaledValue * 48.0) - 24.0);
                        break;
                    case 16: // Pan Depth
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
