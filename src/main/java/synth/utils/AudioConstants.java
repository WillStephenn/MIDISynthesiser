package synth.utils;

public interface AudioConstants {
    double SAMPLE_RATE = 44100.0;
    double RENDER_RATE = 999999;
    int BUFFER_SIZE =  (2048*2)*2;
    int BLOCK_SIZE = 1024;
    int NUMBER_OF_VOICES = 12;
    int LOOKUP_TABLE_SIZE = 16384*2;
}
