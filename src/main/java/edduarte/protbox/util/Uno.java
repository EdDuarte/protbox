package edduarte.protbox.util;

import java.io.Serializable;

/**
 * Simple data structure to store one single value, allowing outside variables to be changed inside anonymous classes.
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public class Uno<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private T value;

    /**
     * Builds a storage class for one single empty value
     */
    public Uno() {
        this.value = null;
    }

    /**
     * Builds a storage class for one single specified value
     * @param value the value to be stored
     */
    public Uno(T value) {
        this.value = value;
    }

    /**
     * Sets the value stored in this class
     * @param value the value stored to be stored in this class
     */
    public void set(T value) {
        this.value = value;
    }

    /**
     * Returns the value stored in this class
     * @return the value stored in this class
     */
    public T get(){
        return value;
    }

    /**
     * Returns the value stored in this class and removes it
     * Using this method twice will always return <tt>null</tt> in the second call.
     * @return the value stored in this class
     */
    public T poll() {
        T toReturn = value;
        value = null;
        return toReturn;
    }

    @Override
    public String toString() {
        return "{"+value.toString()+"}";
    }
}
