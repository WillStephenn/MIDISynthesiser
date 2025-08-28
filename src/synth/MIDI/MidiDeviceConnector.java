package synth.MIDI;

import synth.core.Synthesiser;
import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.List;

public class MidiDeviceConnector {

    /**
     * Lists all available MIDI input devices to the console.
     * An input device is one that can send MIDI messages (has at least one transmitter).
     */
    public static void listMidiDevices() {
        System.out.println("--- Available MIDI Input Devices ---");
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        if (infos.length == 0) {
            System.out.println("No MIDI devices found.");
            return;
        }
        for (int i = 0; i < infos.length; i++) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(infos[i]);
                // A device with transmitters is a MIDI input device (e.g., a keyboard, Logic Pro)
                if (device.getMaxTransmitters() != 0) {
                    System.out.println("- " + infos[i].getName());
                }
            } catch (MidiUnavailableException e) {
                // Ignore devices that can't be accessed
            }
        }
        System.out.println("------------------------------------");
    }

    /**
     * Connects the synthesizer to the first MIDI input device found with the specified name.
     * @param synth The Synthesiser instance to connect.
     * @param deviceName The name of the MIDI device to connect to (e.g., "IAC Driver Bus 1").
     * @return The MidiDevice object if connection is successful, otherwise null.
     */
    public static MidiDevice connectToDevice(Synthesiser synth, String deviceName) {
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            if (info.getName().equals(deviceName)) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    // Ensure it's an input device
                    if (device.getMaxTransmitters() != 0) {
                        device.open();
                        Transmitter transmitter = device.getTransmitter();
                        transmitter.setReceiver(new MidiInputHandler(synth));
                        System.out.println("Successfully connected to MIDI device: " + deviceName);
                        return device;
                    }
                } catch (MidiUnavailableException e) {
                    System.err.println("Could not open MIDI device '" + deviceName + "': " + e.getMessage());
                    return null;
                }
            }
        }
        System.err.println("Could not find MIDI device: '" + deviceName + "'");
        return null;
    }
}