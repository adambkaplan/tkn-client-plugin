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
import javax.annotation.CheckForNull;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class TknBuilder extends Builder implements SimpleBuildStep {

    /**
     * Identifies the {@link TknClientInstallation} to use.
     */
    @CheckForNull
    private String toolVersion;

    @CheckForNull
    private String commands;

    @DataBoundConstructor
    public TknBuilder() {
        this.toolVersion = "";
        // If no commands provided, run `tkn version` by default
        this.commands = "version";
    }

    @CheckForNull
    public String getToolVersion() {
        return toolVersion;
    }

    @DataBoundSetter
    public void setToolVersion(@CheckForNull String name) {
        this.toolVersion = name;
    }

    @CheckForNull
    public String getCommands() {
        return commands;
    }

    @DataBoundSetter
    public void setCommands(@CheckForNull String commands) {
        this.commands = commands;
    }

    public TknClientInstallation getTkn() {
        DescriptorImpl descriptor = (DescriptorImpl) getDescriptor();
        for (TknClientInstallation i : descriptor.getInstallations()) {
            if (i.getName().equals(this.toolVersion)) {
                return i;
            }
        }
        return null;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        TknClientInstallation tkn = getTkn();
        if (tkn == null) {
            // TODO: Add support for Windows
            args.add("tkn");
        } else {
            Computer computer = workspace.toComputer();
            if (computer == null) {
                throw new AbortException("No computer detected - offline?");
            }
            Node node = computer.getNode();
            if (node == null) {
                throw new AbortException("No node detected - offline?");
            }
            tkn = tkn.forNode(node, listener);
            tkn = tkn.forEnvironment(env);
            String exe = tkn.getTkn();
            if (exe == null) {
                throw new AbortException("tkn not found");
            }
            args.add(exe);
        }
        // Proof of Concept - run tkn with arbitrary commands, space-delimited
        String runCommands = this.commands;
        // TODO: DRY out the default "version" command
        if (runCommands == null) {
            runCommands = "version";
        }

        // Warn if KUBECONFIG env var is not set
        if (!env.containsKey("KUBECONFIG")) {
            listener.getLogger().println("WARN: no KUBECONFIG provided, using system default kubeconfig credentials.");
        }

        args.add(runCommands.trim().split("\\s"));
        listener.getLogger().println("Running tkn command");
        try {
            // Run command with stdout and stderr (combined output) printed to the logger
            // TODO: Handle non-zero returns
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

        listener.getLogger().println("Completed running tkn command");
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
