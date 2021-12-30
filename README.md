#  Spring Native, GraalVM, and Kubernetes Native Java


Hi, Spring fans! You know I love _the cloud_ (cue Daft Punk's _The Grid_ theme from the Tron Legacy movie). I think it's an exciting way to build software. And, as you may have noticed, from literally everything I write, tweet, produce, blog, etc., I am also gaga for GraalVM and Spring Native. There's such an exciting opportunity here!

Spring provides an integration point for so much of the enterprise Java ecosystem, which means that we provide the clean glue code to integrate and adapt all the fantastic and best-of-breed bits into one cohesive whole. It's a dirty job, but somebody has to do it! So it was when Spring Framework arrived on the scene more than 20 years ago! Spring integrated many components from around the ecosystem and provided a component model that third-party libraries could use to give their integrations. So it was when we introduced Spring Boot. Spring Boot shipped with autoconfiguration that meant that the mere presence of types on the classpath could trigger some default and helpful behavior in your application. And, of course, the ecosystem rushed in to provide autoconfiguration and starters of their own, too. And so it is with Spring Native, which provides integrations for as many projects as possible to make those projects work flawlessly when used in a GraalVM native image context.

Except that Spring Native isn't a runtime framework like Spring Framework and Spring Boot are. No, Spring Native is a different beast altogether. It is a way to feed configuration into the graalvm native image compiler tool. GraalVm is a drop-in replacement for OpenJDK that offers a superior alternative to the Just-in-Time compiler HotSpot. But that's not all. It provides an intriguing little utility called `native-image`, and that utility is an ahead-of-time (AOT) compiler. Henceforth, I'll refer to GraalVM and the `native-image`  AOT  compiler utility interchangeably. Most people do anyway.

I hope that you're familiar with the JIT compiler's magic at this point: the Java runtime looks at frequently run code paths in your application and turns them into native code. This transition only kicks in after a certain threshold number of runs. The effect is that a medicare Java application can sometimes run faster than a well-written and analogous C or C++ application, whose memory shapes are rife with the memory spikes associated with local resource initialization, acquisition, and - very shortly after that - destruction. Large organizations with large applications sometimes exploit this dynamic, _warming_ their applications up by bragging them with traffic to incite the JIT before unleashing the applications to production.

If JITs such a good idea, why not go the whole nine yards and transpile everything into native code, soup to nuts? Why bother with adaptive compilation? This question (and the obvious answer) is the core conceit of the GraalVM `native-image` AOT engine. It takes your `.class` files and turns them into native code.

<!--  TODO in the more extended form version of this show how to set up a basic project using Spring Native, the Spring INitializr, and GraalVM  -->

Spring Native (and GraalVM) are almost like magic. _Almost_. Spring Native is not a silver bullet. GraalVM does static analysis at compile-time and throws away everything in your application that the compiler can't "reach." "Reach," in this case, means types that other types use in your compilation unit. Static analysis can only get you so far. "Runtime" - the very concept and all that it implies -  is an NP-incomplete problem, after all! There are many things that GraalVM doesn't support out-of-the-box unless you feed the graalvm native image compiler the appropriate configuration. the compiler will bake a sort of shim into the heap the native image so that your code appears to execute correctly. These things include but are not limited to reflection, serialization, proxies (Cglib and JDK-based proxies), JNI, and more.

For all of its warts and stodginess, Java is a very dynamic language that has more in common with the likes of python, ruby, javascript, Perl, Lua, Tcl, PHP, and Smalltalk than it does languages like C++ and C. Java is a very dynamic language. It's possible in a Java application to compile a class definition (from a `java.lang.String`) into a `.class` file, load that `.class`  into the `ClassLoader`, reflectively instantiate an instance of that class, invoke methods on that class, create proxies of that class, serialize instances of that class, and to do all this all without ever referencing a concrete type! You can do all that in terms of `java.lang.Object` and reflection, not once referencing a concrete class in your code. Java's beyond amazing! But GraalVM, well, GraalVM _hates_ all these fun things you can do. It doesn't know about them, and unless you feed it configuration telling it otherwise, it doesn't care about those things, either.

So, you have two, not necessarily mutually exclusive, paths forward. First, you could minimize those things that GraaLVM hates and provide configuration for the stuff you couldn't eliminate and still need. Spring Native helps with both.

Spring Native ships with build time plugins (for Maven and Gradle) and Spring AOT infrastructure. The Spring AOT infrastructure transforms your codebase in meaningful ways to eliminate or reduce things that GraalVM doesn't like. Things like reflection, proxies, and more.

<!--  
The more extended form version shows how it converts Spring when you run a new spring native build. factories into a class, and show how it transpires the `@Configuration` annotated code into functional configuration (also: make sure to deliver the functional configuration in the introduction chapter ) 
-->

Spring Native also ships with a system to contribute configuration to the graalvm compiler that it calls "hints." these are like the autoconfiguration of Spring Boot. Hints are bits of code that are aware of the Spring component model and your Spring beans. This mechanism is an extension plane, as well. Your organization can hook into these extension planes, too! They're also very different than autoconfiguration in that they are involved entirely in the compilation process at compile-time and have no impact on the runtime of the application. They exist to analyze the components in your Spring Boot-based component model and contribute hints accordingly. Is Spring Data on the classpath, and has the user-created any repositories? Well, that'll indeed require some configuration for the proxies created from your repository interface and for the reflection done to analyze the method signatures of that interface. Is Hibernate on the classpath? That'll require configuration, as well. What about any aspect-oriented code? Yep. That'll require special support. What about Spring Batch's `Step` scope and the proxies it creates? Yep. It'll require configuration. And the list goes on and on. Spring Native covers the common cases, but it can't know about every type ever written, so you'll need to provide your integrations as well.

<!-- TODO in a more extended version, this is where I would  -->


GraalVM native applications are small, and they start up very quickly. They take very little in the way of memory. Spring Native applications run inside containers with a minimal operating system. To the extent that it's possible, Spring Native makes Spring Boot and Spring Cloud applications even _more_ cloud-native! Trust me; I'm a professional. I even wrote [the book](https://www.amazon.com/Cloud-Native-Java-Designing-Resilient/dp/1449374646) on building cloud-native Java-based services.

<!-- Here I'm going to cut the big piece short with a discussion of Kubernetes native clients -->

One of the most exciting opportunities these days is using Spring Native to write memory-efficient, small-footprint controllers for Kubernetes. I love the idea. Kubernetes clients are typically code-generated from the schema of the objects in the Kubernetes API server, so who cares in what language a person writes their code? So long as the client can talk to the Kubernetes API server, then you're all set. All set, that is, until you have to deploy the client to production. You'll need to containerize the application in production and get it running. Spring Boot's got built-in support for buildpacks, which you can use to containerize your applications with ease: `mvn spring-boot:build-image` and then `docker tag` and `docker push` your image to production, and you're off to the races! Almost.   If you're using a complete JRE, this will require a more fully-featured operating system like Ubuntu. The JRE and the operating system add substantially to the OCI/Docker image size that contains your application. You can eliminate a lot of that extra heft by using Spring native to create native images with your GraalVm applications. Still, GraalVM is an all-or-nothing proposition: either all of your code works on a GraalVm context, or none of it does.

There are two outstanding Java clients you can use to create controllers in the Java ecosystem, and both do all sorts of stuff you won't appreciate in a GraalVM context. I wanted to make this easier, so I've built two Spring Native integrations, one for Fabric8 and another for the official Java Kubernetes client.

NB: I've gotten some pretty interesting examples working with these clients, but it's not to say that they're perfect. If u discover some poorly supported use case when building your operators with these integrations, _please, [let me know_ (@starbuxman) ](https://twitter.com/starbuxman)!

Let's take a look at them.

One of the most exciting opportunities with these clients is building custom Kubernetes resources and Kubernetes operators to manage those custom resources. If you think about it, this is the real power of Kubernetes: it's an API server! It cares about objects, and Spring is great at managing objects and lifecycles.

Specifically, a Kubernetes controller is what [the documentation](https://github.com/kubernetes/community/blob/8cafef897a22026d42f5e5bb3f104febe7e29830/contributors/devel/controllers.md) calls an "active reconciliation process:" it watches some object for the world's desired state, and it watches the world's actual state, too. Then, it sends instructions to try and make the world's current state be more like the desired state. Suffice it to say that you must act as a client to the API.

Here are some simple examples.

## Working with Fabric8 and Spring Native 

[Fabric8](https://fabric8.io/) is the RedHat-sponsored Java client for Kubernetes. As far as I know, it is older than the official Java client. It works well,  and some Spring modules, such as Spring Cloud Data Flow for Kubernetes and  Spring Cloud Deployer, use the Fabric8 client. And many others besides. It's awesome. Thank you, Red Hat. There are a lot of really cool samples out there that demonstrate how to get something interesting working. I found [this example](https://github.com/rohanKanojia/sample-controller-java) by Rohan Kanojia that shows building a simple Operator to manage custom resources (of type `Foo`) that the example defines in a custom resource definition (CRD). Each time the user deploys a new `Foo`, the operator creates a new `Foo` instance. Trivial, but it does work. I adapted it to use Spring Boot and its lifecycle management. Then I integrated it with the [Fabric8 Spring Native](https://twitter.com/) integration that I wrote to make compilation into a native image easier.

If you want to see it in action, follow these steps:

* `git clone` the `fabric8-sample-controller` repository.
* Connect to a Kubernetes cluster of some sort. Ensure that when you issue a `kubectl` command, like `kubectl get cards, it produces results. Anything. As long as it's more than just `NAME` and `CREATED AT`.
* Apply the `crd.yaml` file in the `k8s` folder: `kubectl apply -f k8s/crd.YAML
* Start the operator on the JRE: `mvn spring-boot:run
* Then apply the sample YAML for the custom `Foo` resource: `kubectl apply -f k8s/example-foo.yml`
* Run `kubectl get deployments` to confirm that there's a new deployment whose name lines up with the name of the `foo' specified in `example-foo.yaml`.
* Open `example-foo.yaml` in your text editor and update the `replicas` value from `1` to `3`.
* Apply `kubectl apply -f k8s/example-foo.yaml`
* Run `kubectl get deployments` to confirm that the `deployment` also has had its `replicas` count changed to `3`.

Not bad, eh?

Now for my favorite part. Let's compile the application using GraalVM and Spring Native. You'll need to have GraalVM installed and run `gu install native-image`. Then, run `./build.sh`. This process takes a good amount of time - perhaps a minute or two. So pour some coffee if you like. Or take a few sips of the coffee you already have. Or maybe take a breather? I have glasses, and My optometrist told me that it's a good idea to exercise your eyes periodically. First, you tare at some point far away, like a distant corner, for 30 seconds. Then you start at some near point, like the wall, for another 30 seconds. Then relax. Maybe close your eyes? Wherever it is that you cast your gaze, _don't_ stare at your monitor. You want some natural and neutral light source. Something that doesn't ask anything more of your eyes than they've already done. You can't know how long yore going to have your peepers. You have to take care of those things! The world would be a lot darker if you couldn't see. Life's already hard enough, even if your eyes work. Sometimes, even the fully sighted can't see. Isn't it ironic?

Anyway, the build may have finished! If so, run the application: `./target/fabric8-sample-controller`, and stand back! Hello, speed! Goodbye, Go! What a time to be alive. I've just built this on my local machine, and I want to get it to production. That's going to require a container. One of the many nice things about Spring Boot these days is that there's built-in support for Docker containerization using Buildpacks. If you're using Sring Native, as this sample certainly does, then you can get a GraalVM-compiled Linux binary in your Docker image with the following incantation: `mvn spring-boot:build-image`. Stand back, as that'll take ever so slightly longer than the first compile, but the result will be a docker image you can run and `docker tag` and `docker push` to your container registry of choice (VMWare Harbor, anyone?).

You can deploy that to your Kubernetes cluster, but you'll probably want to set up service accounts and all that for security. The original `README.md` in Rohan's project does a good job [detailing the service accounts](https://github.com/rohanKanojia/sample-controller-java/blob/master/README.md) and all, so I'll refer you to that.



<!--
* Working with Kubernetes java client

-->





