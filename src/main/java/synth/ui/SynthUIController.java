package synth.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import synth.MIDI.MidiDeviceConnector;
import synth.core.Synthesiser;
import synth.utils.AudioConstants;
import synth.utils.AudioDeviceConnector;

import javax.sound.midi.MidiDevice;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * Controller for the main synthesiser user interface.
 * Manages the initialisation of the synth, audio/MIDI devices,
 * and binds UI controls to the synthesiser's parameters.
 */
public class SynthUIController implements Initializable {

    private Synthesiser synth;
    private SourceDataLine line;
    private MidiDevice midiDevice;
    private Thread audioThread;

    // FXML UI Components
    @FXML private ChoiceBox<String> midiDeviceChoiceBox;
    @FXML private ChoiceBox<String> audioDeviceChoiceBox;
    @FXML private ChoiceBox<Synthesiser.Waveform> waveformChoiceBox;
    @FXML private ChoiceBox<Synthesiser.Waveform> lfoWaveformChoiceBox;
    @FXML private Slider lfoFrequencySlider;
    @FXML private Slider filterCutoffSlider;
    @FXML private Slider filterResonanceSlider;
    @FXML private Slider filterModRangeSlider;
    @FXML private Slider ampAttackSlider;
    @FXML private Slider ampDecaySlider;
    @FXML private Slider ampSustainSlider;
    @FXML private Slider ampReleaseSlider;

    /**
     * Called by JavaFX to initialise the controller after its root element has been processed.
     * @param location The location used to resolve relative paths for the root object, or null.
     * @param resources The resources used to localise the root object, or null.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.synth = new Synthesiser(
                AudioConstants.NUMBER_OF_VOICES,
                AudioConstants.SAMPLE_RATE,
                AudioConstants.BLOCK_SIZE
        );

        setupDeviceSelectors();
        setupControls();
    }

    /**
     * Populates the device selection dropdowns and sets up listeners to handle changes.
     */
    private void setupDeviceSelectors() {
        // Audio Devices
        ArrayList<String> audioDevices = AudioDeviceConnector.getAudioOutputDeviceList();
        audioDeviceChoiceBox.setItems(FXCollections.observableArrayList(audioDevices));
        audioDeviceChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldDevice, newDevice) -> {
            if (newDevice != null) {
                changeAudioDevice(newDevice);
            }
        });

        // MIDI Devices
        ArrayList<String> midiDevices = MidiDeviceConnector.getMidiDevicesList();
        midiDeviceChoiceBox.setItems(FXCollections.observableArrayList(midiDevices));
        midiDeviceChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldDevice, newDevice) -> {
            if (newDevice != null) {
                changeMidiDevice(newDevice);
            }
        });

        // Select the first device by default to kick things off
        if (!audioDevices.isEmpty()) {
            audioDeviceChoiceBox.getSelectionModel().selectFirst();
        }
        if (!midiDevices.isEmpty()) {
            midiDeviceChoiceBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * Binds all UI controls (sliders, choice boxes) to their corresponding synthesiser parameters.
     */
    private void setupControls() {
        waveformChoiceBox.setItems(FXCollections.observableArrayList(Synthesiser.Waveform.values()));
        waveformChoiceBox.setValue(synth.getWaveform());
        waveformChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) synth.setWaveform(n);
        });

        lfoWaveformChoiceBox.setItems(FXCollections.observableArrayList(Synthesiser.Waveform.values()));
        lfoWaveformChoiceBox.setValue(synth.getLFOWaveform());
        lfoWaveformChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) synth.setLFOWaveform(n);
        });

        lfoFrequencySlider.valueProperty().addListener((obs, o, n) -> synth.setLFOFrequency(n.doubleValue()));
        filterCutoffSlider.valueProperty().addListener((obs, o, n) -> synth.setFilterCutoff(n.doubleValue()));
        filterResonanceSlider.valueProperty().addListener((obs, o, n) -> synth.setFilterResonance(n.doubleValue()));
        filterModRangeSlider.valueProperty().addListener((obs, o, n) -> synth.setFilterModRange(n.doubleValue()));
        ampAttackSlider.valueProperty().addListener((obs, o, n) -> synth.setAmpAttackTime(n.doubleValue()));
        ampDecaySlider.valueProperty().addListener((obs, o, n) -> synth.setAmpDecayTime(n.doubleValue()));
        ampSustainSlider.valueProperty().addListener((obs, o, n) -> synth.setAmpSustainLevel(n.doubleValue()));
        ampReleaseSlider.valueProperty().addListener((obs, o, n) -> synth.setAmpReleaseTime(n.doubleValue()));
    }

    /**
     * Changes the active audio output device.
     * @param deviceName The name of the new audio device.
     */
    private void changeAudioDevice(String deviceName) {
        if (audioThread != null) {
            audioThread.interrupt();
        }
        if (line != null) {
            line.close();
        }

        AudioFormat audioFormat = new AudioFormat((float) AudioConstants.SAMPLE_RATE, 16, 2, true, true);
        try {
            line = AudioDeviceConnector.getOutputLine(deviceName, audioFormat);
            if (line != null) {
                line.open(audioFormat, AudioConstants.BUFFER_SIZE);
                line.start();
                startAudioProcessingThread();
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * Changes the active MIDI input device.
     * @param deviceName The name of the new MIDI device.
     */
    private void changeMidiDevice(String deviceName) {
        if (midiDevice != null && midiDevice.isOpen()) {
            midiDevice.close();
        }
        midiDevice = MidiDeviceConnector.connectToDevice(synth, deviceName);
    }

    /**
     * Starts the main audio processing loop on a separate, non-UI thread.
     */
    private void startAudioProcessingThread() {
        audioThread = new Thread(() -> {
            double[] audioBlock = new double[AudioConstants.BLOCK_SIZE * 2];
            byte[] buffer = new byte[AudioConstants.BLOCK_SIZE * 4];

            while (!Thread.currentThread().isInterrupted()) {
                synth.processBlock(audioBlock);
                for (int i = 0; i < AudioConstants.BLOCK_SIZE; i++) {
                    short pcmLeft = (short) (audioBlock[i * 2] * Short.MAX_VALUE);
                    buffer[i * 4] = (byte) (pcmLeft >> 8);
                    buffer[i * 4 + 1] = (byte) pcmLeft;
                    short pcmRight = (short) (audioBlock[i * 2 + 1] * Short.MAX_VALUE);
                    buffer[i * 4 + 2] = (byte) (pcmRight >> 8);
                    buffer[i * 4 + 3] = (byte) pcmRight;
                }
                line.write(buffer, 0, buffer.length);
            }
        });
        audioThread.setDaemon(true);
        audioThread.start();
    }

    /**
     * Safely closes audio and MIDI resources when the application exits.
     */
    public void shutdown() {
        if (audioThread != null) {
            audioThread.interrupt();
        }
        if (line != null) {
            line.stop();
            line.close();
        }
        if (midiDevice != null && midiDevice.isOpen()) {
            midiDevice.close();
        }
        Platform.exit();
    }
}