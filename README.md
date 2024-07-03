# Teamscale plugin for Jenkins

## Introduction

Plugin for Jenkins that provides an after build step task for uploading external code coverage to teamscale.

## Getting started

See [documentation](https://docs.teamscale.com/reference/jenkins-plugin).

## Contributing

Remember to [configure Maven](https://www.jenkins.io/doc/developer/tutorial/prepare/#configure-apache-maven).

Refer to our [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

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
- Ensure you have the credentials for `maven.jenkins-ci.org` added to your `~/.m2/settings.xml` as described [here](https://maven.apache.org/settings.html).
- Run the following command
```bash
mvn release:prepare release:perform
```
- During the execution of the command enter the desired version numbers (performing and committing the changes happens automatically)
- Push the newly created tag to origin once the command succeeds

## LICENSE

Licensed under Apache License, see [LICENSE](LICENSE)

