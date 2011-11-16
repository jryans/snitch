Snitch: In JVM monitoring library
=================================

Introduction
------------
Snitch is an in-JVM monitoring library that allows users to easily expose various counters and metrics in their code to
be monitored by a 3rd party.  The goal of Snitch is to make it as easy as possible for a developer to expose variables
outside of the application while at the same time imposing as minimal overhead on the application as possible.

Usage
-----
Using Snitch is incredibly easy and is best illustrated by an example.  Suppose you've created a service and you'd like
to use Snitch to keep track of the number of requests that have been processed by it:

    import java.util.concurrent.atomic.AtomicLong;
    import org.bazaarvoice.snitch.Monitored;

    public class MyService {
      @Monitored("request-count")
      private static final AtomicLong HANDLED_REQUEST_COUNT = new AtomicLong();

      public void handleRequest() {
        HANDLED_REQUEST_COUNT.incrementAndGet();

        // ... handle the request ...
      }
    }

It's as simple as that.  Snitch is annotation based and anything that is marked with the ```@Monitored``` annotation
will be found and monitored by it.

Snitch will also look for methods that are tagged with the ```@Monitored``` annotation
as well and monitor their return values as well.  The above example could have also been written:

    import java.util.concurrent.atomic.AtomicLong;
    import org.bazaarvoice.snitch.Monitored;

    public class MyService {
      private static final AtomicLong HANDLED_REQUEST_COUNT = new AtomicLong();

      @Monitored("request-count")
      private long getHandledRequestCount() {
        return HANDLED_REQUEST_COUNT.get();
      }

      public void handleRequest() {
        HANDLED_REQUEST_COUNT.incrementAndGet();

        // ... handle the request ...
      }
    }

In order to enable Snitch in a JVM, the JVM must be started with the Snitch java agent.
The java agent gives Snitch the ability to find fields and methods marked with the annotation.  To start a JVM with the
Snitch java agent, just specify the Snitch jar as the agent.

    java -javaagent:snitch.jar <args>

Snitch also supports several arguments that can be specified on the commandline to control how Snitch works.  These are
outlined below in the Agent Arguments section.

Custom Naming Strategies
------------------------
It's possible to provide a custom naming strategy that implements the ```com.bazaarvoice.snitch.NamingStrategy```
interface.  This interface allows the user to control how variable names are determined.  It's possible to write a
custom naming strategy that understands organizational naming conventions for variables and to reverse them when
translating a variable's or method's name into a display name.  It also gives the naming strategy the ability to use
the properties of a custom annotation to build the name of a variable.

Custom Annotations
------------------
It's possible to specify a custom annotation to use as an argument to the agent.  If one is provided the agent will
search loaded classes for static methods and fields that are marked with that annotation class.  It will provide the
instance of the annotation to the naming strategy so that it has the opportunity to use custom properties of the
annotation when determining the name of a variable.

Agent Arguments
---------------
There are several arguments that can be provided to the Snitch java agent.

**```packages```** - A colon separated list of packages that Snitch should check for annotations<br/>
**```annotation```** - The fully qualified name of the annotation class that Snitch should search for<br/>
**```naming```** - The fully qualified name of the naming strategy class to use for discovered fields and methods<br/>

Arguments are separated by commas and specified as ```key=value``` pairs.  For example:

    java -javaagent:snitch.jar=packages=com.foo:com.bar,annotation=com.foo.MyAnnotation,naming=com.foo.MyNaming



