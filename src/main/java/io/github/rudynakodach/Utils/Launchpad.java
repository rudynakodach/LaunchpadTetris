package io.github.rudynakodach.Utils;

import io.github.rudynakodach.Lighting.LightingManager;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

public class Launchpad {
    private final MidiDevice device;
    private final LightingManager manager;

    public Launchpad(MidiDevice device) throws MidiUnavailableException {
        this.device = device;
        this.manager = new LightingManager(this);

        device.open();
    }

    public LightingManager getManager() {
        return manager;
    }

    public MidiDevice getDevice() {
        return device;
    }
}
