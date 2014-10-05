package edduarte.protbox.utils.dataholders;

import java.io.Serializable;

/**
 * Container to ease passing around a tuple of five objects. This object provides a sensible
 * implementation of equals(), returning true if equals() is true on each of the contained
 * objects.
 *
 * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
 * @version 1.0
 */
public class Tuple<T> extends Quintuple<T, T, T, T, T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for a quintuple of elements of the same type.
     *
     * @param first  the first object in the quintuple
     * @param second the second object in the quintuple
     * @param third  the third object in the quintuple
     * @param fourth the third object in the quintuple
     * @param fifth  the third object in the quintuple
     */
    public Tuple(T first, T second, T third, T fourth, T fifth) {
        super(first, second, third, fourth, fifth);
    }
}
