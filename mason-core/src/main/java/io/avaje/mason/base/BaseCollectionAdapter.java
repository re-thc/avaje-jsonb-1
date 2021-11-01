/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.avaje.mason.base;

import io.avaje.mason.JsonAdapter;
import io.avaje.mason.JsonReader;
import io.avaje.mason.JsonWriter;
import io.avaje.mason.Jsonb;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Converts collection types to JSON arrays containing their converted contents.
 */
abstract class BaseCollectionAdapter<C extends Collection<T>, T> extends JsonAdapter<C> {

  public static final JsonAdapter.Factory FACTORY =
    (type, annotations, moshi) -> {
      Class<?> rawType = UtilTypes.getRawType(type);
      if (!annotations.isEmpty()) return null;
      if (rawType == List.class || rawType == Collection.class) {
        return newArrayListAdapter(type, moshi).nullSafe();
      } else if (rawType == Set.class) {
        return newLinkedHashSetAdapter(type, moshi).nullSafe();
      }
      return null;
    };

  private final JsonAdapter<T> elementAdapter;

  private BaseCollectionAdapter(JsonAdapter<T> elementAdapter) {
    this.elementAdapter = elementAdapter;
  }

  static <T> JsonAdapter<List<T>> listOf(JsonAdapter<T> base) {
    return new BaseCollectionAdapter<List<T>, T>(base) {
      @Override
      List<T> newCollection() {
        return new ArrayList<>();
      }
    };
  }

  static <T> JsonAdapter<Collection<T>> newArrayListAdapter(Type type, Jsonb jsonb) {
    Type elementType = UtilTypes.collectionElementType(type, Collection.class);
    JsonAdapter<T> elementAdapter = jsonb.adapter(elementType);
    return new BaseCollectionAdapter<Collection<T>, T>(elementAdapter) {
      @Override
      Collection<T> newCollection() {
        return new ArrayList<>();
      }
    };
  }

  static <T> JsonAdapter<Set<T>> newLinkedHashSetAdapter(Type type, Jsonb jsonb) {
    Type elementType = UtilTypes.collectionElementType(type, Collection.class);
    JsonAdapter<T> elementAdapter = jsonb.adapter(elementType);
    return new BaseCollectionAdapter<Set<T>, T>(elementAdapter) {
      @Override
      Set<T> newCollection() {
        return new LinkedHashSet<>();
      }
    };
  }

  abstract C newCollection();

  @Override
  public C fromJson(JsonReader reader) throws IOException {
    C result = newCollection();
    reader.beginArray();
    while (reader.hasNextElement()) {
      result.add(elementAdapter.fromJson(reader));
    }
    reader.endArray();
    return result;
  }

  @Override
  public void toJson(JsonWriter writer, C value) throws IOException {
    if (value.isEmpty()) {
      writer.emptyArray();
      return;
    }
    writer.beginArray();
    for (T element : value) {
      elementAdapter.toJson(writer, element);
    }
    writer.endArray();
  }

  @Override
  public String toString() {
    return elementAdapter + ".collection()";
  }
}
