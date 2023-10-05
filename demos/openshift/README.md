# Tkn Client Plugin on OpenShift Demo

## Get an OpenShift Cluster

Log into a modeslty sized OpenShift cluster, with at least 2 vCPU and 4Gi of combined free RAM on
worker nodes. Smaller footprints like
[OpenShift Local](https://developers.redhat.com/products/openshift-local/overview) or
[MicroShift](https://microshift.io/) may not have enough resources to run the demo. You can use a
managed OpenShift such as [Red Hat OpenShift Service on AWS](https://aws.amazon.com/rosa/).

## Deploy OpenShift Pipelines

Use OperatorHub to install OpenShift Pipelines. This can be done in one of two ways:

1. In the OpenShift Admin Console, search for "Red Hat OpenShift Pipelines" in OperatorHub.
   Click on the icon, and follow the prompts to install the operator.

2. Create the operator Subscription via the command line:

   ```sh
   $ oc appy -f demos/openshift/tekton/operator-subscription.yaml
   ```

_Note: these steps must be executed by a cluster administrator_.

## Deploy Jenkins

1. Create a project to deploy Jenkins - example: `jenkins-tkn`

   ```sh
   $ oc new-project jenkins-tkn
   ```

   _Note: this may require the user to have elevated permissions._

2. (optional) Build a container image using `build/Dockerfile` from the project root and push it
   to a container registry. Make sure that your cluster is able to pull images from this registry.

   ```sh
   $ cd ../..
   $ export IMAGE_REF="<YOUR-REGISTRY>/<YOUR-ORG>/<YOUR-IMAGE>:<YOUR-TAG>"
   $ podman build -t $IMAGE_REF <YOUR-REGISTRY>/<YOUR-ORG>/<YOUR-IMAGE>:<YOUR-TAG> \
   -f demos/openshift/build/Dockerfile .
   # ... build logs
   $ podman push $IMAGE_REF
   ```

3. Import `quay.io/adambkaplan/openshift-jenkins:2.401.1-tkn` (or your build above) as an
   ImageStream in the `jenkins-tkn` project.

   ```sh
   # When deploying your own image, replace --from with your image reference
   $ oc import-image jenkins --from=quay.io/adambkaplan/openshift-jenkins:2.401.1-tkn --confirm
   ```

3. Deploy the Jenkins ephemeral template using the customized image:

   ```sh
   $ oc new-app jenkins-ephemeral -p JENKINS_IMAGE_STREAM_TAG="jenkins:latest" -p NAMESPACE="jenkins-tkn"
   ```

4. In the Jenkins web console, go to "Manage Jenkins -> Tools", then add an automatic installer for
   the `v0.32.0` tkn CLI release. Use this
   [link](https://github.com/tektoncd/cli/releases/download/v0.32.0/tkn_0.32.0_Linux_x86_64.tar.gz) for
   the download URL (Linux x86_64).

   ![tkn automatic tool installion form for Jenkins](/assets/tkn-cli-install.png)

## Create the Tekton Pipeline

1. Create an ImageStream for our simple Tekton pipeline output:

   ```sh
   $ oc create is golang-ex
   ```

3. Provision a PVC named `s2i-go` to hold the source code of the build:

   ```sh
   $ oc apply -f demos/openshift/tekton/pvc-s2i-go.yaml
   ```

## Run Tekton as Code from Jenkins as Code

1. In the Jenkins admin console, create a new "Pipeline" project (pick a name, such as `tkn-s2i-go`).

2. Under the "Pipeline" configuration section, select "Pipeline script from SCM":

   1. For the repository, type `https://github.com/adambkaplan/tkn-client-plugin.git`

   2. For "Script Path", type `demos/openshift/jenkins/Jenkinsfile`.

3. Save your changes, then return to the project's Jenkins page.

4. On the lefthand panel, click "Build Now" to run the pipeline. You are now running "Pipelines as
   Code" - squared!
