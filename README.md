Runtime non-null assertions with Eclipse
========================================

This is a small compile participant that inserts runtime non-null checks for parameters and methods that are annotated with @NonNull annotations. It is not bound to a limited
set of annotations but harmonizes among different specifications with different APIs instead. The compiler extension works for javax.annotations, the Eclipse JDT nullness annotations and others.

This feature was inspired by the equivalent [functionality](http://www.jetbrains.com/idea/features/annotation_java.html) in IntelliJ IDEA, where the "compiler can be configured to track @NotNull annotation compliance during the application runtime."

What does it do?
----------------

The compiler that inserts the runtime assertions does not depend on JDTs static annotation processing. In fact it is significantly different from that (and thereby works like a complementary addition): Nullness annotations are considered to be part of the contract that a class defines. If you inherit from an annotated class, it should not be necessary to re-specify all its constraints again. In fact the code that is written should be checked against the inherited specification albeit the repeated annotations. That is the value what this project does. All the inherited nullness constraints are taken into account and your code is enhanced at compile time by non-null checks if appropriate. It's pretty much the equivalent to Java's runtime type checks.

As soon as you implement a non-null method but return null by accident, an exception is thrown at runtime that tells you that something went wrong:

```
 Non-null method MyType#myMethod must not return null
```

The same applies for parameters that are specified as non-null. If you pass a null value, an exception is raised with a meaningful message:

```
 Argument for non-null parameter aParam at index 1 of MyType#myMethod must not be null
```

Binaries
--------
[![Build Status](https://dhuebner.ci.cloudbees.com/job/Nullness-nightly/badge/icon)](https://dhuebner.ci.cloudbees.com/job/Nullness-nightly/)
