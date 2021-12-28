package com.example.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
class SampleController {

	private final BlockingQueue<String> workqueue;

	private final SharedIndexInformer<Foo> fooInformer;

	private final SharedIndexInformer<Deployment> deploymentInformer;

	private final Lister<Foo> fooLister;

	private final KubernetesClient kubernetesClient;

	private final MixedOperation<Foo, FooList, Resource<Foo>> fooClient;

	public SampleController(KubernetesClient kubernetesClient, MixedOperation<Foo, FooList, Resource<Foo>> fooClient,
			SharedIndexInformer<Deployment> deploymentInformer, SharedIndexInformer<Foo> fooInformer,
			String namespace) {
		this.kubernetesClient = kubernetesClient;
		this.fooClient = fooClient;
		this.fooLister = new Lister<>(fooInformer.getIndexer(), namespace);
		this.fooInformer = fooInformer;
		this.deploymentInformer = deploymentInformer;
		this.workqueue = new ArrayBlockingQueue<>(1024);
	}

	public void create() {
		// Set up an event handler for when Foo resources change
		fooInformer.addEventHandler(new ResourceEventHandler<Foo>() {
			@Override
			public void onAdd(Foo foo) {
				enqueueFoo(foo);
			}

			@Override
			public void onUpdate(Foo foo, Foo newFoo) {
				enqueueFoo(newFoo);
			}

			@Override
			public void onDelete(Foo foo, boolean b) {
				// Do nothing
			}
		});

		// Set up an event handler for when Deployment resources change. This
		// handler will lookup the owner of the given Deployment, and if it is
		// owned by a Foo resource will enqueue that Foo resource for
		// processing. This way, we don't need to implement custom logic for
		// handling Deployment resources. More info on this pattern:
		// https://github.com/kubernetes/community/blob/8cafef897a22026d42f5e5bb3f104febe7e29830/contributors/devel/controllers.md
		deploymentInformer.addEventHandler(new ResourceEventHandler<Deployment>() {
			@Override
			public void onAdd(Deployment deployment) {
				handleObject(deployment);
			}

			@Override
			public void onUpdate(Deployment oldDeployment, Deployment newDeployment) {
				// Periodic resync will send update events for all known Deployments.
				// Two different versions of the same Deployment will always have
				// different RVs.
				if (oldDeployment.getMetadata().getResourceVersion()
						.equals(newDeployment.getMetadata().getResourceVersion())) {
					return;
				}
				handleObject(newDeployment);
			}

			@Override
			public void onDelete(Deployment deployment, boolean b) {
				handleObject(deployment);
			}
		});
	}

	public void run() {
		log.info("Starting {} controller", Foo.class.getSimpleName());
		log.info("Waiting for informer caches to sync");
		while (!deploymentInformer.hasSynced() || !fooInformer.hasSynced()) {
			// Wait till Informer syncs
			// log.info("waiting for the informers to sync...");
		}

		while (!Thread.currentThread().isInterrupted()) {
			try {
				log.info("trying to fetch item from workqueue...");
				if (workqueue.isEmpty()) {
					log.info("Work Queue is empty");
				}
				String key = workqueue.take();
				Objects.requireNonNull(key, "key can't be null");
				log.info("Got {}", key);
				if (key.isEmpty() || (!key.contains("/"))) {
					log.warn("invalid resource key: {}", key);
				}

				// Get the Foo resource's name from key which is in format namespace/name
				String name = key.split("/")[1];
				log.info("trying to get the Foo with the name " + name);
				try {
					Foo foo = fooLister.get(name);
					log.info("got the Foo with the name " + name);
					if (foo == null) {
						log.error("Foo {} in workqueue no longer exists", name);
						return;
					}
					log.info("going to try to reconcile (" + name + ")");
					reconcile(foo);
				}
				catch (Exception ex) {
					log.error("got an exception", ex);
				}
			}
			catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				log.error("controller interrupted..");
			}
		}
	}

	/**
	 * Compares the actual state with the desired, and attempts to converge the two. It
	 * then updates the Status block of the Foo resource with the current status of the
	 * resource.
	 * @param foo specified resource
	 */
	protected void reconcile(Foo foo) {
		log.info("trying to reconcile " + foo);
		String deploymentName = foo.getSpec().getDeploymentName();
		if (deploymentName == null || deploymentName.isEmpty()) {
			// We choose to absorb the error here as the worker would requeue the
			// resource otherwise. Instead, the next time the resource is updated
			// the resource will be queued again.
			log.warn("No Deployment name specified for Foo {}/{}", foo.getMetadata().getNamespace(),
					foo.getMetadata().getName());
			return;
		}

		log.info("going to call the Kubernetes API and get some deployments");
		log.info("metadata.namespace.name = " + foo.getMetadata().getNamespace());
		log.info("going to get a " + Deployment.class.getName() + " with the name " + deploymentName);
		// Get the deployment with the name specified in Foo.spec
		Deployment deployment = null;

		try {
			deployment = kubernetesClient//
					.apps()//
					.deployments()//
					.inNamespace(foo.getMetadata().getNamespace())//
					.withName(deploymentName)//
					.get();
			log.info("got the deployment? " + (null == deployment ? "no" : "yes"));
		}
		catch (Exception ex) {
			log.error("couldn't get the Deployment ", ex);
		}

		if (deployment == null) {
			createDeployments(foo);
			return;
		}

		// If the Deployment is not controlled by this Foo resource, we should log
		// a warning to the event recorder and return error msg.
		if (!isControlledBy(deployment, foo)) {
			log.warn("Deployment {} is not controlled by Foo {}", deployment.getMetadata().getName(),
					foo.getMetadata().getName());
			return;
		}

		// If this number of the replicas on the Foo resource is specified, and the
		// number does not equal the current desired replicas on the Deployment, we
		// should update the Deployment resource.
		if (foo.getSpec().getReplicas() != deployment.getSpec().getReplicas()) {
			log.info("Foo {} replicas: {}, Deployment {} replicas: {}", foo.getMetadata().getName(),
					foo.getSpec().getReplicas(), deployment.getMetadata().getName(),
					deployment.getSpec().getReplicas());
			deployment.getSpec().setReplicas(foo.getSpec().getReplicas());
			kubernetesClient.apps().deployments().inNamespace(foo.getMetadata().getNamespace())
					.withName(deployment.getMetadata().getNamespace()).replace(deployment);
		}

		// Finally, we update the status block of the Foo resource to reflect the
		// current state of the world
		updateAvailableReplicasInFooStatus(foo, foo.getSpec().getReplicas());
	}

	private void createDeployments(Foo foo) {
		log.info("going to create a Deployment " + foo.toString());
		Deployment deployment = createNewDeployment(foo);
		log.info("created the new Deployment using " + deployment.getFullResourceName());
		kubernetesClient.apps().deployments().inNamespace(foo.getMetadata().getNamespace()).create(deployment);
		log.info("used the KubernetesClient to create a new Deployment");
	}

	private void enqueueFoo(Foo foo) {
		log.info("enqueueFoo({})", foo.getMetadata().getName());
		String key = Cache.metaNamespaceKeyFunc(foo);
		log.info("Going to enqueue key {}", key);
		if (key != null && !key.isEmpty()) {
			log.info("Adding item to workqueue");
			workqueue.add(key);
		}
	}

	private void handleObject(HasMetadata obj) {
		log.info("handleDeploymentObject({})", obj.getMetadata().getName());
		OwnerReference ownerReference = getControllerOf(obj);
		// Objects.requireNonNull(ownerReference);
		if (ownerReference == null || !ownerReference.getKind().equalsIgnoreCase(Foo.class.getSimpleName())) {
			return;
		}
		Foo foo = fooLister.get(ownerReference.getName());
		if (foo == null) {
			log.info("ignoring orphaned object '{}' of foo '{}'", obj.getMetadata().getSelfLink(),
					ownerReference.getName());
			return;
		}
		enqueueFoo(foo);
	}

	private void updateAvailableReplicasInFooStatus(Foo foo, int replicas) {
		FooStatus fooStatus = new FooStatus();
		fooStatus.setAvailableReplicas(replicas);
		// NEVER modify objects from the store. It's a read-only, local cache.
		// You can create a copy manually and modify it
		Foo fooClone = getFooClone(foo);
		fooClone.setStatus(fooStatus);
		// If the CustomResourceSubresources feature gate is not enabled,
		// we must use Update instead of UpdateStatus to update the Status block of the
		// Foo resource.
		// UpdateStatus will not allow changes to the Spec of the resource,
		// which is ideal for ensuring nothing other than resource status has been
		// updated.
		fooClient.inNamespace(foo.getMetadata().getNamespace()).withName(foo.getMetadata().getName())
				.replaceStatus(foo);
	}

	/**
	 * createNewDeployment creates a new Deployment for a Foo resource. It also sets the
	 * appropriate OwnerReferences on the resource so handleObject can discover the Foo
	 * resource that 'owns' it.
	 * @param foo {@link Foo} resource which will be owner of this Deployment
	 * @return Deployment object based on this Foo resource
	 */
	private Deployment createNewDeployment(Foo foo) {
		log.info("creating the new Deployment object using the  " + DeploymentBuilder.class.getName());
		//@formatter:off
		return new DeploymentBuilder()
			.withNewMetadata()
				.withName(foo.getSpec().getDeploymentName())
				.withNamespace(foo.getMetadata().getNamespace())
				.withLabels(getDeploymentLabels(foo))
				.addNewOwnerReference()
					.withController(true)
					.withKind(foo.getKind())
					.withApiVersion(foo.getApiVersion())
					.withName(foo.getMetadata().getName())
					.withUid(foo.getMetadata().getUid())
				.endOwnerReference()
			.endMetadata()
			.withNewSpec()
				.withReplicas(foo.getSpec().getReplicas())
				.withNewSelector()
					.withMatchLabels(getDeploymentLabels(foo))
				.endSelector()
			.withNewTemplate()
				.withNewMetadata()
					.withLabels(getDeploymentLabels(foo))
				.endMetadata()
				.withNewSpec()
					.addNewContainer()
						.withName("nginx")
						.withImage("nginx:latest")
					.endContainer()
				.endSpec()
			.endTemplate()
		.endSpec()
		.build();
		//@formatter:on
	}

	private Map<String, String> getDeploymentLabels(Foo foo) {
		Map<String, String> labels = new HashMap<>();
		labels.put("app", "nginx");
		labels.put("controller", foo.getMetadata().getName());
		return labels;
	}

	private OwnerReference getControllerOf(HasMetadata obj) {
		List<OwnerReference> ownerReferences = obj.getMetadata().getOwnerReferences();
		for (OwnerReference ownerReference : ownerReferences) {
			if (ownerReference.getController().equals(Boolean.TRUE)) {
				return ownerReference;
			}
		}
		return null;
	}

	private boolean isControlledBy(HasMetadata obj, Foo foo) {
		OwnerReference ownerReference = getControllerOf(obj);
		if (ownerReference != null) {
			return ownerReference.getKind().equals(foo.getKind())
					&& ownerReference.getName().equals(foo.getMetadata().getName());
		}
		return false;
	}

	private Foo getFooClone(Foo foo) {
		Foo cloneFoo = new Foo();
		FooSpec cloneFooSpec = new FooSpec();
		cloneFooSpec.setDeploymentName(foo.getSpec().getDeploymentName());
		cloneFooSpec.setReplicas(foo.getSpec().getReplicas());

		cloneFoo.setSpec(cloneFooSpec);
		cloneFoo.setMetadata(foo.getMetadata());

		return cloneFoo;
	}

}
