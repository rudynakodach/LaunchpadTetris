package io.github.rudynakodach.Lighting;

import io.github.rudynakodach.Utils.Launchpad;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.SysexMessage;
import java.awt.Point;
import java.util.*;

public class LightingManager {

    final static private int[] BASE = {240, 0, 32, 41, 2, 13, 3};
    private final Launchpad device;

    public LightingManager(Launchpad launchpad) {
        this.device = launchpad;
    }

    private byte[] convertToBytes() {
        byte[] bytes = new byte[BASE.length];

        for (int i = 0; i < BASE.length; i++) {
            bytes[i] = (byte) BASE[i];
        }
        return bytes;
    }

    private Collection<Byte> arrToCollection(byte[] arr) {
        Collection<Byte> col = new ArrayList<>();
        for (byte b : arr) {
            col.add(b);
        }
        return col;
    }

    public SysexMessage createSysexMessage(Collection<Byte> data) throws InvalidMidiDataException {
        Collection<Byte> message = arrToCollection(convertToBytes());
        message.addAll(data);
        message.add((byte) 247);

        byte[] byteArray = new byte[message.size()];
        int index = 0;

        for (Byte b : message) {
            byteArray[index++] = b;
        }

        return new SysexMessage(byteArray, byteArray.length);
    }

    public void send(SysexMessage message) throws MidiUnavailableException {
        device.getDevice().getReceiver().send(message, -1);
    }

    public int pointToLocation(Point point) {
        final int x = (int)point.getX();
        final int y = (int)point.getY();
        return (10-y)*10+x;
    }

    public void set(Point p, byte mode, LaunchpadColor... colors) throws InvalidMidiDataException, MidiUnavailableException {
        int pos = pointToLocation(p);
        Collection<Byte> data = new ArrayList<>();
        data.add(mode);
        data.add((byte)pos);
        data.addAll(Arrays.stream(colors).map(color -> (byte)color.getValue()).toList());

        send(createSysexMessage(data));
    }

    public void set(Point p, int mode, LaunchpadColor... colors) throws InvalidMidiDataException, MidiUnavailableException {
        set(p, (byte)mode, colors);
    }

    public void allOff() throws InvalidMidiDataException, MidiUnavailableException {
        Collection<Byte> data = new ArrayList<>();
        for (int i = 11; i <= 99; i++) {
            data.add((byte)0);
            data.add((byte)i);
            data.add((byte) LaunchpadColor.OFF.getValue());
        }

        send(createSysexMessage(data));
    }
}
