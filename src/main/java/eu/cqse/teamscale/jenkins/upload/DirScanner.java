package eu.cqse.teamscale.jenkins.upload;

import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirScanner {
    private String pattern;

    public DirScanner( String pattern ) {
        this.pattern = pattern;
    }

    public List<File> list(File dirToScan ) throws IOException {
        DirectoryScanner ds = new DirectoryScanner();
        String[] includes = {  this.pattern };
        //String[] excludes = {"modules\\*\\**"};
        ds.setIncludes(includes);
        //ds.setExcludes(excludes);
        ds.setBasedir(dirToScan);
        //ds.setCaseSensitive(true);
        ds.scan();

        String[] matches = ds.getIncludedFiles();
        List<File> files = new ArrayList(matches.length);
        for (String match : matches) {
            files.add(new File(match));
        }
        return files;
    }
}
