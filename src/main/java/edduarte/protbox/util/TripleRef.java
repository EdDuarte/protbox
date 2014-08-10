package edduarte.protbox.util;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>))
 * @version 1.0
 */
public class TripleRef<A, B, C> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final DoubleRef<A, B> pair;
    private final AtomicReference<C> third;

    /**
     * Builds a storage class for two empty values
     */
    public TripleRef() {
        this.pair = new DoubleRef<>();
        this.third = null;
    }

    /**
     * Builds a storage class for two specified values
     * @param first the first value to be stored
     * @param second the second value to be stored
     */
    public TripleRef(A first, B second, C third) {
        this.pair = new DoubleRef<>(first, second);
        this.third = new AtomicReference<>(third);
    }

    /**
     * Sets the first value stored in this class
     * @param value the first value to be stored in this class
     */
    public void setFirst(A value) {
        this.pair.setFirst(value);
    }

    /**
     * Sets the second value stored in this class
     * @param value the second value to be stored in this class
     */
    public void setSecond(B value) {
        this.pair.setSecond(value);
    }

    /**
     * Sets the third value stored in this class
     * @param value the third value to be stored in this class
     */
    public void setThird(C value) {
        this.third.set(value);
    }

    /**
     * Returns the first value stored in this class
     * @return the first value stored in this class
     */
    public A getFirst(){
        return pair.getFirst();
    }

    /**
     * Returns the second value stored in this class
     * @return the second value stored in this class
     */
    public B getSecond(){
        return pair.getSecond();
    }

    /**
     * Returns the third value stored in this class
     * @return the third value stored in this class
     */
    public C getThird(){
        return third.get();
    }

    /**
     * Returns the first value stored in this class and removes it
     * Using this method twice will always return <tt>null</tt> in the second call.
     * @return the first value stored in this class
     */
    public A pollFirst() {
        return pair.pollFirst();
    }

    /**
     * Returns the second value stored in this class and removes it
     * Using this method twice will always return <tt>null</tt> in the second call.
     * @return the second value stored in this class
     */
    public B pollSecond() {
        return pair.pollSecond();
    }

    /**
     * Returns the second value stored in this class and removes it
     * Using this method twice will always return <tt>null</tt> in the second call.
     * @return the second value stored in this class
     */
    public C pollThird() {
        return third.getAndSet(null);
    }

    @Override
    public String toString() {
        return "{"+ pair.getFirst().toString()+", "+ pair.getSecond().toString()+", "+third.toString()+"}";
    }
}
