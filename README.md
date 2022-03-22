# Description
Java service that servers JSON requests for class and object operations.
* It reads JSON from stdin operations for object instantations and class and object methods
* It writes JSON responses to these calls consisting of one of...
  1. Null if the target method is a void method
  1. An object identifier/reference (probably an integer or uuid of some sort TBD)
  1. A scalar, structured, or collection value

# Installation
The deliverable is a Java 11 jar file with entry point.
One simple way to run the service is:
```
java -jar path/to/jsonproxy.jar
```
