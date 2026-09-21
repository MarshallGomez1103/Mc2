package application;

/** Tamaños finitos admitidos por el almacenamiento actual de snapshots JSON. */
public enum WorldSize {
    SMALL("Pequeño", 2),
    MEDIUM("Mediano", 10),
    LARGE("Grande", 16);

    private final String label;
    private final int chunksPerSide;

    WorldSize(String label, int chunksPerSide) {
        this.label = label;
        this.chunksPerSide = chunksPerSide;
    }

    public String label() {
        return label;
    }

    public int chunksPerSide() {
        return chunksPerSide;
    }

    public int totalChunks() {
        return chunksPerSide * chunksPerSide;
    }
}
