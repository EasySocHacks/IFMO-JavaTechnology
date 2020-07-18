package ru.ifmo.rain.elfimov.arrayset;

import java.util.*;

@SuppressWarnings({"WeakerAccess", "unused", "unchecked"})
public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    private final List<E> array;
    private final Comparator<? super E> comparator;

    public ArraySet() {
        array = new ArrayList<>();
        comparator = null;
    }

    private ArraySet(List<E> list, Comparator<? super E> comparator) {
        array = list;
        this.comparator = comparator;
    }

    public ArraySet(Collection<? extends E> collection) {
        comparator = null;

        if (isSortedCollection(collection)) {
            array = getSortedList(collection);
        } else {
            array = new ArrayList<>(new TreeSet<E>(collection));
        }
    }

    public ArraySet(Comparator<? super E> comparator) {
        array = new ArrayList<>();
        this.comparator = comparator;
    }

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        this.comparator = comparator;

        if (isSortedCollection(collection)) {
            array = getSortedList(collection);
        } else {
            Set<E> set = new TreeSet<>(comparator);
            set.addAll(collection);

            array = new ArrayList<>(set);
        }
    }

    private int binSearch(E element, boolean includingElement, ShiftSide side) {
        int position = Collections.binarySearch(array, element, comparator);

        if (position < 0) {
            position = -(position + 1);

            if (side == ShiftSide.LOWER) {
                position--;
            }
        } else {
            if (!includingElement) {
                position += side.shift;
            }
        }

        return position;
    }

    private E returnElement(int position) {
        if (position >= 0 && position < array.size()) {
            return array.get(position);
        } else {
            return null;
        }
    }

    private int lowerPosition(E e) {
        return binSearch(e, false, ShiftSide.LOWER);
    }

    @Override
    public E lower(E e) {
        return returnElement(lowerPosition(e));
    }

    private int floorPosition(E e) {
        return binSearch(e, true, ShiftSide.LOWER);
    }

    @Override
    public E floor(E e) {
        return returnElement(floorPosition(e));
    }

    private int ceilingPosition(E e) {
        return binSearch(e, true, ShiftSide.HIGHER);
    }

    @Override
    public E ceiling(E e) {
        return returnElement(ceilingPosition(e));
    }

    private int higherPosition(E e) {
        return binSearch(e, false, ShiftSide.HIGHER);
    }

    @Override
    public E higher(E e) {
        return returnElement(higherPosition(e));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("Function 'pollFirst' is unsupported because of ArraySet is unmodified");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("Function 'pollLast' is unsupported because of ArraySet is unmodified");
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(array).iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new ReversibleList<>(array), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("'fromElement' is greater than 'toElement'");
        }

        int fromPosition = fromInclusive ? ceilingPosition(fromElement) : higherPosition(fromElement);
        int toPosition = toInclusive ? floorPosition(toElement) : lowerPosition(toElement);

        if (toPosition + 1 < fromPosition) {
            return new ArraySet<>(comparator);
        }

        return new ArraySet<>(array.subList(fromPosition, toPosition + 1), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        int position = inclusive ? floorPosition(toElement) : lowerPosition(toElement);

        return new ArraySet<>(array.subList(0, position + 1), comparator);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        int position = (inclusive ? ceilingPosition(fromElement) : higherPosition(fromElement));

        return new ArraySet<>(array.subList(position, array.size()), comparator);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (array.isEmpty()) {
            throw new NoSuchElementException("Cannot do 'first' from an empty set");
        }

        return array.get(0);
    }

    @Override
    public E last() {
        if (array.isEmpty()) {
            throw new NoSuchElementException("Cannot do 'last' from an empty set");
        }

        return array.get(array.size() - 1);
    }

    @Override
    public int size() {
        return array.size();
    }

    @Override
    public boolean contains(Object o) {
        int position = Collections.binarySearch(array, (E) o, comparator);

        return position >= 0;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot do 'clear' for unmodified set");
    }

    private static class ReversibleList<E> extends ArrayList<E> {
        private boolean reversed;

        ReversibleList(Collection<? extends E> collection) {
            super(collection);

            if (collection instanceof ArraySet.ReversibleList) {
                reversed = !((ReversibleList<? extends E>) collection).isReversed();
            } else {
                reversed = true;
            }
        }

        @Override
        public E get(int index) {
            if (reversed) {
                return super.get(size() - index - 1);
            } else {
                return super.get(index);
            }
        }

        @Override
        public Iterator<E> iterator() {
            if (reversed) {
                ArrayList<E> reversedList = new ArrayList<>(this);
                Collections.reverse(reversedList);

                return reversedList.iterator();
            } else {
                return super.iterator();
            }
        }

        public boolean isReversed() {
            return reversed;
        }
    }

    private enum ShiftSide {
        LOWER(-1), HIGHER(1);

        final int shift;

        ShiftSide(int shift) {
            this.shift = shift;
        }
    }

    private int compare(E firstElement, E secondElement) {
        if (comparator == null) {
            if (!(firstElement instanceof Comparable) || !(secondElement instanceof Comparable)) {
                throw new ClassCastException("'firstElement' and 'secondElement' cannot be compared to one another using this set's comparator (or, if the set has no comparator, using natural ordering)");
            }

            return ((Comparable) firstElement).compareTo(secondElement);
        }

        return comparator.compare(firstElement, secondElement);
    }

    private boolean isSortedCollection(Collection<? extends E> collection) {
        List <E> listCollection = new ArrayList<>(collection);

        for (int i = 1; i < listCollection.size(); i++) {
            if (compare(listCollection.get(i - 1), listCollection.get(i)) > 0) {
                return false;
            }
        }

        return true;
    }

    private List<E> getSortedList(Collection <? extends E> sortedCollection) {
        List <E> sortedList = new ArrayList<>();
        List <E> originList = new ArrayList<>(sortedCollection);

        for (int i = 0; i < originList.size(); i++) {
            if ((i > 0 && compare(originList.get(i), originList.get(i - 1)) != 0) || i == 0) {
                sortedList.add(originList.get(i));
            }
        }

        return  sortedList;
    }
}
