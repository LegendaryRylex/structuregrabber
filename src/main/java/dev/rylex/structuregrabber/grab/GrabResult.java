package dev.rylex.structuregrabber.grab;

import java.nio.file.Path;

public record GrabResult(Path file, int blocks, int entities) {}
