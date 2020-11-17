# Teamscale plugin for Jenkins

Plugin for Jenkins that provides an after build step task for 
uploading external code coverage to teamscale. 

## Getting started
Install the plugin from the official marketplace on your jenkins server.

### Freestyle Project 
1. Create a freestyle project in Jenkins
![](https://github.com/jenkinsci/teamscale-upload-plugin/blob/master/doc/create_freestyle_project.gif)
2. Also specifically checkout the branch which is cloned from jenkins, so the plugin works correctly.
![](https://github.com/jenkinsci/teamscale-upload-plugin/blob/master/doc/checkout_local_branch.gif)
3. Add the Teamscale Upload plugin as post-build action
4. Configure the plugin

<p align="center">
  <img src="https://github.com/jenkinsci/teamscale-upload-plugin/blob/master/doc/freestyle_rpoject_config.png">
</p>
  
- **URL:** http://www.yoururl.com:port or http://ip-adress:port
- **Credentials:** Select jenkins global credentials consisting of _Teamscale Username_ and _IDE Access Key_ (from _http://your-teamscale-url/user.html#access-key_).
 In jenkins use username and password for storing these information. 
- **Project:** in Teamscale to upload data to.
- **Partition:** The partition of the project to push code coverage to.
- **Upload Message:** Desired message for the data which will be uploaded.
- **File Format:** Ant-pattern for file selection in the jenkins working directory (e.g. here: selecting all *.simple files in working directory)
- **Report Format ID:** Use the PARAMETER VALUE in [supported formats](https://docs.teamscale.com/reference/upload-formats-and-samples/#supported-formats-for-upload). 
- **Revision:** Please leave this one empty! Only needed for the pipeline build steps. 

### Declarative Pipeline

<details>
<summary>Git sample</summary>

```groovy
pipeline {
    agent any
    
    stages {
         stage('Stage 1') { 
            steps {
                git 'https://github.com/Test/test.git' // OR 
                // checkout([$class: 'GitSCM', 
                //  branches: [[name: '*/master']], 
                //  doGenerateSubmoduleConfigurations: false, 
                //  extensions: [[$class: 'LocalBranch', localBranch: 'master']], 
                //  submoduleCfg: [], 
                //  userRemoteConfigs: [[url: 'https://github.com/Test/test']]])  
                //  OR checkout([$class: 'SubversionSCM', remote: 'http://sv-server/repository/trunk']]]) --> Change handover revision of env var to  ${SVN_REVISION}
                teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${GIT_COMMIT}"
            }
        }

         stage('Stage 2') {
            steps {
               teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${GIT_COMMIT}"
               teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${GIT_COMMIT}"
          
            }
        }
    }
    post { 
            always {
              teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${GIT_COMMIT}"
              teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${GIT_COMMIT}"
          
            }
    }
}
```

</details>

<details>
<summary>SVN sample</summary>

```groovy
pipeline {
    agent any
    
    stages {
         stage('Stage 1') { 
            steps {
                checkout([$class: 'SubversionSCM', remote: 'http://sv-server/repository/trunk']]])  
                teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${SVN_REVISION}"
            }
        }

         stage('Stage 2') {
            steps {
               teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${SVN_REVISION}"
               teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${SVN_REVISION}"
          
            }
        }
    }
    post { 
            always {
              teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${SVN_REVISION}"
              teamscale antPatternForFileScan: '**/*.simple', credentialsId: 'teamscale_id', partition: 'pipeline', reportFormatId: 'SIMPLE', teamscaleProject: 'jenkinsplugin', uploadMessage: 'Test', url: 'http://localhost:8100', revision: "${SVN_REVISION}"
          
            }
    }
}
```

</details>

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

