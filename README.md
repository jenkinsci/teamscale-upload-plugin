# Teamscale plugin for Jenkins

Plugin for Jenkins that provides an after build step task for 
uploading external code coverage to teamscale. 





## Getting started
1. Install the plugin on your jenkins server
2. Create a freestyle project in Jenkins
![](https://github.com/jenkinsci/teamscale-upload-plugin/blob/master/doc/create_freestyle_project.gif)
3. Also specifically checkout the branch which is cloned from jenkins that the plugin works correctly.
![](https://github.com/jenkinsci/teamscale-upload-plugin/blob/master/doc/checkout_local_branch.gif)
4. Add the Teamscale Upload plugin as post-build action
5. Configure the plugin

<p align="center">
  <img src="https://github.com/jenkinsci/teamscale-upload-plugin/blob/master/doc/teamscale_upload_plugin_configuration.png">
</p>
  
* <b>URL</b>: http://www.yoururl.com:port or http://ip-adress:port
* <b>Username</b>: user in Teamscale instance
* <b>IDE Key</b>: generated IDE key for the user
* <b>Project</b>: project in Teamscale
* <b>Partition</b>: The partition of the project to push code coverage to
* <b>Upload Message</b>: Any message for the uploaded data
* <b>File Format</b>: Ant-pattern style to look for files (here: looking in all directories for .simple files)
* <b>Report Format ID</b>: Matching ID in teamscale, ask your teamscale consultant (may be automatic mapped in the future)



  





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

