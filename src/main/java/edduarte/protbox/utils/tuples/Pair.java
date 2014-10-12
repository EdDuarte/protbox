package edduarte.protbox.utils.tuples;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Container to ease passing around a tuple of two objects. This object provides a
 * sensible implementation of equals(), returning true if equals() is true on each
 * of the contained objects.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class Pair<U, D> extends Tuple implements Serializable {
    private static final long serialVersionUID = 1L;

    public U first;
    public D second;


    /**
     * Constructor for a Double reference.
     *
     * @param first  the first object in the Double reference
     * @param second the second object in the pair
     */
    protected Pair(U first, D second) {
        this.first = first;
        this.second = second;
    }


    @SuppressWarnings("unchecked")
    public static <T extends Pair<?, ?>> T empty() {
        return (T) new Pair(null, null);
    }


    /**
     * Convenience method for creating an appropriately typed double reference.
     */
    @SuppressWarnings("unchecked")
    public static <A, B, T extends Pair<A, B>>
    T of(A a, B b) {
        if (areAllSameType(a, b)) {
            return (T) new Duo<A>(a, (A) b);
        } else {
            return (T) new Pair<A, B>(a, b);
        }
    }


    /**
     * Returns the first stored object and removes it from the Double reference.
     *
     * @return the first stored object
     */
    public U pollFirst() {
        U toReturn = first;
        first = null;
        return toReturn;
    }


    /**
     * Returns the second stored object and removes it from the Double reference.
     *
     * @return the second stored object
     */
    public D pollSecond() {
        D toReturn = second;
        second = null;
        return toReturn;
    }


    /**
     * Checks the two objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link Pair} to which this one is to be checked for equality
     * @return true if the underlying objects of the Double reference are both
     * considered equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pair)) {
            return false;
        }
        Pair<?, ?> p = (Pair<?, ?>) o;
        return Objects.equal(p.first, first) && Objects.equal(p.second, second);
    }


    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the Double reference
     */
    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^
                (second == null ? 0 : second.hashCode());
    }

}
