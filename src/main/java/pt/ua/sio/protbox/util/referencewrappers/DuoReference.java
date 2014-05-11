package pt.ua.sio.protbox.util.referencewrappers;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple data structure to store two values, allowing methods to easily return two values at once.
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public class DuoReference<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final AtomicReference<T> first;
    private final AtomicReference<T> second;

    /**
     * Builds a storage class for two empty values
     */
    public DuoReference() {
        this.first = null;
        this.second = null;
    }

    /**
     * Builds a storage class for two specified values
     * @param first the first value to be stored
     * @param second the second value to be stored
     */
    public DuoReference(T first, T second) {
        this.first = new AtomicReference<>(first);
        this.second = new AtomicReference<>(second);
    }

    /**
     * Sets the first value stored in this class
     * @param value the first value to be stored in this class
     */
    public void setFirst(T value) {
        this.first.set(value);
    }

    /**
     * Sets the second value stored in this class
     * @param value the second value to be stored in this class
     */
    public void setSecond(T value) {
        this.second.set(value);
    }

    /**
     * Returns the first value stored in this class
     * @return the first value stored in this class
     */
    public T getFirst(){
        return first.get();
    }

    /**
     * Returns the second value stored in this class
     * @return the second value stored in this class
     */
    public T getSecond(){
        return second.get();
    }

    /**
     * Returns the first value stored in this class and removes it
     * Using this method twice will always return <tt>null</tt> in the second call.
     * @return the first value stored in this class
     */
    public T pollFirst() {
        return first.getAndSet(null);
    }

    /**
     * Returns the second value stored in this class and removes it
     * Using this method twice will always return <tt>null</tt> in the second call.
     * @return the second value stored in this class
     */
    public T pollSecond() {
        return second.getAndSet(null);
    }

    @Override
    public String toString() {
        return "{"+first.toString()+", "+second.toString()+"}";
    }
}
