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

### Manual testing

Start Teamscale, create a project with a Git connector.

Start Jenkins with `mvn hpi:run -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true -Dport=8081`.

Go to http://localhost:8081/jenkins/, create new Item, choose Freestyle project.
Then set Source Code Management to Git and choose the same repository as in Teamscale (you can also use a `file:///` URL) and add a Teamscale Upload Post-build Action.
Set URL as http://localhost:8080/, add admin:admin user and configure the remaining settings.
Click Build Now, then go to the Job and check the Console Output for the expected output. You should also see the report uploaded in Teamscale.

Optionally, "Setup Security" with "Jenkins' own user database".
Create a lowpriv user.
Set "Authorization" to "Project-based Matrix Authorization Strategy" and configure appropriately.
Go to http://localhost:8081/jenkins/pipeline-syntax/ and check that lowpriv user can use it.
Optionally enable Overall/SystemRead permission according to Jenkins documentation to also test global configuration read access.

### Publishing

File a [PR](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request) on master
Ask a repository maintainer to approve the PR or become a maintainer yourself 

Consider reading [this](https://jenkins.io/doc/developer/plugin-governance/managing-permissions/) to become a maintainer.

As Maintainer:
Consider reading [Releasing  jenkins plugin](https://jenkins.io/doc/developer/publishing/releasing/)
- Ensure you have the credentials for `maven.jenkins-ci.org` added to your `~/.m2/settings.xml` as described [here](https://maven.apache.org/settings.html).
- Run the following command
```bash
mvn release:prepare release:perform
```
- During the execution of the command enter the desired version numbers (performing and committing the changes happens automatically)
- Push the newly created tag to origin once the command succeeds


