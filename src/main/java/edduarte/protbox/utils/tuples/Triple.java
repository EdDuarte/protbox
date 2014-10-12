package edduarte.protbox.utils.tuples;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Container to ease passing around a tuple of three objects. This object provides
 * a sensible implementation of equals(), returning true if equals() is true on
 * each of the contained objects.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class Triple<U, D, T> extends Tuple implements Serializable {
    private static final long serialVersionUID = 1L;

    public U first;
    public D second;
    public T third;


    /**
     * Constructor for a Triple reference.
     *
     * @param first  the first object in the triple
     * @param second the second object in the triple
     * @param third  the third object in the triple
     */
    Triple(U first, D second, T third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Triple<?, ?, ?>> T empty() {
        return (T) new Triple(null, null, null);
    }


    /**
     * Convenience method for creating an appropriately typed triple reference.
     */
    @SuppressWarnings("unchecked")
    public static <A, B, C, T extends Triple<A, B, C>>
    T of(A a, B b, C c) {
        if (areAllSameType(a, b, c)) {
            return (T) new Trio<A>(a, (A) b, (A) c);
        } else {
            return (T) new Triple<A, B, C>(a, b, c);
        }
    }


    /**
     * Returns the first stored object and removes it from the Triple reference.
     *
     * @return the first stored object
     */
    public U pollFirst() {
        U toReturn = first;
        first = null;
        return toReturn;
    }


    /**
     * Returns the second stored object and removes it from the Triple reference.
     *
     * @return the second stored object
     */
    public D pollSecond() {
        D toReturn = second;
        second = null;
        return toReturn;
    }


    /**
     * Returns the third stored object and removes it from the Triple reference.
     *
     * @return the third stored object
     */
    public T pollThird() {
        T toReturn = third;
        third = null;
        return toReturn;
    }


    /**
     * Checks the two objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link Triple} to which this one is to be checked for equality
     * @return true if the underlying objects of the triple are both considered
     * equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Triple)) {
            return false;
        }
        Triple<?, ?, ?> t = (Triple<?, ?, ?>) o;
        return Objects.equal(t.first, first) &&
                Objects.equal(t.second, second) &&
                Objects.equal(t.third, third);
    }


    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the triple
     */
    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^
                (second == null ? 0 : second.hashCode()) ^
                (third == null ? 0 : third.hashCode());
    }

}
