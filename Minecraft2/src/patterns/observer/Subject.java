package patterns.observer;

/** Contrato mínimo para registrar y notificar observadores. */
public interface Subject<T> {
    void addObserver(Observer<T> observer);

    void removeObserver(Observer<T> observer);

    void notifyObservers(T notification);
}
