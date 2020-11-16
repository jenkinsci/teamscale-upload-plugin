package eu.cqse.teamscale.jenkins.upload;

import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for for the Teamscale Jenkins plugin.
 */
public class TeamscaleUploadUtilities {

    /**
     * Find files in a directory with ant pattern.
     * @param dirToScan top-level directory to start scanning.
     * @param pattern ant pattern to look for files.
     * @return found files with {@code pattern} or {@code empty}
     * @throws IOException scanning directory.
     */
    public static List<File> getFiles(File dirToScan, String pattern) throws IOException {
        DirectoryScanner directoryScanner = new DirectoryScanner();
        String[] includes = {  pattern };
        directoryScanner.setIncludes(includes);
        directoryScanner.setBasedir(dirToScan);
        directoryScanner.scan();

        String[] matches = directoryScanner.getIncludedFiles();
        List<File> files = new ArrayList(matches.length);
        for (String match : matches) {
            files.add(new File(dirToScan + File.separator + match));
        }
        return files;
    }
}
