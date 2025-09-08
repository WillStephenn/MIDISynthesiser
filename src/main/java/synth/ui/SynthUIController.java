package synth.ui;

import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;

import javax.sound.midi.MidiDevice;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import synth.midi.MidiDeviceConnector;
import synth.core.Synthesiser;
import synth.utils.AudioConstants;
import synth.utils.AudioDeviceConnector;

/**
 * Controller for the main synthesiser user interface.
 * Manages the initialisation of the synth, audio/MIDI devices,
 * and binds UI controls to the synthesiser's parameters.
 * 
 * June's Logue - Modern/Vintage Teracotta Theme
 */
public class SynthUIController implements Initializable {

    private Synthesiser synth;
    private SourceDataLine line;
    private MidiDevice midiDevice;
    private Thread audioThread;
    
    // Formatters for parameter readouts
    private final DecimalFormat frequencyFormat = new DecimalFormat("0.0");
    private final DecimalFormat timeFormat = new DecimalFormat("0.000");
    private final DecimalFormat levelFormat = new DecimalFormat("0.00");
    private final DecimalFormat integerFormat = new DecimalFormat("0");
    private final DecimalFormat decibelsFormat = new DecimalFormat("0.0");
    private final DecimalFormat percentFormat = new DecimalFormat("0");

    // FXML UI Components - Device Selection
    @FXML private ChoiceBox<String> midiDeviceChoiceBox;
    @FXML private ChoiceBox<String> audioDeviceChoiceBox;
    
    // FXML UI Components - Oscillator & LFO
    @FXML private ChoiceBox<Synthesiser.Waveform> waveformChoiceBox;
    @FXML private ChoiceBox<Synthesiser.Waveform> lfoWaveformChoiceBox;
    @FXML private Slider lfoFrequencySlider;
    @FXML private Label lfoFrequencyLabel;
    
    // FXML UI Components - Filter
    @FXML private Slider filterCutoffSlider;
    @FXML private Label filterCutoffLabel;
    @FXML private Slider filterResonanceSlider;
    @FXML private Label filterResonanceLabel;
    @FXML private Slider filterModRangeSlider;
    @FXML private Label filterModRangeLabel;
    
    // FXML UI Components - Amp Envelope
    @FXML private Slider ampAttackSlider;
    @FXML private Label ampAttackLabel;
    @FXML private Slider ampDecaySlider;
    @FXML private Label ampDecayLabel;
    @FXML private Slider ampSustainSlider;
    @FXML private Label ampSustainLabel;
    @FXML private Slider ampReleaseSlider;
    @FXML private Label ampReleaseLabel;
    
    // FXML UI Components - Filter Envelope
    @FXML private Slider filterAttackSlider;
    @FXML private Label filterAttackLabel;
    @FXML private Slider filterDecaySlider;
    @FXML private Label filterDecayLabel;
    @FXML private Slider filterSustainSlider;
    @FXML private Label filterSustainLabel;
    @FXML private Slider filterReleaseSlider;
    @FXML private Label filterReleaseLabel;
    
    // FXML UI Components - Global Controls
    @FXML private Slider masterVolumeSlider;
    @FXML private Label masterVolumeLabel;
    @FXML private Slider panDepthSlider;
    @FXML private Label panDepthLabel;
    @FXML private Slider preFilterGainSlider;
    @FXML private Label preFilterGainLabel;
    @FXML private Slider postFilterGainSlider;
    @FXML private Label postFilterGainLabel;
    
    // Track master volume since synth doesn't have a getter
    private double currentMasterVolume = 1.0;

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
        syncUIWithSynthSettings();
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
     * Binds all UI controls (sliders, choice boxes) to their corresponding synthesiser parameters
     * and sets up real-time parameter readouts.
     */
    private void setupControls() {
        // Oscillator Controls
        waveformChoiceBox.setItems(FXCollections.observableArrayList(Synthesiser.Waveform.values()));
        waveformChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) synth.setWaveform(n);
        });

        // LFO Controls
        lfoWaveformChoiceBox.setItems(FXCollections.observableArrayList(Synthesiser.Waveform.values()));
        lfoWaveformChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) synth.setLFOWaveform(n);
        });

        setupLFOControls();
        setupFilterControls();
        setupAmpEnvelopeControls();
        setupFilterEnvelopeControls();
        setupGlobalControls();
    }
    
    /**
     * Sets up LFO frequency controls with real-time readouts.
     */
    private void setupLFOControls() {
        lfoFrequencySlider.valueProperty().addListener((obs, o, n) -> {
            double freq = n.doubleValue();
            synth.setLFOFrequency(freq);
            lfoFrequencyLabel.setText(frequencyFormat.format(freq) + " Hz");
        });
    }
    
    /**
     * Sets up filter controls with real-time readouts.
     */
    private void setupFilterControls() {
        filterCutoffSlider.valueProperty().addListener((obs, o, n) -> {
            double cutoff = n.doubleValue();
            synth.setFilterCutoff(cutoff);
            filterCutoffLabel.setText(integerFormat.format(cutoff) + " Hz");
        });
        
        filterResonanceSlider.valueProperty().addListener((obs, o, n) -> {
            double resonance = n.doubleValue();
            synth.setFilterResonance(resonance);
            filterResonanceLabel.setText(frequencyFormat.format(resonance));
        });
        
        filterModRangeSlider.valueProperty().addListener((obs, o, n) -> {
            double modRange = n.doubleValue();
            synth.setFilterModRange(modRange);
            filterModRangeLabel.setText(integerFormat.format(modRange) + " Hz");
        });
    }
    
    /**
     * Sets up amplitude envelope controls with real-time readouts.
     */
    private void setupAmpEnvelopeControls() {
        ampAttackSlider.valueProperty().addListener((obs, o, n) -> {
            double attack = n.doubleValue();
            synth.setAmpAttackTime(attack);
            ampAttackLabel.setText(timeFormat.format(attack) + " s");
        });
        
        ampDecaySlider.valueProperty().addListener((obs, o, n) -> {
            double decay = n.doubleValue();
            synth.setAmpDecayTime(decay);
            ampDecayLabel.setText(timeFormat.format(decay) + " s");
        });
        
        ampSustainSlider.valueProperty().addListener((obs, o, n) -> {
            double sustain = n.doubleValue();
            synth.setAmpSustainLevel(sustain);
            ampSustainLabel.setText(levelFormat.format(sustain));
        });
        
        ampReleaseSlider.valueProperty().addListener((obs, o, n) -> {
            double release = n.doubleValue();
            synth.setAmpReleaseTime(release);
            ampReleaseLabel.setText(timeFormat.format(release) + " s");
        });
    }
    
    /**
     * Sets up filter envelope controls with real-time readouts.
     */
    private void setupFilterEnvelopeControls() {
        filterAttackSlider.valueProperty().addListener((obs, o, n) -> {
            double attack = n.doubleValue();
            synth.setFilterAttackTime(attack);
            filterAttackLabel.setText(timeFormat.format(attack) + " s");
        });
        
        filterDecaySlider.valueProperty().addListener((obs, o, n) -> {
            double decay = n.doubleValue();
            synth.setFilterDecayTime(decay);
            filterDecayLabel.setText(timeFormat.format(decay) + " s");
        });
        
        filterSustainSlider.valueProperty().addListener((obs, o, n) -> {
            double sustain = n.doubleValue();
            synth.setFilterSustainLevel(sustain);
            filterSustainLabel.setText(levelFormat.format(sustain));
        });
        
        filterReleaseSlider.valueProperty().addListener((obs, o, n) -> {
            double release = n.doubleValue();
            synth.setFilterReleaseTime(release);
            filterReleaseLabel.setText(timeFormat.format(release) + " s");
        });
    }
    
    /**
     * Sets up global controls with real-time readouts.
     */
    private void setupGlobalControls() {
        masterVolumeSlider.valueProperty().addListener((obs, o, n) -> {
            double volume = n.doubleValue();
            currentMasterVolume = volume;
            synth.setMasterVolume(volume);
            masterVolumeLabel.setText(percentFormat.format(volume * 100) + "%");
        });
        
        panDepthSlider.valueProperty().addListener((obs, o, n) -> {
            double panDepth = n.doubleValue();
            synth.setPanDepth(panDepth);
            panDepthLabel.setText(levelFormat.format(panDepth));
        });
        
        preFilterGainSlider.valueProperty().addListener((obs, o, n) -> {
            double gain = n.doubleValue();
            synth.setPreFilterGainDB(gain);
            preFilterGainLabel.setText(decibelsFormat.format(gain) + " dB");
        });
        
        postFilterGainSlider.valueProperty().addListener((obs, o, n) -> {
            double gain = n.doubleValue();
            synth.setPostFilterGainDB(gain);
            postFilterGainLabel.setText(decibelsFormat.format(gain) + " dB");
        });
    }

    /**
     * Syncs the UI controls with the current synthesiser settings.
     * This pulls the current patch values and updates all UI elements accordingly.
     */
    private void syncUIWithSynthSettings() {
        // Oscillator settings
        waveformChoiceBox.setValue(synth.getWaveform());
        
        // LFO settings
        lfoWaveformChoiceBox.setValue(synth.getLFOWaveform());
        lfoFrequencySlider.setValue(synth.getLFOFrequency());
        
        // Filter settings
        filterCutoffSlider.setValue(synth.getFilterCutoff());
        filterResonanceSlider.setValue(synth.getFilterResonance());
        filterModRangeSlider.setValue(synth.getFilterModRange());
        
        // Amp Envelope settings
        ampAttackSlider.setValue(synth.getAmpAttackTime());
        ampDecaySlider.setValue(synth.getAmpDecayTime());
        ampSustainSlider.setValue(synth.getAmpSustainLevel());
        ampReleaseSlider.setValue(synth.getAmpReleaseTime());
        
        // Filter Envelope settings
        filterAttackSlider.setValue(synth.getFilterAttackTime());
        filterDecaySlider.setValue(synth.getFilterDecayTime());
        filterSustainSlider.setValue(synth.getFilterSustainLevel());
        filterReleaseSlider.setValue(synth.getFilterReleaseTime());
        
        // Global Control settings
        masterVolumeSlider.setValue(currentMasterVolume);
        panDepthSlider.setValue(synth.getPanDepth());
        preFilterGainSlider.setValue(synth.getPreFilterGainDB());
        postFilterGainSlider.setValue(synth.getPostFilterGainDB());
        
        // Update all readout labels
        updateAllReadouts();
    }
    
    /**
     * Updates all parameter readout labels with current values.
     */
    private void updateAllReadouts() {
        lfoFrequencyLabel.setText(frequencyFormat.format(lfoFrequencySlider.getValue()) + " Hz");
        filterCutoffLabel.setText(integerFormat.format(filterCutoffSlider.getValue()) + " Hz");
        filterResonanceLabel.setText(frequencyFormat.format(filterResonanceSlider.getValue()));
        filterModRangeLabel.setText(integerFormat.format(filterModRangeSlider.getValue()) + " Hz");
        
        ampAttackLabel.setText(timeFormat.format(ampAttackSlider.getValue()) + " s");
        ampDecayLabel.setText(timeFormat.format(ampDecaySlider.getValue()) + " s");
        ampSustainLabel.setText(levelFormat.format(ampSustainSlider.getValue()));
        ampReleaseLabel.setText(timeFormat.format(ampReleaseSlider.getValue()) + " s");
        
        filterAttackLabel.setText(timeFormat.format(filterAttackSlider.getValue()) + " s");
        filterDecayLabel.setText(timeFormat.format(filterDecaySlider.getValue()) + " s");
        filterSustainLabel.setText(levelFormat.format(filterSustainSlider.getValue()));
        filterReleaseLabel.setText(timeFormat.format(filterReleaseSlider.getValue()) + " s");
        
        masterVolumeLabel.setText(percentFormat.format(masterVolumeSlider.getValue() * 100) + "%");
        panDepthLabel.setText(levelFormat.format(panDepthSlider.getValue()));
        preFilterGainLabel.setText(decibelsFormat.format(preFilterGainSlider.getValue()) + " dB");
        postFilterGainLabel.setText(decibelsFormat.format(postFilterGainSlider.getValue()) + " dB");
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