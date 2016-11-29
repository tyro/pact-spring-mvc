/*
 * #%L
 * pact-spring-mvc
 * %%
 * Copyright (C) 2016 Tyro Payments Pty Ltd
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.tyro.oss.pact.spring4.pact.examples.stateful.contract.api;

import static java.util.Arrays.asList;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


import com.google.common.base.Objects;

public class BookCollectionDTO {
    private final Set<BookDTO> books = new HashSet<>();

    public BookCollectionDTO() {
    }

    public BookCollectionDTO(Collection<BookDTO> books) {
        this.books.addAll(books);
    }

    public BookCollectionDTO(BookDTO... books) {
        this(asList(books));
    }

    public Collection<BookDTO> getBooks() {
        return Collections.unmodifiableCollection(books);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BookCollectionDTO that = (BookCollectionDTO) o;
        return Objects.equal(books, that.books);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(books);
    }

    @Override
    public String toString() {
        return "BookCollectionDTO{" +
                "books=" + books +
                '}';
    }
}
