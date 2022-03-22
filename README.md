# Description
Java service that serves JSON requests for class and object operations.
It is also the repository of live objects created by the client.
  
* It reads JSON from stdin operations for
  1. object instantations, providing a string instance key (handle/id) for the new object
  1. class and instance method invocations, specifying the class or the instance key
  1. instance destruction via service.remove(key)
* It writes JSON responses to these calls consisting of one of...
  1. Null if the target method is a void method, or if call was instantiate()
     or remove()
  1. A scalar, structured, or collection value, being the return value of a
     method invocation

Implementation language is Groovy.

Build system is Gradle.

# Installation
The deliverable is a Java 11 jar file with entry point.
One simple way to run the service is:
```
java -jar path/to/jsonproxy.jar
```
