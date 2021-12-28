package com.example.operator;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

@Slf4j
class Runner implements ApplicationListener<ApplicationReadyEvent> {

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		try (var client = new DefaultKubernetesClient()) {
			var namespace = client.getNamespace();
			var context = new CustomResourceDefinitionContext.Builder().withVersion("v1alpha1") //
					.withScope("Namespaced")//
					.withGroup("samplecontroller.k8s.io") //
					.withPlural("foos")//
					.build();
			var informerFactory = client.informers();
			log.info("got the informers() ");
			var fooClient = client.customResources(Foo.class, FooList.class);
			log.info("got the FooClient<Foo,FooList,Resource<Foo>>");
			var deploymentSharedIndexInformer = informerFactory.<Deployment>sharedIndexInformerFor(Deployment.class,
					10 * 60 * 1000);
			log.info("got the sharedIndexInformerFor(" + Deployment.class + ")");
			var fooSharedIndexInformer = informerFactory.<Foo, FooList>sharedIndexInformerForCustomResource(context,
					Foo.class, FooList.class, 10 * 60 * 1000);
			log.info("got the sharedIndexInformerForCustomResource(" + Foo.class.getName() + ","
					+ FooList.class.getName() + ")");
			var sampleController = new SampleController(client, fooClient, deploymentSharedIndexInformer,
					fooSharedIndexInformer, namespace);
			log.info("built the " + SampleController.class.getName() + ", about to .create() it...");
			sampleController.create();
			log.info("sampleConroller#create()");
			informerFactory.startAllRegisteredInformers();
			log.info("started all the registered informers");
			informerFactory.addSharedInformerEventListener(
					exception -> log.error("Exception occurred, but caught", exception));
			log.info("added a shared informer event listener");
			sampleController.run();
			log.info("called SampleConroller#run()");
		}
	}

}
