# pact-spring-mvc

[![Download](https://maven-badges.herokuapp.com/maven-central/com.tyro.oss.pact/pact-spring-mvc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.tyro.oss.pact/pact-spring-mvc)
[![Build Status](https://travis-ci.org/tyro/pact-spring-mvc.svg?branch=master)](https://travis-ci.org/tyro/pact-spring-mvc)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

A library to enable recording/publishing and download/playback of PACT test files.

This library is not thread safe - making mock mvc calls from multiple threads will not work. This reflects the underlying behaviour of the spring test framework.

## Purpose

This project allows you to record your integration tests (using a mocked server) against a client and then replay them against the real server to make sure your
mocks are still faking the server correctly and that your server and client are playing nicely.

## Getting Started

`pact-spring-mvc` is available on Maven Central.
```xml
<dependency>
    <groupId>com.tyro.oss.pact</groupId>
    <artifactId>pact-spring-mvc</artifactId>
    <version>2.0.1</version>
    <scope>test</scope>
</dependency>
```

`pact-spring-mvc` requires URLs for a [PACT broker](https://github.com/bethesque/pact_broker) in order to publish and download PACT files.
You will need to specify the URLs when using this library. This can be done by:
    1. Specifying System Properties with maven or on the command line. (higher precedence)
    2. A properties file named pact-broker.properties that needs to exist on the test classpath.

The required properties are:

*  `pact.broker.publish.url` - used to publish the pact on behalf of the consumer
*  `pact.broker.download.url` - used to download the pact on behalf of the provider

If you don't specify these properties an exception will be thrown.

## Maven Configuration to publish PACTs
    <profiles>
        <profile>
            <id>publish-pact</id>
            <activation>
                <activeByDefault>false</activeByDefault>
            </activation>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.codehaus.mojo</groupId>
                        <artifactId>exec-maven-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>publish-all-pacts</id>
                                <phase>install</phase>
                                <goals>
                                    <goal>java</goal>
                                </goals>
                                <configuration>
                                    <mainClass>com.tyro.oss.pact.spring.pact.consumer.PactPublisher</mainClass>
                                    <classpathScope>test</classpathScope>
                                    <commandlineArgs>{{your-artifactName Here}} ${project.version} ${project.basedir}/target/pact/</commandlineArgs>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>

## Trial by Example

The following sections describe different use case examples.  These examples can be found in the test tree of the project in all their glory.

### Simple Contract

The most basic example of a contract test is the retrieval of a object.  In this case, we will be retrieving a DTO containing a single integer.  The request
contains no request body or parameters.  We will be wrapping this call in an IntegerService, shown below.

~~~
public class IntegerService {

    private RestRequestInvoker restRequestInvoker;

    public IntegerService(RestTemplate restTemplate) {
        this.restRequestInvoker = new RestRequestInvoker(restTemplate);
    }

    public IntegerDTO getLatestInteger() {
        return restRequestInvoker.forDescriptor(new RestRequestDescriptor<>("/integer", HttpMethod.GET, null, IntegerDTO.class));
    }
}
~~~

The service itself is quite simple, it creates a RestRequestDescriptor and passes it to our invoker to make the call.  The details of both are left as an
exercise for the reader :)

Next we will define our contract expectations. We create the class SimpleConsumerPactTest.

~~~
class SimpleConsumerPactTest {

    ...
    private TuPactRecordingServer recordingServer;
    private IntegerService integerService;

    @BeforeEach
    void setup() throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        integerService = new IntegerService(restTemplate);

        File pactFile = new File("target/pact/example_provider_pacts.json");
        recordingServer = TuPactRecordingServer.createServer(restTemplate, pactFile);
    }

    ...

    @AfterEach
    void closeTuPact() throws Exception {
        recordingServer.close();
    }
}
~~~

In our setup, create a new TuPactRecordingServer with a RestTemplate and the File to which we will write our contract.  If the file
already exists, we will append our contracts to it, so it is helpful to place it in the target directory to allow `mvn clean` to clean
it up for us.  We also initialise our service under test with the same RestTemplate.

We also need to remember to close our RecordingServer in our teardown to verify our expectations were fulfilled, ensure our contract
file is flushed to disk and closed, and return the RestTemplate to its original state.

~~~
class SimpleConsumerPactTest {

    @BeforeEach
    ...

    @Test
    void shouldRetrieveDefaultValue(TestInfo testInfo) {
        recordingServer.startWorkflow(testInfo.getDisplayName());

        recordingServer.expect(new RestRequestDescriptor<>("/integer", HttpMethod.GET, null, IntegerDTO.class))
                .andReturn(new IntegerDTO(0));

        assertThat(integerService.getLatestInteger(), is(new IntegerDTO(0)));
    }

    @AfterEach
    ...
}
~~~

The test itself needs to initialise a new TuPact workflow, giving it an name that can be used to trace back to the contract definition
if it happens to fail on the provider side.  Using the TestName rule has proven to be most useful when you have access to both the consumer
and provider.

Once the workflow has been named, we can start defining expected behaviour.  In our case, we expect the same RestRequestDescriptor and return
an IntegerDTO with a value of 0.  We are then set up and can test our services behaviour.

Running that test, we can see the file we declared in the setup is created in the target directory.  We will use this file directly, instead of
using a Pact Broker, to build our provider test.

Our provider controller for this example is even simpler than our service.

~~~
@RestController
public class IntegerController {

    @RequestMapping(value = "/integer")
    public IntegerDTO getLatestInteger() {
        return new IntegerDTO(0);
    }
}
~~~

As you can see, we have hard-coded the IntegerDTO value, so there is no state to confuse our tests.

~~~
@PactDefinition(
        provider = "example-provider",
        consumer = "example-consumer",
        localPactFilePath = "target/pact/example_provider_pacts.json"
)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SimpleWebConfig.class)
@WebAppConfiguration
public class SimpleProviderPactTest extends PactTest {

    @Override
    protected String getServletContextPathWithoutTrailingSlash() {
        return "/servletContextPath";
    }
}
~~~

The pact test is created by extending PactTest and defining the pacts to execute using the PactDefinition annotation.  The `provider` and
`consumer` fields are required and must align with those used by the consumer when publishing the pact file to the Broker.  In our case, however,
we are overriding that behaviour using the `localPactFilePath` field to indicate the same file we generated above.

    Note: the use of localPactFilePath in our

    examples creates a dependency between our two tests.
    The consumer test must be run before the provider test.

We have also added annotations to define our configuration and declare us a web app.  Finally, we have overridden the ```getServletContextPathWithoutTrailingSlash```.

Assuming our ```WebConfig``` and controller are properly configured (they are), this is sufficient to find the pact file,
load it and execute it in the WebContext.

### Stateful Contract

The simple example is just that, simple.  It assumes you have a provider with no internal state that can affect the results of your request. So how do you
deal with a state-dependent contract?

Our next example enforces the contract of a bookshelf application.  On the consumer side, we have a simple service that wraps two calls to the provider, to
get a listing of books you own and to get a listing of books you've read.

~~~
public class BookshelfService {

    private RestRequestInvoker restRequestInvoker;

    public BookshelfService(RestTemplate restTemplate) {
        this.restRequestInvoker = new RestRequestInvoker(restTemplate);
    }

    public BookCollectionDTO getBooksOwned() {
        return restRequestInvoker.forDescriptor(new RestRequestDescriptor<>("/shelf", HttpMethod.GET, null, BookCollectionDTO.class));
    }

    public BookCollectionDTO getBooksRead() {
        return restRequestInvoker.forDescriptor(new RestRequestDescriptor<>("/read", HttpMethod.GET, null, BookCollectionDTO.class));
    }
}
~~~

The provider can only meet the contract the consumer will define if it has the same assumptions about its default state. One option is to simply agree on a
default state, but that creates a bit of a maintenance nightmare.  We have taken the approach of separating the responsibility of defining the state between
the provider and the consumer. The provider is responsible for defining methods to build the expected state, and the consumer is responsible for providing the
required initialisation data for those methods.

The setup of this consumer test, below, is similar to the previous one, but it adds calls to the ```inState``` method.  This method records the name of the provider
state and zero or more objects that will be passed into it. These objects must conform to the method signature provided by the provider.

~~~
class StatefulConsumerPactTest {

    ...

    @BeforeEach
    void setup() throws Exception {
        ...

        shelvedRead = new BookDTO("Essentialism: The Disciplined Pursuite of Less", true, true);
        shelvedUnread = new BookDTO("Quiet: The Power of Introverts in a World That Can't Stop Talking", true, false);
        unshelvedRead = new BookDTO("Harry Potter and the Order of the Phoenix", false, true);
        unshelvedUnread = new BookDTO("The Winds of Winter", false, false);

        recordingServer = TuPactRecordingServer.createServer(restTemplate, pactFile)
                .inState("withKnownBook", shelvedRead)
                .inState("withKnownBook", shelvedUnread)
                .inState("withKnownBook", unshelvedRead)
                .inState("withKnownBook", unshelvedUnread);
    }

    @Test
    ...
}
~~~

The test methods below proceed in the same manner as the previous example, except that now our expectations can reflect the state we've defined above.

~~~
class StatefulConsumerPactTest {

    @BeforeEach
    ...

    @Test
    void shouldRetrieveEntireBookshelf(TestInfo testInfo) {
        recordingServer.startWorkflow(testInfo.getDisplayName());

        recordingServer
                .expect(getBooksOwnedDescriptor())
                .andReturn(new BookCollectionDTO(asList(shelvedRead, shelvedUnread)));

        assertThat(bookshelfService.getBooksOwned(), is(new BookCollectionDTO(shelvedRead, shelvedUnread)));
    }

    @Test
    void shouldRetrieveEntireReadList(TestInfo testInfo) {
        recordingServer.startWorkflow(testInfo.getDisplayName());

        recordingServer
                .expect(getBooksReadDescriptor())
                .andReturn(new BookCollectionDTO(asList(shelvedRead, unshelvedRead)));

        assertThat(bookshelfService.getBooksRead(), is(new BookCollectionDTO(shelvedRead, unshelvedRead)));
    }

    @AfterEach
    ...
}
~~~

The provider test, below, has one additional feature to support the loading of shared state.  The ```@ProviderState``` defines a method that the PactRunner
will call to load the objects added to the contract by the ```withState``` call above.

~~~
@PactTestRunner.PactDefinition(
        provider = "example-provider",
        consumer = "example-consumer",
        localPactFilePath = "target/pact/stateful_contract_pacts.json"
)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StatefulWebConfig.class)
@WebAppConfiguration
class StatefulProviderPactTest extends PactTest {

    @Autowired
    private Bookshelf bookshelf;

    @Override
    protected String getServletContextPathWithoutTrailingSlash() {
        return "/servletContextPath";
    }

    @ProviderState
    public void withKnownBook(BookDTO book) {
        bookshelf.addBook(book);
    }
}
~~~

## Copyright and Licensing

Copyright (C) 2016 - 2020 Tyro Payments Limited

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Contributing

See [CONTRIBUTING](CONTRIBUTING.md) for details.