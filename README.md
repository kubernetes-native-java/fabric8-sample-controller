# Sample Controller in Java Using Spring Boot, Spring Native, and the Fabric8 Kubernetes Client

This is a fork of an Apache 2-licensed [sample by Rohan Kanojia](https://github.com/rohanKanojia/sample-controller-java/workflows/Java%20CI%20with%20Maven/badge.svg?branch=master)
. It's awesome, and it works:  _thank you_, Rohan.

This repository implements a simple controller for watching Foo resources as
defined with a CustomResourceDefinition (CRD). This is a simple operator for a
custom resource called `Foo`. Here is what this resource looks like:

```yaml
apiVersion: samplecontroller.k8s.io/v1alpha1
kind: Foo
metadata:
  name: example-foo
spec:
  deploymentName: example-foo-deploy
  replicas: 1
```

Each `Foo` object maintains a child `Deployment` instance in the cluster.

## Build

```shell 
mvn clean install
```

## How to Run

You need to use Kubernetes 1.9 or later for this example to work.

```bash
kubectl create -f k8s/crd.yaml
mvn spring-boot:run
kubectl create -f k8s/example-foo.yaml
kubectl get deployments
```

## Deploy to Kubernetes

You'll need to turn the application into a Docker container and then deploy it to a Docker registry like VMware Harbor, Dockerhub, and Google Container Registry. You can create a Docker image using Spring Boot's built-in support for creating containers:

```shell
IMAGE_NAME=my-repo-org/my-docker-image-name
mvn -f ./pom.xml -DskipTests=true clean spring-boot:build-image -Dspring-boot.build-image.imageName=$IMAGE_NAME 
docker push $IMAGE_NAME
```

Generate and Apply Kubernetes manifests to Kubernetes Cluster. First you'll need to configure `ServiceAccount` instances with the correct roles. Otherwise, you might get a `403` from Kubernetes API server. Since our application will use the `default` `ServiceAccount`, we can configure it like this:

```shell
kubectl create clusterrolebinding default-pod --clusterrole cluster-admin --serviceaccount=default:default
# In case of some other namespace:
kubectl create clusterrolebinding default-pod --clusterrole cluster-admin --serviceaccount=<namespace>:default
```

Then you can go ahead and apply the resources. Create some `Foo` resource and check if the `Deployment` gets created. Enjoy!

