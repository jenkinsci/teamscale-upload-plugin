# Teamscale plugin for Jenkins

Plugin for Jenkins that provides an after build step task for 
uploading external code coverage to teamscale. 

## Getting started
See [documentation](https://docs.teamscale.com/reference/jenkins-plugin)  

## For Developers

Fork or clone [https://github.com/jenkinsci/teamscale-upload-plugin](https://github.com/jenkinsci/teamscale-upload-plugin)  
create a new branch with name specifying what you want modify

### Building


To build the project locally
 ```
 mvn hpi:run
```

To package as ```.hpi and .jar``` for manual installation in jenkins
  ```
  mvn package
```


### Testing

To run all tests either via maven 

```bash
mvn test
```

or the unit tests in your IDE.

### Publishing

File a [PR](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request) on master
Ask a repository maintainer to approve the PR or become a maintainer yourself 

Consider reading [this](https://jenkins.io/doc/developer/plugin-governance/managing-permissions/) to become a maintainer.

As Maintainer:
Consider reading [Releasing  jenkins plugin](https://jenkins.io/doc/developer/publishing/releasing/)
```bash
mvn release:prepare release:perform
```

