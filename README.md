# SolrLib
_an easy to use solr client, that works embedded or with SolrServer and SolrCloud._

[![Build Status](https://travis-ci.org/redlink-gmbh/solrlib.svg?branch=master)](https://travis-ci.org/redlink-gmbh/solrlib)
[![Maven Central](https://img.shields.io/maven-central/v/io.redlink.solrlib/solrlib.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.redlink.solrlib%22)
[![Sonatype (Snapshots)](https://img.shields.io/nexus/s/https/oss.sonatype.org/io.redlink.solrlib/solrlib.svg)](https://oss.sonatype.org/#nexus-search;gav~io.redlink.solrlib~solrlib~~~)
[![Javadocs](https://www.javadoc.io/badge/io.redlink.solrlib/solrlib.svg)](https://www.javadoc.io/doc/io.redlink.solrlib/solrlib)

**SolrLib** aims to ease the use of [Apache Solr](http://lucene.apache.org/solr/) within Java applications.

Often you want to internally use Solr - **SolrLib** helps launching up an embedded CoreContainer. The big advantage: You
keep the core/collection configuration where it belongs - next to the code.
If later, you want to switch to using an external Solr Server (classic or SolrCloud), you can do so without code-changes.
**SolrLib** will even _deploy_/_update_ your collections on the remote instances.

## Usage

```xml
<dependency>
    <groupId>io.redlink.solrlib</groupId>
    <artifactId>solrlib</artifactId>
    <version>${VERSION}</version>
</dependency>
```

**SolrLib** is split up in several modules:
* `solrlib-api`
* `solrlib-embedded`
* `solrlib-standalone`
* `solrlib-cloud`

For convenience, there is also a `solrlib-spring-boot-autoconfigure` module for ease-of-use within 
[Spring Boot](https://projects.spring.io/spring-boot/) environments.

Cores/collections are registered by using a `SolrCoreDescriptor`, e.g. a `SimpleCoreDescriptor`:

```java
// Create a core-descriptor
CoreDescriptor myCore = new SimpleCoreDescriptor("my-core", Paths.get("/path/to/solr-conf"));

// Create SolrCoreContainer and register Core
EmbeddedCoreContainerConfiguration config = new EmbeddedCoreContainerConfiguration();
config.setHome(solrHome);
config.setDeleteOnShutdown(true);

SolrCoreContainer coreContainer = new EmbeddedCoreContainer(Collections.singleton(coreDescriptor), config, null);
coreContainer.initialize();

// ...

// retrieve a SolrClient
try (SolrClient solrClient = coreContainer.getSolrClient(myCore)) {
    solrClient.ping().getStatus();
}
```

### Embedded Mode

When using `solrlib-embedded`, an embedded CoreContainer will be launched. There is no direct
access to the Solr webservices or the admin-ui, you can retrieve an `SolrClient` to execute
queries in your code.

Upon `initialize()`, all registered `CoreDescriptors` will be deployed. 
`EmbeddedCoreContainerConfiguration.setDeleteOnShutdown` controls if solr-home will be deleted upon
shutdown.

### Standalone Mode

When using `solrlib-standalone`, _SolrLib_ connects to an external Solr server via http. If 
the home-directory is configured, the registered cores are copied there and registered via the
_Solr Core Admin API_. The configuration flag `deployCores` chan further disable this.

### Cloud Mode

In `solrlib-cloud`, _SolrLib_ connects to an SolrCloud ensemble via the provided zookeeper connection
string.
If the configuration flag `deployCores` is set, all registered cores will be deployed to SolrCloud
using `CloudSolrClient.uploadConfig`. 

**NOTE:** adding runtime-libraries from the `lib` folder is currently not supported!

### Spring Boot

To use **SolrLib** in a Spring Boot environment, add the following dependencies to your project:
```xml
<dependencies>
    <!-- Spring Boot Autoconfiguration for SolrLib -->
    <dependency>
        <groupId>io.redlink.solrlib</groupId>
        <artifactId>solrlib-spring-boot-autoconfigure</artifactId>
        <version>${solrlib.version}</version>
    </dependency>
    <!-- at least on implementation for runtime -->
    <dependency>
        <groupId>io.redlink.solrlib</groupId>
        <artifactId>solrlib-embedded</artifactId>
        <version>${solrlib.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.redlink.solrlib</groupId>
        <artifactId>solrlib-standalone</artifactId>
        <version>${solrlib.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.redlink.solrlib</groupId>
        <artifactId>solrlib-cloud</artifactId>
        <version>${solrlib.version}</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

The autoconfiguration checks solrlib with the following priority, the first one to match will be used:

1. **SolrLib Cloud** (requires `solrlib.zk-connection` to be set)
1. **SolrLib Standalone** (requires `solrlib.base-url` to be set)
1. **SolrLib Embedded** (fallback)

All supported configuration properties for **SolrLib**:

```properties
# Used by embedded and standalone
#      the ${SOLR_HOME} directory
solrlib.home = /path/to/solr/home
    
# This will trigger using solrlib-standalone if available on the classpath
#      base-url for all solr requests
solrlib.base-url = http://localhost:8983/solr
    
# This will trigger using solrlib-cloud if available on the classpath
#       ZooKeeper connection string
solrlib.zk-connection = zookeeper1:8121,zookeeper2:8121
    
# Only used by standalone and cloud
#      prefix for the remote collection names, 
#      to avoid name-clashes on shared servers. 
solrlib.collection-prefix = 
    
# Only relevant in cloud-mode
solrlib.max-shards-per-node = 1
        
# Only used by standalone and cloud
#      option to disable automatic configuration update/deployment
#      to remote servers. You might not have the karma to do so.
solrlib.deploy-cores = true
    
# Only used by embedded
#      option to delete the solrlib-home upon shutdown
solrlib.delete-on-shutdown = false
```

## License

**SolrLib** is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
