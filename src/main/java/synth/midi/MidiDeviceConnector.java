package synth.midi;

import synth.core.Synthesiser;
import javax.sound.midi.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.InputMismatchException;


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

    /**
     * Prompts the user to select a MIDI input device.
     *
     * @return The name of the selected MIDI device, or null if none are found.
     */
    public static String promptUser() {
        ArrayList<String> devices = getMidiDevicesList();
        if (devices == null || devices.isEmpty()) {
            System.out.println("No MIDI devices available.");
            return null;
        }

        Scanner scanner = new Scanner(System.in);
        int input = -1;

        while (true) {
            System.out.print("Enter the number of the device you want to use: ");
            try {
                input = scanner.nextInt();
                if (input >= 1 && input <= devices.size()) {
                    break; // Exit loop if input is valid
                } else {
                    System.out.println("Invalid number. Please enter a number between 1 and " + devices.size() + ".");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.next();
            }
        }

        String selectedDevice = devices.get(input - 1);
        System.out.println("Selecting " + selectedDevice);
        return selectedDevice;
    }


    /**
     * Connects the synthesiser to the first MIDI input device found with the specified name.
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