package dev.tekton.plugins.jenkins.tkn;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class TknBuilder extends Builder implements SimpleBuildStep {

    /**
     * Identifies the {@link TknClientInstallation} to use.
     */
    private final String tknName;

    @DataBoundConstructor
    public TknBuilder(String tknName) {
        this.tknName = tknName;
    }

    public String getTknName() {
        return tknName;
    }

    public TknClientInstallation getTkn() {
        if (tknName == null) {
            return null;
        }
        DescriptorImpl descriptor = (DescriptorImpl) getDescriptor();
        for (TknClientInstallation i : descriptor.getInstallations()) {
            if (i.getName().equals(this.tknName)) {
                return i;
            }
        }
        return null;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        run.addAction(new TknAction(tknName));
        ArgumentListBuilder args = new ArgumentListBuilder();
        TknClientInstallation tkn = getTkn();
        if (tkn == null) {
            // TODO: Add support for Windows
            args.add("tkn");
        } else {
            Node node = Computer.currentComputer().getNode();
            if (node == null) {
                throw new AbortException("offline?");
            }
            tkn = tkn.forNode(node, listener);
            tkn = tkn.forEnvironment(env);
            String exe = tkn.getTkn();
            if (exe == null) {
                throw new AbortException("tkn not found");
            }
            args.add(exe);
        }
        // Proof of Concept - run tkn version
        args.add("version");
        listener.getLogger().println("Running tkn version");
        try {
            // Run command with stdout and stderr (combined output) printed to the logger
            launcher.launch()
                    .cmds(args)
                    .envs(env)
                    .stdout(listener.getLogger())
                    .stderr(listener.getLogger())
                    .pwd(workspace)
                    .join();
        } catch (IOException e) {
            // TODO: Can we throw a better exception type here?
            throw new AbortException("action failed: " + e.getMessage());
        } finally {
            // Flush the logger to ensure all output is printed.
            listener.getLogger().flush();
        }

        listener.getLogger().println("Completed running tkn version");
    }

    @Extension
    @Symbol("tkn")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public DescriptorImpl() {
            load();
        }

        public TknClientInstallation[] getInstallations() {
            ToolDescriptor<TknClientInstallation> descriptors = (ToolDescriptor<TknClientInstallation>)
                    ToolInstallation.all().find(TknClientInstallation.class);
            return descriptors.getInstallations();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.TknBuilder_DescriptorImpl_DisplayName();
        }
    }
}
