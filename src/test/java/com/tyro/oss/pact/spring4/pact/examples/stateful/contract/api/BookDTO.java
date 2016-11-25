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

import com.google.common.base.Objects;

public class BookDTO {
    public String name;
    public boolean onShelf;
    public boolean read;

    public BookDTO() {

    }

    public BookDTO(String name, boolean onShelf, boolean read) {
        this.name = name;
        this.onShelf = onShelf;
        this.read = read;
    }

    public String getName() {
        return name;
    }

    public boolean isOnShelf() {
        return onShelf;
    }

    public boolean isRead() {
        return read;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BookDTO bookDTO = (BookDTO) o;
        return Objects.equal(onShelf, bookDTO.onShelf)
                && Objects.equal(read, bookDTO.read)
                && Objects.equal(name, bookDTO.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, onShelf, read);
    }

    @Override
    public String toString() {
        return "BookDTO{" +
                "name='" + name + '\'' +
                ", onShelf=" + onShelf +
                ", read=" + read +
                '}';
    }
}
