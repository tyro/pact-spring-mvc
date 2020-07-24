/*
 * #%L
 * pact-spring-mvc
 * %%
 * Copyright (C) 2016 - 2020 Tyro Payments Pty Ltd
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
package com.tyro.oss.pact.spring.pact.examples.stateful.contract.provider.controller;

import com.tyro.oss.pact.spring.pact.examples.stateful.contract.api.BookCollectionDTO;
import com.tyro.oss.pact.spring.pact.examples.stateful.contract.api.BookDTO;
import com.tyro.oss.pact.spring.pact.examples.stateful.contract.provider.repository.Bookshelf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
public class BookshelfController {

    @Autowired
    private Bookshelf bookshelf;


    @RequestMapping(value = "/read")
    public BookCollectionDTO getReadBooks() {
        List<BookDTO> readBooks = bookshelf.getBooks().stream().filter(BookDTO::isRead).collect(toList());
        return new BookCollectionDTO(readBooks);
    }


    @RequestMapping(value = "/shelf")
    public BookCollectionDTO getShelvedBooks() {
        List<BookDTO> shelvedBooks = bookshelf.getBooks().stream().filter(BookDTO::isOnShelf).collect(toList());
        return new BookCollectionDTO(shelvedBooks);
    }
}
