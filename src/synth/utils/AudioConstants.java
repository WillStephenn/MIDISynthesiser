package synth.utils;

public interface AudioConstants {
    double SAMPLE_RATE = 44100.0;
    double RENDER_RATE = 999999;
    int BUFFER_SIZE =  1024;
    int BLOCK_SIZE = 128;
    int NUMBER_OF_VOICES = 32;
    int LOOKUP_TABLE_SIZE = 16384*2;
}
