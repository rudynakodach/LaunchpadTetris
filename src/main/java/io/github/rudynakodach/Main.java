package io.github.rudynakodach;

import io.github.rudynakodach.Lighting.LaunchpadColor;
import io.github.rudynakodach.Utils.Launchpad;

import javax.sound.midi.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class Main {
    private static void log(Boolean[][] map) {
        for(Boolean[] arr : map) {
            System.out.printf("\t%s%n", Arrays.stream(arr).map(String::valueOf).collect(Collectors.joining(", ")));
        }
    }

    private static void recolorLaunchpad(Launchpad launchpad, Boolean[][] map) throws InvalidMidiDataException, MidiUnavailableException {
        Collection<Byte> message = new ArrayList<>();
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                Point p = new Point(j+1, i+1);
                message.add((byte)0);
                message.add((byte)launchpad.getManager().pointToLocation(p));
                message.add((byte)(map[i][j] ? LaunchpadColor.BLUE.getValue() : LaunchpadColor.OFF.getValue()));
            }
        }
        launchpad.getManager().send(launchpad.getManager().createSysexMessage(message));
    }

    /**
     * Moves all floating cells one cell down.
     */
    private static void applyGravity(Boolean[][] map) {
        for (int y = 7; y >= 0; y--) {
            for (int x = 0; x < 9; x++) {
                if (!map[y][x]) {
                    continue;
                }
                boolean isOccupiedBelow = map[y + 1][x];
                if (!isOccupiedBelow) {
                    map[y][x] = false;
                    map[y + 1][x] = true;
                }
            }
        }
    }

    private static boolean floatingCellsExist(Boolean[][] map) {
        for (int y = 7; y > 0; y--) {
            for (int x = 0; x < 8; x++) {
                boolean current = map[y][x];
                boolean next = map[y + 1][x];

                if (current && !next) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsFullRows(Boolean[][] map) {
        log(map);
        for (int y = 0; y < 9; y++) {
            boolean isFull = true;
            for (int x = 0; x < 9; x++) {
                if (!map[y][x]) {
                    isFull = false;
                    break;
                }
            }
            if(isFull) {
                return true;
            }
        }
        return false;
    }


    private static void removeFullRows(Boolean[][] map, Launchpad launchpad) throws InvalidMidiDataException, MidiUnavailableException, InterruptedException {
        Collection<Byte> message = new ArrayList<>();
        Collection<Integer> fullRows = new ArrayList<>();
        for (int y = 0; y < map.length; y++) {
            boolean isFull = true;
            for (Boolean b : map[y]) {
                if (!b) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                fullRows.add(y);
                for (int x = 0; x < 9; x++) {
                    message.add((byte) 0);
                    message.add((byte) launchpad.getManager().pointToLocation(new Point(x + 1, y + 1)));
                    message.add((byte) LaunchpadColor.WHITE.getValue());
                }
            }
        }
        if(fullRows.size() > 0) {
            launchpad.getManager().send(launchpad.getManager().createSysexMessage(message));
            Thread.sleep(333);
            recolorLaunchpad(launchpad, map);
            Thread.sleep(333);
            launchpad.getManager().send(launchpad.getManager().createSysexMessage(message));
            Thread.sleep(333);
            for(int i : fullRows) {
                Arrays.fill(map[i], false);
            }
        }
    }

    public static void main(String[] args) throws MidiUnavailableException, InvalidMidiDataException, InterruptedException {
        LaunchpadColor[] colors = {LaunchpadColor.CYAN, LaunchpadColor.ORANGE, LaunchpadColor.PURPLE, LaunchpadColor.PINK, LaunchpadColor.GREEN, LaunchpadColor.RED};

        final long TICK_DELAY = 100;
        Boolean[][] map = new Boolean[9][9];

        for (Boolean[] booleans : map) {
            Arrays.fill(booleans, false);
        }

        final MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        final MidiDevice device = MidiSystem.getMidiDevice(Arrays.stream(devices).filter(info -> info.getName().equalsIgnoreCase("LPMiniMK3 MIDI")).findFirst().get());
        Launchpad launchpad = new Launchpad(device);

        launchpad.getManager().allOff();

        while(true) {
            int point = (int) Math.round((Math.random()) * 8) + 1;
            LaunchpadColor color = colors[(int)Math.random() * (colors.length - 1) + 1];
            Point p = new Point(point, 1);
            if (!map[0][p.x - 1]) {
                for (int i = 0; i < map.length; i++) {
                    if (p.y > 1) {
                        map[p.y - 2][p.x - 1] = false;
                    }

                    map[p.y - 1][p.x - 1] = true;

                    p.y += 1;
                    recolorLaunchpad(launchpad, map);
                    Thread.sleep(TICK_DELAY);
                    if (p.y > map.length || map[p.y - 1][p.x - 1]) {
                        break;
                    }
                }
                while(floatingCellsExist(map)) {
                    applyGravity(map);
                    recolorLaunchpad(launchpad, map);

                    log(map);
                    Thread.sleep(TICK_DELAY);
                }
                if(containsFullRows(map)) {
                    removeFullRows(map, launchpad);
                    Thread.sleep(TICK_DELAY);
                }
                recolorLaunchpad(launchpad, map);
            }
        }
    }
}
