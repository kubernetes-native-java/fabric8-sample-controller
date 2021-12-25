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
			var context = new CustomResourceDefinitionContext.Builder()
				.withVersion("v1alpha1")
				.withScope("Namespaced")
				.withGroup("samplecontroller.k8s.io")
				.withPlural("foos")
				.build();
			var informerFactory = client.informers();
			var fooClient = client.customResources(Foo.class, FooList.class);
			var deploymentSharedIndexInformer = informerFactory.<Deployment>sharedIndexInformerFor(Deployment.class, 10 * 60 * 1000);
			var fooSharedIndexInformer = informerFactory.<Foo, FooList>sharedIndexInformerForCustomResource(context, Foo.class, FooList.class, 10 * 60 * 1000);
			var sampleController = new SampleController(client, fooClient, deploymentSharedIndexInformer, fooSharedIndexInformer, namespace);
			sampleController.create();
			informerFactory.startAllRegisteredInformers();
			informerFactory.addSharedInformerEventListener(exception -> log.error("Exception occurred, but caught", exception));
			sampleController.run();
		}
	}
}
