# EnvInject Plugin for Jenkins

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/envinject.svg)](https://plugins.jenkins.io/envinject)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/envinject-plugin.svg?label=release)](https://github.com/jenkinsci/envinject-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/envinject.svg?color=blue)](https://plugins.jenkins.io/envinject)

This plugin makes it possible to setup a custom environment for your jobs.

Features:
* Removes inherited environment variables by the Jenkins Java process
* Injects environment variables at node (master/slave) startup
* Executes a setup script before or/and after a SCM checkout for a run
* Injects environment variables before or/and after a SCM checkout for a run
* Injects environment variables as a build step for a run
* Securely injects password values for a run
* Exports environment variables at the end of the build in order to display the set of environment variables used for each build

More info: [the plugin's Wiki page](https://wiki.jenkins-ci.org/display/JENKINS/EnvInject+Plugin)

## Changelog

See [GitHub Releases](https://github.com/jenkinsci/envinject-plugin/releases)
