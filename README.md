# Teamscale plugin for Jenkins

Plugin for Jenkins that provides an after build step task for 
uploading external code coverage to teamscale. 

## Getting started

Clone [https://github.com/cqse/teamscale-jenkins-plugin.git](https://github.com/cqse/teamscale-jenkins-plugin.git).


## Building


To build the project 
 ```
 mvn hpi:run
```

To package as ```.hpi and .jar``` for manual installation in jenkins
  ```
  mvn package
```


## Testing

To run all tests either via maven 

```bash
mvn test
```

or the unit tests in your IDE.

## Publishing

 `To be done` 

```bash
To be done
```

##Configuring Jenkins

1. Create a freestyle project in Jenkins
2. Add the Teamscale Upload plugin as post-build action
3. ![Configure the plugin] (/images/teamscale_upload_plugin_configuration.png)