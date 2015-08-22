# EnvInject Plugin for Jenkins CI

[![Build Status](https://jenkins.ci.cloudbees.com/buildStatus/icon?job=plugins/envinject-plugin)](https://jenkins.ci.cloudbees.com/job/plugins/job/envinject-plugin/)

This plugin makes it possible to setup a custom environment for your jobs.

Features:
* Removes inherited environment variables by the Jenkins Java process
* Injects environment variables at node (master/slave) startup
* Executes a setup script before or/and after a SCM checkout for a run
* Injects environment variables before or/and after a SCM checkout for a run
* Injects environment variables as a build step for a run
* Securely injects password values for a run
* Exports environment variables at the end of the build in order to display the set of environment variables used for each build

More info: [the plugin's Wiki page][1]

[1]: https://wiki.jenkins-ci.org/display/JENKINS/EnvInject+Plugin
