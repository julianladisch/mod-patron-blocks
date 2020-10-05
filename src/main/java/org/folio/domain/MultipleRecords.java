package org.folio.domain;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Stream.concat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MultipleRecords<T> {
  private static final String TOTAL_RECORDS_PROPERTY_NAME = "totalRecords";

  private final Collection<T> records;
  private final Integer totalRecords;

  public MultipleRecords(Collection<T> records, Integer totalRecords) {
    this.records = records;
    this.totalRecords = totalRecords;
  }

  public static <T> MultipleRecords<T> empty() {
    return new MultipleRecords<>(new ArrayList<>(), 0);
  }

  public static <T> Future<MultipleRecords<T>> from(JsonObject representation,
                                                    Function<JsonObject, T> mapper,
                                                    String recordsPropertyName) {

    List<T> wrappedRecords = mapToList(representation, recordsPropertyName, mapper);
    Integer totalRecords = representation.getInteger(TOTAL_RECORDS_PROPERTY_NAME);

    return Future.succeededFuture(new MultipleRecords<>(
      wrappedRecords, totalRecords));
  }

  public <R> Set<R> toKeys(Function<T, R> keyMapper) {
    return getRecords().stream()
      .map(keyMapper)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
  }

  public Map<String, T> toMap(Function<T, String> keyMapper) {
    return getRecords().stream()
      .collect(Collectors.toMap(keyMapper, identity(),
        (record1, record2) -> record1));
  }

  /**
   * Maps the records within a multiple records collection
   * using the providing mapping function
   * @param mapper function to map each record to new record
   * @param <R> Type of record to map to
   * @return new multiple records collection with mapped records
   * and same total record count
   */
  public <R> MultipleRecords<R> mapRecords(Function<T, R> mapper) {
    return new MultipleRecords<>(
      getRecords().stream().map(mapper).collect(Collectors.toList()),
        getTotalRecords());
  }

  public MultipleRecords<T> combine(MultipleRecords<T> other) {
    final List<T> allRecords = concat(records.stream(), other.records.stream())
      .collect(Collectors.toList());

    return new MultipleRecords<>(allRecords, totalRecords + other.totalRecords);
  }

  public MultipleRecords<T> filter(Predicate<T> predicate) {
    final List<T> filteredRecords = getRecords().stream()
      .filter(predicate)
      .collect(Collectors.toList());

    final int numberOfFilteredOutRecords = totalRecords - filteredRecords.size();
    return new MultipleRecords<>(filteredRecords, totalRecords - numberOfFilteredOutRecords);
  }

  public JsonObject asJson(
    Function<T, JsonObject> mapper,
    String recordsPropertyName) {

    final List<JsonObject> mappedRecords = getRecords().stream()
      .map(mapper)
      .collect(Collectors.toList());

    return new JsonObject()
      .put(recordsPropertyName, new JsonArray(mappedRecords))
      .put(TOTAL_RECORDS_PROPERTY_NAME, totalRecords);
  }

  public Collection<T> getRecords() {
    return records;
  }

  public Integer getTotalRecords() {
    return totalRecords;
  }

  public boolean isEmpty() {
    return records.isEmpty();
  }

  public static <T> List<T> mapToList(JsonObject within, String arrayPropertyName,
                                      Function<JsonObject, T> mapper) {

    if (within == null || !within.containsKey(arrayPropertyName)) {
      return emptyList();
    }

    return mapToList(within.getJsonArray(arrayPropertyName), mapper);
  }

  public static <T> List<T> mapToList(JsonArray array, Function<JsonObject, T> mapper) {
    if (array == null) {
      return emptyList();
    }

    return toStream(array)
      .map(mapper)
      .collect(Collectors.toList());
  }

  public static Stream<JsonObject> toStream(JsonArray array) {
    return array
      .stream()
      .map(castToJsonObject())
      .filter(Objects::nonNull);
  }

  private static Function<Object, JsonObject> castToJsonObject() {
    return entry -> {
      if (entry instanceof JsonObject) {
        return (JsonObject)entry;
      }
      else {
        return null;
      }
    };
  }
}
