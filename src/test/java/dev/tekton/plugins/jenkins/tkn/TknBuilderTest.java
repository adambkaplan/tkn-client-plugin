package dev.tekton.plugins.jenkins.tkn;

import static org.junit.Assert.assertNotNull;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class TknBuilderTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    final String tknVersion = "v0.31.1";

    @Test
    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TknBuilder(tknVersion));
        project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(
                new TknBuilder(tknVersion), project.getBuildersList().get(0));
    }

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        TknBuilder builder = new TknBuilder(tknVersion);
        project.getBuildersList().add(builder);
        // Simply check that the build was scheduled
        // Actual builds can fail if tkn is not in the PATH
        // TODO: Test happy/sad paths for tkn existence
        QueueTaskFuture<FreeStyleBuild> r = project.scheduleBuild2(0);
        assertNotNull("build was not scheduled", r);
        jenkins.assertLogContains("tkn version", r.get());
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript = "node {tkn '" + tknVersion + "'}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        // Simply check that the build was scheduled
        // Actual builds can fail if tkn is not in the PATH
        // TODO: Test happy/sad paths for tkn existence
        QueueTaskFuture<WorkflowRun> r = job.scheduleBuild2(0);
        assertNotNull("pipeline run was not scheduled", r);
        jenkins.assertLogContains("tkn version", r.get());
    }
}
