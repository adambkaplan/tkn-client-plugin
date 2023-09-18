package dev.tekton.plugins.jenkins.tkn;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class TknBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String tknVersion = "v0.31.1";

    @Before
    public void setUp() {
        try {
            TknToolInstallations.configureDefaultTkn(jenkins.getInstance(), tknVersion);
        } catch (IOException e) {
            Assert.fail("Failed to configure tkn installer: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        TknBuilder builder = new TknBuilder();
        builder.setToolVersion(tknVersion);
        builder.setCommands("version");
        project.getBuildersList().add(builder);
        project = jenkins.configRoundtrip(project);

        TknBuilder expected = new TknBuilder();
        expected.setToolVersion(tknVersion);
        expected.setCommands("version");
        jenkins.assertEqualDataBoundBeans(expected, project.getBuildersList().get(0));
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        TknBuilder builder = new TknBuilder();
        builder.setToolVersion(tknVersion);
        builder.setCommands("version");
        project.getBuildersList().add(builder);
        // tkn version should succeed
        // TODO: Test happy/sad paths for tkn existence
        FreeStyleBuild build = jenkins.assertBuildStatusSuccess(project.scheduleBuild2(0));
        jenkins.assertLogContains("tkn version", build);
    }

    @Test
    public void testScriptedPipelineNoKubeconfig() throws Exception {
        // String agentLabel = "my-agent";
        // jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript = loadGroovyScript("pipeline-noauth.groovy");
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        // tkn version should succeed
        // TODO: Test happy/sad paths for tkn existence
        WorkflowRun run = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("tkn version", run);
        jenkins.assertLogContains("no KUBECONFIG provided", run);
    }

    @Test
    public void testScriptedPipelineWithKubeconfig() throws Exception {
        // String agentLabel = "my-agent";
        // jenkins.createOnlineSlave(Label.get(agentLabel));
        CredentialsProvider.lookupStores(jenkins.jenkins)
                .iterator()
                .next()
                .addCredentials(Domain.global(), dummyCredentials("test-kubeconfig"));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-authed-pipeline");

        String pipelineScript = loadGroovyScript("pipeline-auth.groovy");
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        // tkn version should succeed
        // TODO: Test happy/sad paths for tkn existence
        WorkflowRun run = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("tkn version", run);
        jenkins.assertLogNotContains("no KUBECONFIG provided", run);
    }

    public Credentials dummyCredentials(String credId) {
        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credId, "dummy", "test-user", "test-pass");
    }

    public String loadGroovyScript(String script) throws IOException, URISyntaxException {
        return Files.readString(Paths.get(getClass().getResource(script).toURI()));
    }
}
