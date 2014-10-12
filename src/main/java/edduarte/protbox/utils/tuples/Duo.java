package edduarte.protbox.utils.tuples;

import java.io.Serializable;

/**
 * Container to ease passing around a tuple of two objects. This object provides a
 * sensible implementation of equals(), returning true if equals() is true on each
 * of the contained objects.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class Duo<T> extends Pair<T, T> implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * Constructor for a double of elements of the same type.
     *
     * @param first  the first object in the duo
     * @param second the second object in the duo
     */
    protected Duo(T first, T second) {
        super(first, second);
    }

}
