package edduarte.protbox.utils.tuples;

import java.io.Serializable;

/**
 * Container to ease passing around a tuple of four objects. This object provides a
 * sensible implementation of equals(), returning true if equals() is true on each
 * of the contained objects.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class Quad<T> extends Quadruple<T, T, T, T> implements Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * Constructor for a quadruple of elements of the same type.
     *
     * @param first  the first object in the trio
     * @param second the second object in the trio
     * @param third  the second object in the trio
     */
    protected Quad(T first, T second, T third, T fourth) {
        super(first, second, third, fourth);
    }

}
