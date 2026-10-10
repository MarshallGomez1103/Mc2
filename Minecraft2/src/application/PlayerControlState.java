package application;

import domain.block.BlockType;

/** Instantánea de controles para HUD/cámara; no expone la estamina mutable. */
public record PlayerControlState(double staminaCurrent, double staminaMaximum,
                                 double staminaFraction, boolean exhausted,
                                 boolean sprinting, boolean moving, BlockType selectedType) { }
