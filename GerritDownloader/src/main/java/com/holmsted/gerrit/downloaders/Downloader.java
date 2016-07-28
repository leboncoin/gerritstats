package com.holmsted.gerrit.downloaders;

import com.holmsted.file.FileWriter;
import com.holmsted.gerrit.CommandLineParser;
import com.holmsted.gerrit.GerritServer;
import com.holmsted.gerrit.downloaders.ssh.GerritSsh;
import com.holmsted.gerrit.downloaders.ssh.GerritSsh.Version;
import com.holmsted.gerrit.downloaders.ssh.SshDownloader;
import com.holmsted.gerrit.downloaders.ssh.SshProjectLister;

import java.io.File;
import java.util.List;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

public class Downloader {

    @Nonnull
    private final CommandLineParser commandLine;
    @Nonnull
    private final GerritServer gerritServer;

    private Version gerritVersion;

    public Downloader(CommandLineParser commandLine) {
        this.commandLine = commandLine;
        gerritServer = new GerritServer(
                commandLine.getServerName(),
                commandLine.getServerPort(),
                commandLine.getPrivateKey());
    }

    public void download() {
        List<String> projectNames = commandLine.getProjectNames();
        if (projectNames == null || projectNames.isEmpty()) {
            projectNames = createProjectLister().getProjectListing();
        }

        gerritVersion = GerritSsh.version(gerritServer);

        for (String projectName : projectNames) {
            AbstractGerritStatsDownloader downloader = createDownloader();
            downloader.setOverallCommitLimit(commandLine.getCommitLimit());
            downloader.setProjectName(projectName);
            String data = downloader.readData();
            if (data.isEmpty()) {
                System.out.println(String.format("No output was generated for project '%s'", projectName));
            } else {
                String outputDir = checkNotNull(commandLine.getOutputDir());
                String outputFilename = outputDir + File.separator + projectNameToFilename(projectName);
                FileWriter.writeFile(outputFilename, data);
                System.out.println("Wrote output to " + outputFilename);
            }
        }
    }

    private ProjectLister createProjectLister() {
        return new SshProjectLister(gerritServer);
    }

    private AbstractGerritStatsDownloader createDownloader() {
        return new SshDownloader(gerritServer, checkNotNull(gerritVersion));
    }

    private static String projectNameToFilename(String projectName) {
        return sanitizeFilename(projectName) + ".json";
    }

    private static String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}