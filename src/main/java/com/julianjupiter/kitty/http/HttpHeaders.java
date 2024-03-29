/*
 * Copyright 2013-2023 Julian Jupiter.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.julianjupiter.kitty.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Julian Jupiter
 */
class HttpHeaders {
    private final Map<String, HttpHeader> headers = new HashMap<>();

    private HttpHeaders() {
    }

    private HttpHeaders(List<HttpHeader> headers) {
        if (headers != null) {
            headers.forEach(httpHeader -> this.headers.put(httpHeader.name(), httpHeader));
        }
    }

    public static HttpHeaders create() {
        return new HttpHeaders();
    }

    public static HttpHeaders create(List<HttpHeader> headers) {
        return new HttpHeaders(headers);
    }

    public int size() {
        return this.headers.size();
    }

    public boolean isEmpty() {
        return this.headers.isEmpty();
    }


    public boolean contains(String name) {
        return this.headers.containsKey(normalize(name));
    }

    public List<HttpHeader> get() {
        return this.headers.values().stream()
                .toList();
    }

    public Optional<HttpHeader> get(String name) {
        return Optional.ofNullable(this.headers.get(normalize(name)));
    }

    public HttpHeader getOrDefault(String name, Set<String> defaultValue) {
        return this.headers.getOrDefault(name, new HttpHeader(normalize(name), defaultValue));
    }

    public HttpHeaders add(String name, Set<String> values) {
        values.forEach(HttpHeaders::checkValue);
        this.headers.put(normalize(name), new HttpHeader(normalize(name), values));
        return this;
    }

    public HttpHeaders add(HttpHeader header) {
        header.values().forEach(HttpHeaders::checkValue);
        this.headers.put(normalize(header.name()), new HttpHeader(normalize(header.name()), header.values()));
        return this;
    }

    public HttpHeaders addAll(List<HttpHeader> headers) {
        headers.forEach(this::add);
        return this;
    }

    public HttpHeaders replace(String name, Set<String> newValue) {
        this.headers.replace(normalize(name), new HttpHeader(normalize(name), newValue));
        return this;
    }

    public HttpHeaders remove(String name) {
        this.headers.remove(normalize(name));
        return this;
    }

    public HttpHeaders clear() {
        this.headers.clear();
        return this;
    }

    @Override
    public String toString() {
        return this.headers.values().stream()
                .map(HttpHeader::toString)
                .collect(Collectors.joining("\n"));
    }

    private String normalize(String name) {
        Objects.requireNonNull(name);

        var length = name.length();
        if (length == 0) {
            return name;
        }

        var parts = name.toLowerCase().split("-");

        for (var i = 0; i < parts.length; i++) {
            char[] chars = parts[i].toCharArray();
            for (char c : chars) {
                if (c < 'a' || c > 'z') {
                    throw new IllegalArgumentException("Illegal character in HTTP header name");
                }
            }

            chars[0] = Character.toUpperCase(chars[0]);
            parts[i] = new String(chars);
        }

        return String.join("-", parts);
    }

    private static void checkValue(String value) {
        var length = value.length();
        for (var i = 0; i < length; i++) {
            var c = value.charAt(i);
            if (c == '\r') {
                // is allowed if it is followed by \n and a whitespace char
                if (i >= (length - 2)) {
                    throw new IllegalArgumentException("Illegal CR found in header");
                }

                var c1 = value.charAt(i + 1);
                var c2 = value.charAt(i + 2);
                if (c1 != '\n') {
                    throw new IllegalArgumentException("Illegal character found after CR in header");
                }

                if (c2 != ' ' && c2 != '\t') {
                    throw new IllegalArgumentException("No whitespace found after CRLF in header");
                }

                i += 2;
            } else if (c == '\n') {
                throw new IllegalArgumentException("Illegal LF found in header");
            }
        }
    }
}
