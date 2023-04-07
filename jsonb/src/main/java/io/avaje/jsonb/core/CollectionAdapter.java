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
package io.avaje.jsonb.core;

import io.avaje.jsonb.*;
import io.avaje.jsonb.spi.ViewBuilder;
import io.avaje.jsonb.spi.ViewBuilderAware;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Converts collection types to JSON arrays containing their converted contents.
 */
abstract class CollectionAdapter<C extends Collection<T>, T> implements JsonAdapter<C>, ViewBuilderAware {

  static final JsonAdapter.Factory FACTORY = (type, jsonb) -> {
    Class<?> rawType = Util.rawType(type);
    if (rawType == List.class || rawType == Collection.class) {
      return newListAdapter(type, jsonb).nullSafe();
    } else if (rawType == Set.class) {
      return newSetAdapter(type, jsonb).nullSafe();
    }
    return null;
  };

  static <T> JsonAdapter<Collection<T>> newListAdapter(Type type, Jsonb jsonb) {
    Type elementType = Util.collectionElementType(type);
    JsonAdapter<T> elementAdapter = jsonb.adapter(elementType);
    return new CollectionAdapter<Collection<T>, T>(elementAdapter) {
      @Override
      Collection<T> newCollection() {
        return new ArrayList<>();
      }
    };
  }

  static <T> JsonAdapter<Set<T>> newSetAdapter(Type type, Jsonb jsonb) {
    Type elementType = Util.collectionElementType(type);
    JsonAdapter<T> elementAdapter = jsonb.adapter(elementType);
    return new CollectionAdapter<Set<T>, T>(elementAdapter) {
      @Override
      Set<T> newCollection() {
        return new LinkedHashSet<>();
      }
    };
  }

  private final JsonAdapter<T> elementAdapter;

  private CollectionAdapter(JsonAdapter<T> elementAdapter) {
    this.elementAdapter = elementAdapter;
  }

  abstract C newCollection();

  @Override
  public boolean isViewBuilderAware() {
    return elementAdapter.isViewBuilderAware();
  }

  @Override
  public ViewBuilderAware viewBuild() {
    return this;
  }

  @Override
  public void build(ViewBuilder builder, String name, MethodHandle handle) {
    builder.addArray(name, elementAdapter, handle);
  }

  @Override
  public C fromJson(JsonReader reader) {
    C result = newCollection();
    reader.beginArray();
    while (reader.hasNextElement()) {
      result.add(elementAdapter.fromJson(reader));
    }
    reader.endArray();
    return result;
  }

  @Override
  public void toJson(JsonWriter writer, C value) {
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
