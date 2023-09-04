package dev.tekton.plugins.jenkins.tkn;

import hudson.tools.InstallSourceProperty;
import hudson.tools.ToolInstaller;
import hudson.tools.ZipExtractionInstaller;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import jenkins.model.Jenkins;

public class TknToolInstallations {

    public static TknClientInstallation configureDefaultTkn(Jenkins jenkins, String version) throws IOException {
        Path tkn = Paths.get(System.getProperty("user.dir"), "work", "tools", "tkn", version);
        if (!tkn.toFile().exists()) {
            tkn.toFile().mkdirs();
        }
        // Create property to auto-install from GitHub tar or zip
        ToolInstaller installer = new ZipExtractionInstaller(null, getTknUrl(version), null);
        InstallSourceProperty installProperty = new InstallSourceProperty(Arrays.asList(installer));
        TknClientInstallation install =
                new TknClientInstallation(version, tkn.toAbsolutePath().toString(), Arrays.asList(installProperty));
        jenkins.getDescriptorByType(TknClientInstallation.DescriptorImpl.class).setInstallations(install);
        return install;
    }

    public static String getTknUrl(String version) {
        if (version.startsWith("v")) {
            version = version.substring(1);
        }
        // TODO: Make this a describable, universal installer.
        String baseUrl = "https://github.com/tektoncd/cli/releases/download";
        String os = "Linux";
        String arch = "x86_64";
        String extension = "tar.gz";

        // https://github.com/tektoncd/cli/releases/download/v0.31.2/tkn_0.31.2_Linux_x86_64.tar.gz
        return String.format("%1$s/v%2$s/tkn_%2$s_%3$s_%4$s.%5$s", baseUrl, version, os, arch, extension);
    }
}
