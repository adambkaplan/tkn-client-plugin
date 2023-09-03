package dev.tekton.plugins.jenkins.tkn;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class TknAction implements RunAction2 {

    private String name;
    private transient Run<?, ?> run;

    public TknAction(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Run<?, ?> getRun() {
        return run;
    }

    @Override
    public String getDisplayName() {
        return "Greeting";
    }

    @Override
    public String getIconFileName() {
        return "document.png";
    }

    @Override
    public String getUrlName() {
        return "greeting";
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }
}
