# Teamscale plugin for Jenkins

Plugin for Jenkins that provides an after build step task for 
uploading external code coverage to teamscale. 





## Getting started
1. Install the plugin on your jenkins server
2. Create a freestyle project in Jenkins
![](https://github.com/jenkinsci/teamscale-upload-plugin/blob/master/doc/create_freestyle_project.gif)
3. Add the Teamscale Upload plugin as post-build action
4. Configure the plugin

![](https://github.com/jenkinsci/teamscale-upload-plugin/blob/master/doc/teamscale_upload_plugin_configuration.png)


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

```bash
To be done
```

