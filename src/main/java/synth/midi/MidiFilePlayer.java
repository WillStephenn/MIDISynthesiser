package synth.midi;


import java.io.File;
import java.io.IOException;
import javax.sound.midi.*;
import synth.core.Synthesiser;

/**
 * Handles playback of MIDI files, sending MIDI events to a synthesiser.
 */
public class MidiFilePlayer {
    private final Synthesiser synth;

    /**
     * Constructs a MidiFilePlayer.
     * @param synth The synthesiser that will play the MIDI notes. Must not be null.
     */
    public MidiFilePlayer(Synthesiser synth) {
        if (synth == null) {
            throw new IllegalArgumentException("Synthesiser cannot be null.");
        }
        this.synth = synth;
    }

    /**
     * Plays a MIDI file from the given file path.
     * @param filePath The path to the MIDI file. Must not be null or empty.
     * @return The Sequencer instance used for playback, or null if an error occurs.
     */
    public Sequencer playMidiFile(String filePath){
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("Error: MIDI file path is null or empty.");
            return null;
        }
        try{
            // Load Midi File
            File midiFile = new File(filePath);
            if (!midiFile.exists()) {
                System.err.println("Error: MIDI file not found at " + filePath);
                return null;
            }
            Sequence sequence = MidiSystem.getSequence(midiFile);

            // Create a sequencer and open it. False indicates no connected midi device.
            Sequencer sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.setSequence(sequence);

            // Connected the sequencer to the receiver via a transmitter
            Transmitter transmitter = sequencer.getTransmitter();
            Receiver receiver = new MidiInputHandler(synth);
            transmitter.setReceiver(receiver);

            System.out.println("Playing MIDI file: " + filePath);
            sequencer.start();
            return sequencer;

        } catch (InvalidMidiDataException | IOException | MidiUnavailableException e) {
            System.err.println("Error playing MIDI file: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
