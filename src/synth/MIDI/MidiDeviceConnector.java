package synth.MIDI;

import synth.core.Synthesiser;
import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Scanner;

public class MidiDeviceConnector {

    /**
     * Lists all available MIDI input devices to the console.
     * An input device is one that can send MIDI messages (has at least one transmitter).
     */
    public static ArrayList<String> getMidiDevicesList() {
        ArrayList<String> Devices = new ArrayList<>();
        System.out.println("--- Select MIDI Input Device ---");
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        if (infos.length == 0) {
            System.out.println("No MIDI devices found.");
            return null;
        }
        int i = 1;
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.getMaxTransmitters() != 0) {
                    Devices.add(info.getName());
                    System.out.println(i + "- " + info.getName());
                    i ++;
                }
            } catch (MidiUnavailableException e) {
            }
        }
        System.out.println("------------------------------------");
        return Devices;
    }

    public static String promptUser(){
        ArrayList<String> Devices = getMidiDevicesList();
        assert Devices != null;
        Scanner scanner = new Scanner(System.in);
        int input = scanner.nextInt();
        System.out.println("Selecting " + Devices.get(input - 1));
        return Devices.get(input - 1);
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