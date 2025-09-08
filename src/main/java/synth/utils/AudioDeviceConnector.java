package synth.utils;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * A utility class for listing and connecting to available audio output devices.
 */
public class AudioDeviceConnector {

    /**
     * Lists all available audio output devices to the console.
     * An output device is a mixer that can provide a SourceDataLine.
     * @return An ArrayList of strings, where each string is the name of an available device.
     */
    public static ArrayList<String> getAudioOutputDeviceList() {
        ArrayList<String> devices = new ArrayList<>();
        System.out.println("--- Select Audio Output Device ---");
        int i = 1;
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            // Filter to skip devices starting with "Port "
            if (info.getName().startsWith("Port ")) {
                continue;
            }
            try {
                Mixer mixer = AudioSystem.getMixer(info);
                // Check if the mixer supports output (SourceDataLine)
                if (mixer.getSourceLineInfo().length > 0) {
                    devices.add(info.getName());
                    System.out.println(i + "- " + info.getName());
                    i++;
                }
            } catch (Exception e) {
                // Ignore devices that can't be accessed
            }
        }
        System.out.println("------------------------------------");
        return devices;
    }

    /**
     * Prompts the user to select an audio output device.
     *
     * @return The name of the selected audio device, or null if none are found.
     */
    public static String promptUser() {
        ArrayList<String> devices = getAudioOutputDeviceList();
        if (devices.isEmpty()) {
            System.out.println("No audio output devices available.");
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
     * Gets a SourceDataLine from a named audio device.
     *
     * If the named device is not found or cannot be opened, it falls back
     * to the system's default SourceDataLine.
     *
     * @param deviceName The name of the desired audio output device.
     * @param format The AudioFormat to be used by the line.
     * @return An opened SourceDataLine ready for use, or null if an error occurs.
     */
    public static SourceDataLine getOutputLine(String deviceName, AudioFormat format) {
        SourceDataLine line = null;
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(deviceName)) {
                try {
                    Mixer mixer = AudioSystem.getMixer(info);
                    if (mixer.isLineSupported(new DataLine.Info(SourceDataLine.class, format))) {
                        line = (SourceDataLine) mixer.getLine(new DataLine.Info(SourceDataLine.class, format));
                        System.out.println("Successfully connected to audio device: " + deviceName);
                        return line;
                    }
                } catch (LineUnavailableException e) {
                    System.err.println("Could not open audio device '" + deviceName + "': " + e.getMessage());
                    // Fall through to default device
                }
            }
        }

        // If the named device wasn't found or failed, try getting the default line
        try {
            System.err.println("Audio device '" + deviceName + "' not found or failed to open. Using default device.");
            line = AudioSystem.getSourceDataLine(format);
            System.out.println("Successfully connected to default audio device.");
            return line;
        } catch (LineUnavailableException e) {
            System.err.println("Could not get default audio line.");
            e.printStackTrace();
            return null;
        }
    }
}