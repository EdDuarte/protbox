package edduarte.protbox.utils.dataholders;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Container to ease passing around a tuple of three objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class Quadruple<U, D, T, Q> implements Serializable {
    private static final long serialVersionUID = 1L;

    public U first;
    public D second;
    public T third;
    public Q fourth;

    /**
     * Constructor for a Triple reference.
     *
     * @param first  the first object in the triple
     * @param second the second object in the triple
     * @param third  the third object in the triple
     */
    public Quadruple(U first, D second, T third, Q fourth) {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    /**
     * Returns the first stored object and removes it from the Quadruple reference.
     *
     * @return the first stored object
     */
    public U pollFirst() {
        U toReturn = first;
        first = null;
        return toReturn;
    }

    /**
     * Returns the second stored object and removes it from the Quadruple reference.
     *
     * @return the second stored object
     */
    public D pollSecond() {
        D toReturn = second;
        second = null;
        return toReturn;
    }

    /**
     * Returns the third stored object and removes it from the Quadruple reference.
     *
     * @return the third stored object
     */
    public T pollThird() {
        T toReturn = third;
        third = null;
        return toReturn;
    }

    /**
     * Returns the fourth stored object and removes it from the Quadruple reference.
     *
     * @return the fourth stored object
     */
    public Q pollFourth() {
        Q toReturn = fourth;
        fourth = null;
        return toReturn;
    }

    /**
     * Checks the two objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link Quadruple} to which this one is to be checked for equality
     * @return true if the underlying objects of the triple are both considered equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Quadruple)) {
            return false;
        }
        Quadruple<?, ?, ?, ?> t = (Quadruple<?, ?, ?, ?>) o;
        return Objects.equal(t.first, first) && Objects.equal(t.second, second) && Objects.equal(t.third, third)
                && Objects.equal(t.fourth, fourth);
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
                (third == null ? 0 : third.hashCode()) ^
                (fourth == null ? 0 : fourth.hashCode());
    }

}
