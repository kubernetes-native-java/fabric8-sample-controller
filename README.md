#  Spring Native, GraalVM and Kubernetes Native Java


Hi, Spring fans! You know I love _the cloud_ (cue Daft Punk's _The Grid_ theme from the Tron Legacy movie). I think it's an interesting way to build software. And, as you may have noticed from literally everything I write, tweet, produce, blog, etc., I am also gaga for GraalVM and Spring Native. There's such an exciting opportunity here!

Spring provides an integration point for so much of the enterprise Java ecosystem. This means that so much of what we do is to provide the clean glue code to integrate and adapt all the cool and best-of-breed bits into one cohesive whole. It's a dirty job, but somebody's gotta do it! So it was when Spring Framework arrived on the scene, more than 20 years ago! Spring integrated a lot of componetns from around the ecosystem and provided a component model that third-party libraries could use to provide integrations of their own. So it was when we introduced Spring Boot. Spring Boot shipped with autoconfigurations that meant that the mere presence of types on the classpath could trigger some default  and useful behavior in your application. And of course the ecosystem rushed in to provide autoconfigurations and starters of their own, too. And so it is with Spring Native, which provides integrations for as many projects as possible so that they work flawlessly when used in a GraalVM native image context.

Except that Spring Native isn't a runtime framework in the way that Spring Framework and Spring Boot are. No, Spring Native is a differnet beast altogether. Its a way to feed configuration ito the graalvm native image compiler tool. Yes, GraalVm os a drop in replacement for OPenJDK that offers a superior alternative to the Just-in-time compiler HotSpot. But that's not all. It offers an intriguing little utility called `native-image`, and that utility is an ahead-of-time (AOT) compiler. Henceforth, Ill refer to GraalVM and the `native-image`  AOT  compiler utility interchangeably. Most people do anyway.

The JITs magic is hoepfully well understood at this point: the Java runtime looks at frequently run codepaths in your application and turns them into native code. This transpiltion only kicks in after a certain threshold number of runs. The effect is that a medicare Java application can sometimes run faster than a well-written and analagous C or C++ application, whose memory shapes are rife with the memory spikes associated with local resouce initialization, acquisition and - very shortly thereafeter - destruction. Large organizations with large applications sometimes exploit this dyanmic, _warming_ their applications up by baragging them with traffic to incite the JIT beforee loosing the applications in production.

If JITs such a good idea, why not go the full nine yards and transpile everything into native code, soup to nuts? Why bother with the adaptive transpilation? This is the core conceit of the GraalVM `native-image` AOT engine. It takes your `.class` files and turns them into native code.

<!--  TODO in the longer form version of this show how to setup a basicp roject using Spring Native, the Spring INitializr and GraalVM  -->

Spring Native (and GraalVM) are like almost like magic. _Almost_. Spring Native is not a silver bullet. GraalVM does static analysis at compile time, and throws away everything in your application tha cant be 'reached'. reachable means types that are used by other types in your compilation uinit. whether theyre in your code or in the code on the classpath or in the JRE itself. Static analaysis at compile time can only get you so far, but will never be 100% aware of what functionality exists at runtime. This is an NP-complete problem, after all! There are a whole host of things that graalvm doesnt support out-of-the-box, unless you feed the graalvm native image compiler the appropriate configuration. the compiler will bake a sort of shim into the heap of the native image, so that your code appears to execute correctly. These things include but are not limited to things like reflection, serialization, proxies (both cglib/aot and jdk-based proxies), JNI, and more.

Remember, Java, for all of its warts and stodginess, is actually a very dyanmic language that has more in common with the lieks of python, ruby, javascript, perl, lua, tcl, php, and smalltalk than it does languages like c++ and c. its a very dynamic language. its possible in a java appication to dynamically compile a class definition (from a `java.lang.String`) into  a `.class` file from, load that class in to the `ClassLoader`, reflectively new-up an instance of that class, invoke methods on that class, crate JDK proxies (a `Proxy` with an `Invocationhandler`) of that type, serialize instances of that class, and all without ever compiling against a concrete type. You can do all of that in terms of `java.lang.Object` and reflection, not once referencing a concrete type in your code. Java's beyond amazing! But GraalVM, well, GraalVM _hates_ all these fun things you can do. it doesn't know about them and unless you feed it configuration telling it otherwise, it doesn't care about those things, either.

So, you have two, not mutually exclusive, paths forward. You can minimize those things that GraaLVM hates and you can provide configuration for the stuff yo ucouldn't eliminate and still need to work. Spring Native helps with both.

Spring Native ships with build time plugins (for Maven and Gradle) and infrastructure called Spring AOT. The Spring AOT infastructure transforms your code base in meaningful ways to eliminate or reduce things that GraalVM doesnt like. Things like reflection, proxies, and more.

<!--  
in the longer form version, show how when you run a new spring native build, it converts spring.facotries into a class, and show how it transpiles the `@Configuration` annotated code into functional configuration (also: make sure that functional configuration is shown in the introduction chapter ) 
-->

Spring Native also ships with a system to contribute configuration to the graalvm compiler that it calls "hints." these are kind of like the autoconfigurations of Spring Boot. Theyre code that are aware of the Spring commponent model, and your Spring beans. This mechanism is an extension plane, as well. You too can build your own organizational extension planes, too!  They're also very different than autoconfigurations, in that they  are involved entirely in the compilation process at compile time, and have no impact on the runtime of the application. They exist to analyze the components in you Spring Boot-based component model and contribute hints accordingly. Is Spring Data on the classpatha nd has the user created any repositories? Well, that'll surely require some configuration for the proxies created from your repository interface and for the reflection done to analyze the method signatures of that interface. Is Hibernate on the classpath? That'll require configuration, as well. What about any aspect-oriented code? Yep. That'll require special support. What about Spring Batch's `Step` scope and the proxies it creates? Yep. It'll require coniguration. And the list goes on and on. Spring Native covers the common cases, but it can't know about every type ever written, so you'll need to provide your own integrations as well.

<!-- TODO in a longer version, this is where I would  -->


graalvm native applications are small and they start up very quckly. they take very little in the way of memory and they can be run inside containers with a minimal operating system. to the extent that its possible, spring native makes spring boot and spring cloud applications even more cloud native! I wrote a book abotut hins tsort of thing (CNJ). trust me, im a professional. docker containers love these sorts of aplications too! genreally, spring boot and spring cloud are te most powerful way to desgin and describe distriuted systems intended for the cloud, and now they can be deployed in one of the most memory footprint efficient means possible. what a time to be alive!

<!-- Here Im going to cut the big piece short wiht a discussion of Kubernetes native clients -->

one of the mos excvitign opportunities these days is usign spring native to write memory-efficient, small-footprint controllers for kubernetes. I love the idea. And it'd make sense, right? The Kubernetes clienits are autogenerated based on the schema of the API in the API server, so who cares in what language a person writes their code? So long as the client can talk to the Kubernetes API server, then you're all set. Until, that is, you have to deploy the client to production. In priduction, youll need to containerize the application and get it running. Spring Boot's got built in support for buildpacks, which you can use to containerize your applications with ease: `mvn spring-boot:build-image` and then `docker tag` and `docker push` your image to production and you're off to the races! Almost.   If you're using a full JRE, then this will require a more fully-featured operating system like Ubuntu. The JRE and the operating system add substantially to the size of the OCI/Docker image that contains your application. You can eliminate a lot of that extra heft by using Spring native to create native images with your GraalVm applications, but GraalVM is an all-or-nothing propposition: either all of your code works on a GraalVm contexrt or none of it does.

In the java ecosystem, there are two very good Java clients you can use to create controllers, and both do all sorts of stuff you won't appreciate ina a GraalVM context. I wanted to make this easier so ive built two Spring Native integrations, one for Fabric8 and anotehr for the official Java Kuvernetes client.

NB: ive gotten some fairly interesting examples workign with these clients, but its not to sayt hat theyre perfect. if u dfiscover some use case that hasnt been propoerly supported when buildign your native application using these integrations, _please, [let me know_ (@starbuxman) ](https://twitter.com/starbuxman)!


Let's take a look at them.

One of the mot interesting opportunities with these clients is that you can build custom resources ad operators to manage those custom resources. This is the real power of Kubernetes, if you think about it: it's an API server! It cares about objects, and Spring is great at managing objects and their lifecycles.

Specifically, a Kubernetes controller is what [the documentation](https://github.com/kubernetes/community/blob/8cafef897a22026d42f5e5bb3f104febe7e29830/contributors/devel/controllers.md) calls an "active reconciliation process:"  it watches some object for the world's desired state, and it watches the world's actual state, too. Then, it sends instructions to try and make the world's current state be more like the desired state. Suffice it to say its very important that you be able to act as a client to the API.

Here are some simple examples.

## Working with Fabric8

Fabric8 is the RedHat sponsored Java client for Kubernetes. It is, as far as I know, older than the official Java client. It works raelly well, it's even used in some Spring modules, such as in the Spring CLoud Data Flow for Kubernetes integration and in the Spring Cloud Deployer integration for Kubernetes. And perhaps many others beisdes. It's awesome. Thank you RedHat. Ther are a lot of really cool samples out there that demonstrate how to get somethng interesting working. I found [this example](https://github.com/rohanKanojia/sample-controller-java) by Rohan Kanojia that demonstrates building a simple Operator to manage  custom resources (of type `Foo`) that the example defines in a custom resource definition (CRD). Each time the user deploys a new `Foo`, a new `Deployment` of `nginx` is created. Trivial, but it does work. I adapted it to use Spring Boot and its lifecycle management and then I integrated it with the [Fabric8 Spring Native](https://twitter.com/) integration that I wrote to make transpilation into a native image easier.

If you want to see it in action, follow these steps:

* `git clone` the `fabric8-sample-controller` repository.
* Connect to a Kubernetes cluster of some sort. Make sure that when you issue a `kubectl` command, like `kubectl get crds`, it produces results. Anything. As long as it's more than just `NAME` and `CREATED AT`.
* Apply the `crd.yaml` file in the `k8s` folder: `kubectl apply -f k8s/crd.yaml`
* Start the operator on the JRE: `mvn spring-boot:run`
* Then apply the sample YAML for the custom `Foo` resource: `kubectl apply -f k8s/example-foo.yml`
* Run `kubectl get deployments` to confirm that there's a new deployment whose name lines up with the name of the `foo` specified in `example-foo.yaml`.
* Open `example-foo.yaml` in your text editor and update the `replicas` value from `1` to `3`.
* Apply `kubectl apply -f k8s/example-foo.yaml`
* Run `kubectl get deployments` to confirm that the `deployment` also has had its `replicas` count changed to `3`.

Not bad, eh?

Now for my favorite part. Let's compile the application using GraalVM and Spring Native. YOu'll need to have GraalVM installed and then have run `gu install native-image`. Then, run `./build.sh`. This process takes a goodly amount of time - a minute or two perhaps. So pour some coffee if ya like. Or take a few sips of the coffee you already have. Or maybe just take a breather? You know, I have glasses and I was told that it's a good idea to periodically exercise your eyes. First, you tare at some point far away, like a distant corner, for 30 seconds. then you start at some near point, like the wall, for another 30 seconds. then just relax. Maybe close your eyes? Either way, don't stare at your monitor. You want some sort of neautral light source. Something that doesn't ask anythign more of your eyes than they've already done. You can't know how long yore goign to have your peepers. YOu gotta take care of those things! The world would be a lot darker if you could't see. Life's already hard enough, even if your eyes work. Sometimes, even the fully sighted can't see. Isn't it ironic?

Anyway, the build might be done now. If so, run the application: `./target/fabric8-sample-controller`, and stand back! Hello speed! Good bye Go! What a time to be alive. Obviously I've just built this on my local machine and I want to get it to production. Thats going tor requite a container. One of the many nice things about Spring Boot htese days is that there's built-in support for Docker containerzation using Buildpacks. If youre using Sring Native, as this sample cetainly does, then you can get a GraalVM-compiled Linux binary in your Docker image with the following incantation: `mvn spring-boot:build-image`. Stand back, as that'll take ever so slightly longer than the first compile, but the resut will be a docker image you can run and `docker tag` and `docker push` to yoru container registry of choice (VMWare Harbor, anyone?).

You can deploy that to your Kubernetes cluster, but youll probably want to setup service accounts and all that for security. The original `README.md` in Rohan's project does a pretty good job [detailing the service accounts](https://github.com/rohanKanojia/sample-controller-java/blob/master/README.md) and all, so I'll refer you to that.




* working with kibernetes java client




