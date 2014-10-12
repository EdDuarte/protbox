package edduarte.protbox.utils.tuples;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * Simple data structure to store one single value, allowing anonymous classes to
 * change outside variables. Since the compiler forces outside variables used by
 * anonymous classes to be indicated as "final", one can wrap a value inside the Uno
 * structure and indicate the Uno as "final", allowing transition and modification
 * of the value. This object also provides a sensible implementation of equals(),
 * returning true if equals() is true on each of the contained objects.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class Single<T> extends Tuple implements Serializable {
    private static final long serialVersionUID = 1L;

    public T value;


    /**
     * Constructor for a Single reference.
     *
     * @param value the value object in the Single reference
     */
    protected Single(T value) {
        this.value = value;
    }


    @SuppressWarnings("unchecked")
    public static <T extends Single<?>> T empty() {
        return (T) new Single(null);
    }


    /**
     * Convenience method for creating an appropriately typed single reference.
     */
    public static <A> Single<A> of(A a) {
        return new Single<A>(a);
    }


    /**
     * Returns the value stored object and removes it from the Single reference.
     *
     * @return the value stored object
     */
    public T poll() {
        T toReturn = value;
        value = null;
        return toReturn;
    }


    /**
     * Checks the two objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link Single} to which this one is to be checked for equality
     * @return true if the underlying objects of the Single reference are both
     * considered equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Single)) {
            return false;
        }
        Single<?> p = (Single<?>) o;
        return Objects.equal(p.value, value);
    }


    /**
     * Compute a hash code using the hash codes of the underlying object
     *
     * @return a hashcode of the Single reference
     */
    @Override
    public int hashCode() {
        return (value == null ? 0 : value.hashCode());
    }

}
