package ru.ifmo.rain.elfimov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroupsByFunction(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroupsByFunction(students, this::sortStudentsById);
    }

    @Override
    public String getLargestGroup(final Collection<Student> students) {
        return getStreamLargestGroup(
                students.stream().collect(
                        Collectors.groupingBy(Student::getGroup, Collectors.counting())).
                        entrySet().stream());
    }

    @Override
    public String getLargestGroupFirstName(final Collection<Student> students) {
        return getStreamLargestGroup(
                getStudentsStreamGroupingByFunction(students, Student::getGroup).
                        entrySet().stream().map(
                                (Map.Entry<String, List<Student>> o) ->
                                        Map.entry(o.getKey(),
                                                o.getValue().stream().map(Student::getFirstName).distinct().count())));
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return getStudentsByFunction(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return getStudentsByFunction(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(final List<Student> students) {
        return getStudentsByFunction(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return getStudentsByFunction(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return (Set<String>) getStudentsByFunction(students,
                Student::getFirstName, Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMinStudentFirstName(final List<Student> students) {
        return getSortedStudentsStream(students, Student::compareTo).
                findFirst().map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return getSortedStudentsStream(students, Student::compareTo).
                collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return  getSortedStudentsStream(students,
                    Comparator.comparing(Student::getLastName).
                    thenComparing(Student::getFirstName).
                    thenComparing(Student::getId)).
                collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return findStudentsByPredicate(students, s -> s.getFirstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return findStudentsByPredicate(students, s -> s.getLastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final String group) {
        return findStudentsByPredicate(students, s -> s.getGroup().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final String group) {
        return students.stream().filter(s -> s.getGroup().equals(group)).
                collect(Collectors.toMap(Student::getLastName,
                        Student::getFirstName, (s1, s2) -> s1.compareTo(s2) < 0 ? s1 : s2));
    }

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(this::getFullName,
                Collectors.mapping(Student::getGroup, Collectors.toSet()))).
                entrySet().stream().max(Comparator.comparing((Map.Entry<String, Set<String>> entry) ->
                entry.getValue().size()).
                thenComparing(Map.Entry::getKey)).
                map(Map.Entry::getKey).orElse("");
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] indices) {
        return getStudentsListByFunctionAndIndices(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] indices) {
        return getStudentsListByFunctionAndIndices(students, indices, Student::getLastName);
    }

    @Override
    public List<String> getGroups(final Collection<Student> students, final int[] indices) {
        return getStudentsListByFunctionAndIndices(students, indices, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] indices) {
        return getStudentsListByFunctionAndIndices(students, indices, this::getFullName);
    }

    private <R> List<R> getStudentsListByFunctionAndIndices(final Collection<Student> students,
                                                            final int[] indices,
                                                            final Function<? super Student, ? extends R> function) {
        return Arrays.stream(indices).
                mapToObj(e -> (Student) students.toArray()[e]).
                map(function).collect(Collectors.toList());
    }

    private <R> Map<R, List<Student>> getStudentsStreamGroupingByFunction(final Collection<Student> students,
                                                                          final Function<? super Student, ? extends R> function) {
        return students.stream().
                collect(Collectors.groupingBy(function));
    }

    private String getFullName(final Student student) {
        return String.join(" ", student.getFirstName(), student.getLastName());
    }

    private <T> Collection<T> getStudentsByFunction(final List<Student> students,
                                                    final Function <Student, T> mapper,
                                                    final Collector<T, ?, ? extends Collection<T>> collector) {
        return students.stream().map(mapper).collect(collector);
    }

    private <T> List<T> getStudentsByFunction(final List<Student> students, final Function <Student, T> mapper) {
        return (List<T>) getStudentsByFunction(students, mapper, Collectors.toList());
    }

    private Stream<Student> getSortedStudentsStream(final Collection<Student> student,
                                                    final Comparator<Student> comparator) {
        return student.stream().sorted(comparator);
    }

    private List<Student> findStudentsByPredicate(final Collection<Student> students,
                                                  final Predicate<? super Student> predicate) {
        return sortStudentsByName(students.stream().filter(predicate).collect(Collectors.toList()));
    }

    private List<Group> getGroupsByFunction(final Collection<Student> students,
                                            final Function<? super List<Student>, ? extends List<Student>> function) {
        return getStudentsStreamGroupingByFunction(students, Student::getGroup).
                entrySet().stream().
                sorted(Map.Entry.comparingByKey()).
                map((Map.Entry <String, List<Student>> o) -> new Group(o.getKey(), function.apply(o.getValue()))).
                collect(Collectors.toList());
    }

    private String getStreamLargestGroup(final Stream<Map.Entry<String, Long>> stream) {
        return stream.
                min(Map.Entry.<String, Long>comparingByValue().reversed().
                        thenComparing(Map.Entry.comparingByKey())).
                map(Map.Entry::getKey).orElse("");
    }
}
