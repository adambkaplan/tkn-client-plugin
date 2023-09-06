package dev.tekton.plugins.jenkins.tkn;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import java.io.IOException;
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
    public void testScriptedPipeline() throws Exception {
        // String agentLabel = "my-agent";
        // jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript = "node {tkn toolVersion: '" + tknVersion + "', commands: 'version'}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        // tkn version should succeed
        // TODO: Test happy/sad paths for tkn existence
        WorkflowRun run = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("tkn version", run);
    }
}
