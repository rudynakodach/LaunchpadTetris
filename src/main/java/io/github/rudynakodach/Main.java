package io.github.rudynakodach;

import io.github.rudynakodach.Lighting.LaunchpadColor;
import io.github.rudynakodach.Utils.Block;
import io.github.rudynakodach.Utils.Launchpad;

import javax.sound.midi.*;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

public class Main {
    private static void log(Block[][] map) {
        for(Block[] arr : map) {
            System.out.printf("\t%s%n", Arrays.stream(arr).map(i -> i == null ? "." : "X").collect(Collectors.joining(" ")));
        }
    }

    private static int[] calculateColumnEmptiness(Block[][] map) {
        int[] columnEmptiness = new int[9];

        for (int col = 0; col < 9; col++) {
            int emptiness = 0;
            for (int row = 0; row < 9; row++) {
                if (map[row][col] == null) {
                    emptiness++;
                }
            }
            columnEmptiness[col] = emptiness;
        }

        return columnEmptiness;
    }

    // Method to generate a random number based on column weights
    private static int generateRandomColumn(Block[][] map) {
        int[] columnEmptiness = calculateColumnEmptiness(map);

        // Calculate total emptiness to use as the range for random number generation
        int totalEmptiness = 9;
        for (int emptiness : columnEmptiness) {
            totalEmptiness += emptiness;
        }

        // Generate a random number within the total emptiness range
        Random rand = new Random();
        int randomNumber = rand.nextInt(totalEmptiness);

        // Find the column corresponding to the generated random number
        int cumulativeEmptiness = 0;
        for (int col = 0; col < 9; col++) {
            cumulativeEmptiness += columnEmptiness[col];
            if (randomNumber < cumulativeEmptiness + 9) {
                return col;
            }
        }

        return 0;
    }

    private static void recolorLaunchpad(Launchpad launchpad, Block[][] map) throws InvalidMidiDataException, MidiUnavailableException {
        Collection<Byte> message = new ArrayList<>();
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                Point p = new Point(j+1, i+1);
                message.add((byte)0);
                message.add((byte)launchpad.getManager().pointToLocation(p));
                message.add((byte)(map[i][j] != null ? map[i][j].color.getValue() : LaunchpadColor.OFF.getValue()));
            }
        }
        launchpad.getManager().send(launchpad.getManager().createSysexMessage(message));
    }

    /**
     * Moves all floating cells one cell down.
     */
    private static void applyGravity(Block[][] map) {
        for (int y = 7; y >= 0; y--) {
            for (int x = 0; x < 9; x++) {
                if (map[y][x] == null) {
                    continue;
                }
                boolean isOccupiedBelow = map[y + 1][x] != null;
                if (!isOccupiedBelow) {
                    map[y + 1][x] = map[y][x];
                    map[y][x] = null;
                }
            }
        }
    }

    private static boolean floatingCellsExist(Block[][] map) {
        for (int y = 7; y > 0; y--) {
            for (int x = 0; x < 8; x++) {
                Block current = map[y][x];
                Block next = map[y + 1][x];

                if (current != null && next == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsFullRows(Block[][] map) {
        for (int y = 0; y < 9; y++) {
            boolean isFull = true;
            for (int x = 0; x < 9; x++) {
                if (map[y][x] == null) {
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

    private static int containsFullColumns(Block[][] map) {
        for (int x = 0; x < map.length; x++) {
            boolean isColumnFull = true;
            for (int y = 0; y < map[x].length; y++) {
                if (map[y][x] == null) {
                    isColumnFull = false;
                    break;
                }
            }
            if(isColumnFull) {
                return x;
            }
        }
        return -1;
    }

    private static void removeFullRows(Block[][] map, Launchpad launchpad) throws InvalidMidiDataException, MidiUnavailableException, InterruptedException {
        Collection<Byte> message = new ArrayList<>();
        Collection<Integer> fullRows = new ArrayList<>();
        for (int y = 0; y < map.length; y++) {
            boolean isFull = true;
            for (Block b : map[y]) {
                if (b == null) {
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
                Arrays.fill(map[i], null);
            }
        }
    }

    public static void main(String[] args) throws MidiUnavailableException, InvalidMidiDataException, InterruptedException {
        final MidiDevice.Info[] devices = MidiSystem.getMidiDeviceInfo();
        final MidiDevice device = MidiSystem.getMidiDevice(Arrays.stream(devices).filter(info -> info.getName().equalsIgnoreCase("LPMiniMK3 MIDI")).findFirst().get());
        Launchpad launchpad = new Launchpad(device);

        launchpad.getManager().allOff();

        tetrisLoop(launchpad);
    }

    private static void tetrisLoop(Launchpad launchpad) throws MidiUnavailableException, InvalidMidiDataException, InterruptedException {
        LaunchpadColor[] colors = {LaunchpadColor.CYAN, LaunchpadColor.ORANGE, LaunchpadColor.PURPLE, LaunchpadColor.PINK, LaunchpadColor.GREEN, LaunchpadColor.RED};

        final long TICK_DELAY = 25;
        Block[][] map = new Block[9][9];

        for (Block[] blocks : map) {
            Arrays.fill(blocks, null);
        }

        while(true) {
//            int point = (int) Math.round((Math.random()) * 8) + 1;
            int point = generateRandomColumn(map) + 1;
            LaunchpadColor color = colors[(int) Math.round(Math.random() * (colors.length - 1))];
            Point p = new Point(point, 1);

            Block b = new Block(color, p);
            if (map[0][p.x - 1] == null) {
                for (int i = 0; i < map.length; i++) {
                    if (p.y > 1) {
                        map[p.y - 2][p.x - 1] = null;
                    }

                    map[p.y - 1][p.x - 1] = b;

                    p.y += 1;
                    recolorLaunchpad(launchpad, map);
                    Thread.sleep(TICK_DELAY);
                    if (p.y > map.length || map[p.y - 1][p.x - 1] != null) {
                        break;
                    }
                }

                do {
                    if(containsFullRows(map)) {
                        removeFullRows(map, launchpad);
                        Thread.sleep(TICK_DELAY);
                    }
                    applyGravity(map);
                    recolorLaunchpad(launchpad, map);

                    Thread.sleep(TICK_DELAY);
                } while(floatingCellsExist(map));

                if(containsFullColumns(map) >= 0) {
                    for (Block[] blocks : map) {
                        Arrays.fill(blocks, null);
                    }
                    recolorLaunchpad(launchpad, map);
                }
                Thread.sleep(TICK_DELAY);
            }
        }
    }
}
