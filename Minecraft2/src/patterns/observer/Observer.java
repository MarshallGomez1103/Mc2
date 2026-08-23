package patterns.observer;

/** Observer genérico y deliberadamente pequeño. */
@FunctionalInterface
public interface Observer<T> {
    void update(T notification);
}
