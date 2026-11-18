package domain;

/** Demo intencional: el dominio no puede depender del adaptador de red. */
final class DemoArchitectureViolation {
    network.ProtocolCodec codec;
}
