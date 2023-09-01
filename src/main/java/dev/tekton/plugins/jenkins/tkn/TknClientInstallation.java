package dev.tekton.plugins.jenkins.tkn;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import java.io.IOException;
import java.util.List;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

public class TknClientInstallation extends ToolInstallation
        implements EnvironmentSpecific<TknClientInstallation>, NodeSpecific<TknClientInstallation> {

    @DataBoundConstructor
    public TknClientInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    @Override
    public TknClientInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new TknClientInstallation(getName(), translateFor(node, log), getProperties());
    }

    @Override
    public TknClientInstallation forEnvironment(EnvVars environment) {
        return new TknClientInstallation(getName(), environment.expand(getHome()), getProperties());
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        if (getHome() != null) {
            env.put("PATH+TKN", getHome());
        }
    }

    @Extension
    @Symbol("tkn")
    public static class DescriptorImpl extends ToolDescriptor<TknClientInstallation> {

        @Override
        public String getDisplayName() {
            return "tkn";
        }
    }
}
