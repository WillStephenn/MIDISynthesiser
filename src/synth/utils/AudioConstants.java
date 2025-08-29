package synth.utils;

public interface AudioConstants {
    double SAMPLE_RATE = 44100.0;
    double CONTROL_RATE = 64;
    double RENDER_RATE = SAMPLE_RATE / (64);
    int NUMBER_OF_VOICES = 12;
}
