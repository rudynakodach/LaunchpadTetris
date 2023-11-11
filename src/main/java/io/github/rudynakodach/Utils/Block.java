package io.github.rudynakodach.Utils;

import io.github.rudynakodach.Lighting.LaunchpadColor;

import java.awt.Point;

public class Block {
    public final LaunchpadColor color;
    final Point point;

    public Block(LaunchpadColor color, Point point) {
        this.color = color;
        this.point = point;
    }
}
