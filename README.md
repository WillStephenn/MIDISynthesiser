# Java MIDI Synthesiser

This project is a polyphonic MIDI synthesiser built entirely in Java with a JavaFX GUI. It was developed as a personal exercise to gain a deeper understanding of Object-Oriented Programming (OOP), real-time audio processing, and digital signal processing (DSP) fundamentals.

The primary goal was to build a functional synthesiser from the ground up, intentionally avoiding third-party libraries for DSP and synthesis to engage directly with the core challenges of audio programming.

![Arpeggiated Chord Stabs Player](httpss://raw.githubusercontent.com/WillStephenn/MIDISynthesiser/master/Repo%20Resources/Arpeggiated%20Chord%20Stabs.mp4)
## Features

The synthesiser provides a complete synthesis signal path with a flexible, polyphonic voice architecture.

* **Polyphonic Voice Architecture**: The synthesiser supports multiple voices (6 by default), with a voice-stealing algorithm to manage polyphony.
* **Multi-Waveform Oscillators**: Each voice is equipped with an oscillator that can generate sine, square, sawtooth, and triangle waveforms.
* **Resonant Low-Pass Filter**: A topology-preserving transform (TPT) state-variable filter is included in each voice, complete with frequency cutoff and resonance controls.
* **Dual ADSR Envelopes**: Each voice has two independent ADSR (Attack, Decay, Sustain, Release) envelopes—one for modulating the amplitude and another for modulating the filter cutoff.
* **Low-Frequency Oscillator (LFO)**: A global LFO with multiple waveforms (sine, saw, triangle, square) can be used to modulate parameters, including stereo panning.
* **Full MIDI Control**: The synthesiser can be controlled in real-time via any standard MIDI input device. It handles Note On, Note Off, Velocity and Control Change (CC) messages to dynamically shape the sound.
* **Graphical User Interface**: A complete GUI built with JavaFX allows for intuitive control over all synthesiser parameters, including device selection, oscillator and filter settings, envelopes, and global controls.

---

## Technical Deep Dive: An Exercise in Optimisation

A core focus of this project was learning to write performant code suitable for real-time audio within the Java ecosystem. The nature of audio processing requires consistent, low-latency performance, which presents unique challenges for a managed environment like the JVM.

To achieve this, several optimisation strategies were employed:

* **Minimising the Audio Thread Workload**: The most critical aspect was ensuring the audio processing loop is as efficient as possible. This meant minimising floating-point operations (FLOPs) and avoiding any operations that could introduce unpredictable delays.
* **Pre-computation and Lookup Tables (LUTs)**: To avoid expensive calculations like `Math.sin()` or `Math.tan()` in the real-time audio thread, these values were pre-computed on startup and stored in large lookup tables. This includes all oscillator waveforms and the coefficients for the resonant filter at various cutoff and resonance settings.
* **Eliminating Garbage Collection**: The audio processing loop is carefully designed to be garbage-free. All necessary memory, such as audio buffers, is allocated at initialisation and reused throughout the application's lifecycle. This is crucial for avoiding the unpredictable pauses that garbage collection can introduce, which would otherwise manifest as audible clicks or glitches.
* **Efficient Operations**: Where possible, more efficient operations were used. For example, the oscillator's phase wrapping is handled with a bitwise `AND` operation (`& phaseMask`) instead of a more costly conditional or modulo operation. Constants, such as the reciprocal of the sample rate, were also pre-calculated to turn divisions into multiplications within the audio loop.

---

## Limitations and Reflections

While this project was a success as a learning exercise, building a real-time synthesiser in Java highlighted the inherent challenges of the platform for this type of application. The Java Virtual Machine (JVM) with its automatic memory management and just-in-time compilation is not ideally suited for the demands of low-latency audio processing.

The non-deterministic nature of the garbage collector, even with careful coding practices, remains a potential source of instability. A language like **C++**, which offers manual memory management and closer-to-the-metal control, would be a more conventional and robust choice for a production-grade audio application. This project, therefore, stands not as a template for production software, but as a testament to the educational value of pushing a language to its limits to better understand the fundamentals of software engineering and digital audio.

---

## Getting Started

To run this project locally, you will need Java and Maven installed.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/WillStephenn/MIDISynthesiser
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd MIDIsynthesiser
    ```
3.  **Build and run the application using Maven:**
    ```bash
    mvn clean javafx:run
    ```
    This will launch the synthesiser's graphical user interface. From there, you can select your preferred MIDI input and audio output devices to begin playing.
