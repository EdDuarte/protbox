package edduarte.protbox.utils;

import com.google.common.base.Objects;

import java.io.Serializable;

public final class Ref {


    /**
     * Convenience method for creating an appropriately typed single reference.
     */
    public static <F> Single<F> of1(F value) {
        return new Single<>(value);
    }


    /**
     * Convenience method for creating an appropriately typed double reference.
     */
    public static <F, S> Double<F, S> of2(F first, S second) {
        return new Double<>(first, second);
    }


    /**
     * Convenience method for creating an appropriately typed triple reference.
     */
    public static <F, S, T> Triple<F, S, T> of3(F first, S second, T third) {
        return new Triple<>(first, second, third);
    }


    /**
     * Convenience method for creating an appropriately typed quadruple reference.
     */
    public static <F, S, T, R> Quadruple<F, S, T, R> of4(F first, S second, T third, R fourth) {
        return new Quadruple<>(first, second, third, fourth);
    }


    /**
     * Convenience method for creating an appropriately typed duo reference.
     */
    public static <T> Duo<T> of1(T first, T second) {
        return new Duo<>(first, second);
    }


    /**
     * Convenience method for creating an appropriately typed trio reference.
     */
    public static <T> Trio<T> of1(T first, T second, T third) {
        return new Trio<>(first, second, third);
    }


    /**
     * Convenience method for creating an appropriately typed quintuple reference.
     */
    public static <T> Tuple<T> of1(T first, T second, T third, T fourth, T fifth) {
        return new Tuple<>(first, second, third, fourth, fifth);
    }


    /**
     * Container to ease passing around a tuple of two objects. This object provides a sensible
     * implementation of equals(), returning true if equals() is true on each of the contained
     * objects.
     *
     * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
     * @version 1.0
     */
    public static final class Double<F, S> implements Serializable {
        private static final long serialVersionUID = 1L;

        public F first;
        public S second;

        /**
         * Constructor for a Double reference.
         *
         * @param first the first object in the Double reference
         * @param second the second object in the pair
         */
        private Double(F first, S second) {
            this.first = first;
            this.second = second;
        }

        /**
         * Returns the first stored object and removes it from the Double reference.
         * @return the first stored object
         */
        public F pollFirst() {
            F toReturn = first;
            first = null;
            return toReturn;
        }

        /**
         * Returns the second stored object and removes it from the Double reference.
         * @return the second stored object
         */
        public S pollSecond() {
            S toReturn = second;
            second = null;
            return toReturn;
        }

        /**
         * Checks the two objects for equality by delegating to their respective
         * {@link Object#equals(Object)} methods.
         *
         * @param o the {@link edduarte.protbox.utils.Ref.Double} to which this one is to be checked for equality
         * @return true if the underlying objects of the Double reference are both considered equal
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Double)) {
                return false;
            }
            Double<?, ?> p = (Double<?, ?>) o;
            return Objects.equal(p.first, first) && Objects.equal(p.second, second);
        }

        /**
         * Compute a hash code using the hash codes of the underlying objects
         *
         * @return a hashcode of the Double reference
         */
        @Override
        public int hashCode() {
            return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
        }
    }


    /**
     * Container to ease passing around a tuple of three objects. This object provides a sensible
     * implementation of equals(), returning true if equals() is true on each of the contained
     * objects.
     *
     * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
     * @version 1.0
     */
    public static final class Triple<F, S, T> implements Serializable {
        private static final long serialVersionUID = 1L;

        public F first;
        public S second;
        public T third;

        /**
         * Constructor for a Triple reference.
         *
         * @param first the first object in the triple
         * @param second the second object in the triple
         * @param third the third object in the triple
         */
        private Triple(F first, S second, T third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        /**
         * Returns the first stored object and removes it from the Triple reference.
         * @return the first stored object
         */
        public F pollFirst() {
            F toReturn = first;
            first = null;
            return toReturn;
        }

        /**
         * Returns the second stored object and removes it from the Triple reference.
         * @return the second stored object
         */
        public S pollSecond() {
            S toReturn = second;
            second = null;
            return toReturn;
        }

        /**
         * Returns the third stored object and removes it from the Triple reference.
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
         * @param o the {@link edduarte.protbox.utils.Ref.Triple} to which this one is to be checked for equality
         * @return true if the underlying objects of the triple are both considered equal
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Triple)) {
                return false;
            }
            Triple<?, ?, ?> t = (Triple<?, ?, ?>) o;
            return Objects.equal(t.first, first) && Objects.equal(t.second, second) && Objects.equal(t.third, third);
        }

        /**
         * Compute a hash code using the hash codes of the underlying objects
         *
         * @return a hashcode of the triple
         */
        @Override
        public int hashCode() {
            return  (first == null ? 0 : first.hashCode()) ^
                    (second == null ? 0 : second.hashCode()) ^
                    (third == null ? 0 : third.hashCode());
        }

    }


    /**
     * Container to ease passing around a tuple of three objects. This object provides a sensible
     * implementation of equals(), returning true if equals() is true on each of the contained
     * objects.
     *
     * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
     * @version 1.0
     */
    public static final class Quadruple<F, S, T, R> implements Serializable {
        private static final long serialVersionUID = 1L;

        public F first;
        public S second;
        public T third;
        public R fourth;

        /**
         * Constructor for a Triple reference.
         *
         * @param first the first object in the triple
         * @param second the second object in the triple
         * @param third the third object in the triple
         */
        private Quadruple(F first, S second, T third, R fourth) {
            this.first = first;
            this.second = second;
            this.third = third;
            this.fourth = fourth;
        }

        /**
         * Returns the first stored object and removes it from the Quadruple reference.
         * @return the first stored object
         */
        public F pollFirst() {
            F toReturn = first;
            first = null;
            return toReturn;
        }

        /**
         * Returns the second stored object and removes it from the Quadruple reference.
         * @return the second stored object
         */
        public S pollSecond() {
            S toReturn = second;
            second = null;
            return toReturn;
        }

        /**
         * Returns the third stored object and removes it from the Quadruple reference.
         * @return the third stored object
         */
        public T pollThird() {
            T toReturn = third;
            third = null;
            return toReturn;
        }

        /**
         * Returns the fourth stored object and removes it from the Quadruple reference.
         * @return the fourth stored object
         */
        public R pollFourth() {
            R toReturn = fourth;
            fourth = null;
            return toReturn;
        }

        /**
         * Checks the two objects for equality by delegating to their respective
         * {@link Object#equals(Object)} methods.
         *
         * @param o the {@link edduarte.protbox.utils.Ref.Quadruple} to which this one is to be checked for equality
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
            return  (first == null ? 0 : first.hashCode()) ^
                    (second == null ? 0 : second.hashCode()) ^
                    (third == null ? 0 : third.hashCode()) ^
                    (fourth == null ? 0 : fourth.hashCode());
        }

    }


    /**
     * Simple data structure to store one single value, allowing anonymous classes to change outside
     * variables. Since the compiler forces outside variables used by anonymous classes to be
     * indicated as "final", one can wrap a value inside the Uno structure and indicate the Uno as
     * "final", allowing transition and modification of the value. This object also provides a sensible
     * implementation of equals(), returning true if equals() is true on each of the contained objects.
     *
     * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
     * @version 1.0
     */
    public static final class Single<T> implements Serializable {
        private static final long serialVersionUID = 1L;

        public T value;

        /**
         * Constructor for a Single reference.
         *
         * @param value the value object in the Single reference
         */
        private Single(T value) {
            this.value = value;
        }

        /**
         * Returns the value stored object and removes it from the Single reference.
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
         * @param o the {@link edduarte.protbox.utils.Ref.Single} to which this one is to be checked for equality
         * @return true if the underlying objects of the Single reference are both considered equal
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


    /**
     * Container to ease passing around a tuple of two objects. This object provides a sensible
     * implementation of equals(), returning true if equals() is true on each of the contained
     * objects.
     *
     * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
     * @version 1.0
     */
    public static final class Duo<T> implements Serializable {
        private static final long serialVersionUID = 1L;

        public T first;
        public T second;

        /**
         * Constructor for a duo of elements of the same type.
         *
         * @param first  the first object in the duo
         * @param second the second object in the duo
         */
        private Duo(T first, T second) {
            this.first = first;
            this.second = second;
        }

        /**
         * Returns the first stored object and removes it from the Duo reference.
         * @return the first stored object
         */
        public T pollFirst() {
            T toReturn = first;
            first = null;
            return toReturn;
        }

        /**
         * Returns the second stored object and removes it from the Duo reference.
         * @return the second stored object
         */
        public T pollSecond() {
            T toReturn = second;
            second = null;
            return toReturn;
        }

        /**
         * Checks the two objects for equality by delegating to their respective
         * {@link Object#equals(Object)} methods.
         *
         * @param o the {@link edduarte.protbox.utils.Ref.Duo} to which this one is to be checked for equality
         * @return true if the underlying objects of the triple are both considered equal
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Duo)) {
                return false;
            }
            Duo<?> t = (Duo<?>) o;
            return Objects.equal(t.first, first) && Objects.equal(t.second, second);
        }

        /**
         * Compute a hash code using the hash codes of the underlying objects
         *
         * @return a hashcode of the duo
         */
        @Override
        public int hashCode() {
            return (first == null ? 0 : first.hashCode()) ^
                    (second == null ? 0 : second.hashCode());
        }
    }


    /**
     * Container to ease passing around a tuple of three objects. This object provides a sensible
     * implementation of equals(), returning true if equals() is true on each of the contained
     * objects.
     *
     * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
     * @version 1.0
     */
    public static final class Trio<T> implements Serializable {
        private static final long serialVersionUID = 1L;

        public T first;
        public T second;
        public T third;

        /**
         * Constructor for a duo of elements of the same type.
         *
         * @param first  the first object in the duo
         * @param second the second object in the duo
         */
        private Trio(T first, T second, T third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        /**
         * Returns the first stored object and removes it from the Trio reference.
         * @return the first stored object
         */
        public T pollFirst() {
            T toReturn = first;
            first = null;
            return toReturn;
        }

        /**
         * Returns the second stored object and removes it from the Trio reference.
         * @return the second stored object
         */
        public T pollSecond() {
            T toReturn = second;
            second = null;
            return toReturn;
        }

        /**
         * Returns the third stored object and removes it from the Trio reference.
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
         * @param o the {@link edduarte.protbox.utils.Ref.Trio} to which this one is to be checked for equality
         * @return true if the underlying objects of the triple are both considered equal
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Trio)) {
                return false;
            }
            Trio<?> t = (Trio<?>) o;
            return Objects.equal(t.first, first) && Objects.equal(t.second, second) && Objects.equal(t.third, third);
        }

        /**
         * Compute a hash code using the hash codes of the underlying objects
         *
         * @return a hashcode of the duo
         */
        @Override
        public int hashCode() {
            return (first == null ? 0 : first.hashCode()) ^
                    (second == null ? 0 : second.hashCode()) ^
                    (third == null ? 0 : third.hashCode());
        }
    }


    /**
     * Container to ease passing around a tuple of four objects. This object provides a sensible
     * implementation of equals(), returning true if equals() is true on each of the contained
     * objects.
     *
     * @author Eduardo Duarte (<a href="mailto:emod@ua.pt">emod@ua.pt</a>)
     * @version 1.0
     */
    public static final class Tuple<T> implements Serializable {
        private static final long serialVersionUID = 1L;

        public T first;
        public T second;
        public T third;
        public T forth;
        public T fifth;

        /**
         * Constructor for a quintuple of elements of the same type.
         *
         * @param first the first object in the quintuple
         * @param second the second object in the quintuple
         * @param third the third object in the quintuple
         * @param forth the third object in the quintuple
         * @param fifth the third object in the quintuple
         */
        private Tuple(T first, T second, T third, T forth, T fifth) {
            this.first = first;
            this.second = second;
            this.third = third;
            this.forth = forth;
            this.fifth = fifth;
        }

        /**
         * Returns the first stored object and removes it from the Tuple reference.
         * @return the first stored object
         */
        public T pollFirst() {
            T toReturn = first;
            first = null;
            return toReturn;
        }

        /**
         * Returns the second stored object and removes it from the Tuple reference.
         * @return the second stored object
         */
        public T pollSecond() {
            T toReturn = second;
            second = null;
            return toReturn;
        }

        /**
         * Returns the third stored object and removes it from the Tuple reference.
         * @return the third stored object
         */
        public T pollThird() {
            T toReturn = third;
            third = null;
            return toReturn;
        }

        /**
         * Returns the fourth stored object and removes it from the Tuple reference.
         * @return the fourth stored object
         */
        public T pollFourth() {
            T toReturn = forth;
            forth = null;
            return toReturn;
        }

        /**
         * Returns the fifth stored object and removes it from the Tuple reference.
         * @return the fifth stored object
         */
        public T pollFifth() {
            T toReturn = fifth;
            fifth = null;
            return toReturn;
        }

        /**
         * Checks the two objects for equality by delegating to their respective
         * {@link Object#equals(Object)} methods.
         *
         * @param o the {@link edduarte.protbox.utils.Ref.Tuple} to which this one is to be checked for equality
         * @return true if the underlying objects of the triple are both considered equal
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Tuple)) {
                return false;
            }
            Tuple<?> t = (Tuple<?>) o;
            return  Objects.equal(t.first, first) && Objects.equal(t.second, second) &&
                    Objects.equal(t.third, third) && Objects.equal(t.forth, forth) &&
                    Objects.equal(t.fifth, fifth);
        }

        /**
         * Compute a hash code using the hash codes of the underlying objects
         *
         * @return a hashcode of the quintuple
         */
        @Override
        public int hashCode() {
            return  (first == null ? 0 : first.hashCode()) ^
                    (second == null ? 0 : second.hashCode()) ^
                    (third == null ? 0 : third.hashCode()) ^
                    (forth == null ? 0 : forth.hashCode()) ^
                    (fifth == null ? 0 : fifth.hashCode());
        }
    }
}