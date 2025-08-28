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
     * Processes an incoming MIDI message.
     * @param message The MIDI message to process.
     * @param timeStamp The timestamp of the message.
     */
    @Override
    public void send(MidiMessage message, long timeStamp){
        if(message instanceof ShortMessage sm){

            byte pitch = (byte) sm.getData1();  // Grabs the pitch byte from the midi message
            double velocity = sm.getData2() /127.00; // Grabs the velocity from the midi message and converts it to a scalar

            // Trigger the Synth
            if (sm.getCommand() == ShortMessage.NOTE_ON && velocity > 0) {
                synth.noteOn(pitch, velocity);
            } else if (sm.getCommand() == ShortMessage.NOTE_OFF || (sm.getCommand() == ShortMessage.NOTE_ON && velocity == 0)) {
                synth.noteOff(pitch);
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
