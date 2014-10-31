/*
 * Copyright 2014 University of Aveiro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edduarte.protbox.utils;


/**
 * @author Eduardo Duarte (<a href="mailto:eduardo.miguel.duarte@gmail.com">eduardo.miguel.duarte@gmail.com</a>)
 * @version 2.0
 */
public class Pair<T> {

    public final T first;
    public final T second;

    private Pair(T first, T second) {
        this.first = first;
        this.second = second;
    }

    public static <T> Pair<T> of(T first, T second) {
        if (first == null) {
            throw new IllegalArgumentException();
        }
        if (second == null) {
            throw new IllegalArgumentException();
        }
        return new Pair<T>(first, second);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        if (!first.equals(pair.first)) return false;
        if (!second.equals(pair.second)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ')';
    }
}
