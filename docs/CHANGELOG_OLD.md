Release Notes (archive)
=============

#### Newer releases

See [GitHub Releases](https://github.com/jenkinsci/envinject-plugin/releases)

#### 2.1.6 (Jul 07, 2018)

Release with minor improvements in the codebase:

-   [![(info)](images/improvement.svg)
    PR \#131](https://github.com/jenkinsci/envinject-plugin/pull/131) -
    refactor the code to prevent warnings about attempts to
    (de-)serialize anonymous classes in Jenkins 2.107.2+
-   [![(info)](images/improvement.svg)
    PR \#129](https://github.com/jenkinsci/envinject-plugin/pull/129) -
    update the plugin so that it can be tested with [Plugin Compat
    Tester](https://github.com/jenkinsci/plugin-compat-tester)
-   [![(info)](images/improvement.svg)
    PR \#130](https://github.com/jenkinsci/envinject-plugin/pull/130) -
    fix typo in the Groovy script variable documentation

#### 2.1.5 (Oct 13, 2017)

-   Add compatibility notice for issues reported in 2.1.4

#### 2.1.4 (Oct 07, 2017)

-   [![(error)](images/bug.svg)](https://jenkins.io/security/advisory/2017-04-10/#environment-injector-envinject-plugin)[JENKINS-26583](https://issues.jenkins-ci.org/browse/JENKINS-26583) -
    The plugin does not longer override Environment Variables from
    plugins by the values persisted in its cache.
    -   This update does not solve all potential conflicts between
        EnvInject and other environment-contributing plugins
-   [![(error)](images/bug.svg)](https://jenkins.io/security/advisory/2017-04-10/#environment-injector-envinject-plugin)[JENKINS-46479](https://issues.jenkins-ci.org/browse/JENKINS-46479) -
    Prevent environment variables from being deleted on existing builds
    when the action is saved before cache initialization.
-   ![(info)](images/improvement.svg) Update
    EnvInject Lib from 1.26 to 1.27 in order to pick FindBugs fixes
    and [JENKINS-46479](https://issues.jenkins-ci.org/browse/JENKINS-46479).

#### 2.1.3 (July 3, 2017)

-   [![(error)](images/bug.svg)](https://jenkins.io/security/advisory/2017-04-10/#environment-injector-envinject-plugin)
    [](https://issues.jenkins-ci.org/browse/JENKINS-32428)
    [JENKINS-32428](https://issues.jenkins-ci.org/browse/JENKINS-32428) -
    Fix glitches in JSON escaping in REST API (regression in 2.1.1)

#### 2.1.2 (Jun 28, 2017)

-   [![(error)](images/bug.svg)](https://jenkins.io/security/advisory/2017-04-10/#environment-injector-envinject-plugin)
    [](https://issues.jenkins-ci.org/browse/JENKINS-32428)
    [JENKINS-45056](https://issues.jenkins-ci.org/browse/JENKINS-45056)
    - Fix issue with the incorrect [EnvInject API
    Plugin](https://wiki.jenkins.io/display/JENKINS/EnvInject+API+Plugin)
    1.0 packaging. Now there is a plugin dependency
-   [![(error)](images/bug.svg)](https://jenkins.io/security/advisory/2017-04-10/#environment-injector-envinject-plugin)
    [](https://issues.jenkins-ci.org/browse/JENKINS-32428)
    [JENKINS-45055](https://issues.jenkins-ci.org/browse/JENKINS-45055) -
    Update EnvInject Lib from 1.25 to 1.26 in order to pick FindBugs
    fixes
    ([changelog](https://github.com/jenkinsci/envinject-lib/blob/master/CHANGELOG.md#126))

#### 2.1.1 (Jun 20, 2017)

-   [![(error)](images/bug.svg)](https://jenkins.io/security/advisory/2017-04-10/#environment-injector-envinject-plugin)
    [JENKINS-32428](https://issues.jenkins-ci.org/browse/JENKINS-32428) -
    Escape XML and JSON outputs in REST API for "Injected Environment
    Variables"
-   [![(error)](images/bug.svg)](https://jenkins.io/security/advisory/2017-04-10/#environment-injector-envinject-plugin)
    [JENKINS-44263](https://issues.jenkins-ci.org/browse/JENKINS-44263) -
    Prevent showing empty "Injected Environment Variables" list after
    build loading from the disk due (reliance on the cached value)

#### 2.1 (May 12, 2017)

This is an API release, which is a preparation for a better Jenkins
Pipeline support in the plugin
([JENKINS-42614](https://issues.jenkins-ci.org/browse/JENKINS-42614)).

-   [![(plus)](images/rfe.svg) JENKINS-43845](https://issues.jenkins-ci.org/browse/JENKINS-43845) -
    Create new [EnvInject
    API](https://wiki.jenkins.io/display/JENKINS/EnvInject+API+Plugin)
    plugin, which provides new Pipeline-compatible API of EnvInject Lib
    and EnvInject API util classes
-   [![(plus)](images/rfe.svg) JENKINS-43536](https://issues.jenkins-ci.org/browse/JENKINS-43536) -
    Rework internal logic to avoid using AbstractProject explicitly
    where possible
-   [![(plus)](images/rfe.svg) JENKINS-43535](https://issues.jenkins-ci.org/browse/JENKINS-43535) -
    Deprecate old API in EnvInject Lib and provide migration guidelines

Expected behavior changes:

-   AbstractProject types (Freestyle, Matrix, JobDSL, etc.) - nothing
    changes in the behavior
-   Jenkins Pipeline - some use-cases may start or stop working. Jenkins
    Pipeline stays untested (see Known Limitations)

#### 2.0 (Apr 10th, 2017)

This is a security release of the plugin.

-   [![(error)](images/bug.svg) SECURITY-256](https://jenkins.io/security/advisory/2017-04-10/#environment-injector-envinject-plugin) -
    Arbitrary Groovy code execution vulnerability
-   [![(error)](images/bug.svg) SECURITY-348](https://jenkins.io/security/advisory/2017-04-10/#environment-injector-envinject-plugin-allows-low-privilege-users-to-access-parts-of-arbitrary-files-on-master)
    - Low privilege users are able to read parts of some files on master

  

| WARNING: This plugin release is not fully compatible with Previous EnvInject versions. A manual job configuration update may be required. See the referenced issues for more information. |
| --- |

#### 1.93.1 (Sep 30th, 2016)

-   ![(plus)](images/rfe.svg) [JENKINS-31829](https://issues.jenkins-ci.org/browse/JENKINS-31829) -
    Add currentListener Groovy variable
-   ![(info)](images/improvement.svg)
    [JENKINS-38607](https://issues.jenkins-ci.org/browse/JENKINS-38607) -
    Jenkins Update Center now shows compatibility warnings when
    upgrading from versions below 1.93 (See the compatibility notice
    regarding
    [JENKINS-31573](https://issues.jenkins-ci.org/browse/JENKINS-31573))
-   ![(error)](images/bug.svg)
    [PR \#109](https://github.com/jenkinsci/envinject-plugin/pull/109) -
    Incorrect initialization of the field in EnvInject build wrapper
    with Deprecated API. No user-visible impact
-   ![(info)](images/improvement.svg)
    [PR \#109](https://github.com/jenkinsci/envinject-plugin/pull/108) -
    Escape all plugin Jelly views by default
-   ![(info)](images/improvement.svg)
    [PR \#107](https://github.com/jenkinsci/envinject-plugin/pull/107) -
    Upgrade Jenkins Test Harness framework to the latest version,
    improve test coverage

#### 1.93 (Sep 26th, 2016)

-   ![(info)](images/improvement.svg)
    Update to the new parent POM, 1.609.3 is a new target core version
-   ![(error)](images/bug.svg)
    [JENKINS-31573](https://issues.jenkins-ci.org/browse/JENKINS-31573) -
    Incorrect parsing of newline characters
-   ![(error)](images/bug.svg)
    [JENKINS-30028](https://issues.jenkins-ci.org/browse/JENKINS-30028) -
    [Timestamper](https://wiki.jenkins.io/display/JENKINS/Timestamper)
    was displaying shifted timestamps due to the integration issue with
    EnvInject
-   ![(error)](images/bug.svg)
    [JENKINS-36545](https://issues.jenkins-ci.org/browse/JENKINS-36545) -
    WORKSPACE variable was unavailable in Groovy script injector
-   ![(error)](images/bug.svg)
    [JENKINS-36466](https://issues.jenkins-ci.org/browse/JENKINS-36466) -
    Update env-inject-lib to 1.24 in order to avoid potential
    incompatibilities on core versions with detached and deleted Matrix
    Project plugin
-   ![(error)](images/bug.svg)
    [JENKINS-32693](https://issues.jenkins-ci.org/browse/JENKINS-32693) -
    Fix BuildCauseRetriever when a build has null cause action
-   ![(error)](images/bug.svg)
    Cleanup issues reported by FindBugs: tolerance against node
    disconnection, etc.
-   ![(info)](images/improvement.svg)
    [JENKINS-23274](https://issues.jenkins-ci.org/browse/JENKINS-23274) -
    Do not output contents of the "Evaluated Groovy script" to console
-   ![(info)](images/improvement.svg)
    Document the current behavior of Node and Jenkins properties
    ([JENKINS-23666](https://issues.jenkins-ci.org/browse/JENKINS-23666))
-   ![(info)](images/improvement.svg)
    Improve diagnostics of issues during the environment setup in
    EnvInjectBuildWrapper (e.g.
    [JENKINS-36237](https://issues.jenkins-ci.org/browse/JENKINS-36237))

##### Compatibility notes:

-   Fix of
    [JENKINS-31573](https://issues.jenkins-ci.org/browse/JENKINS-31573)
    -   The change introduces a new property file parsing engine, which
        starts following the standard for backslashes
    -   You may need to update the property files (e.g. convert
        backslash entries to double backslash)

#### 1.92.1 (Aug 21th, 2015)

-   ![(error)](images/bug.svg)
    [JENKINS-27382](https://issues.jenkins-ci.org/browse/JENKINS-27382) -
    EnvInjectPluginAction::buildEnvVars() injects masks instead of
    passwords to the environment (regression since 1.90)
    -   There are many similar issues in JIRA, which have been probably
        fixed by the patch

#### 1.92 (Aug 18th, 2015)

-   ![(plus)](images/rfe.svg)
    [JENKINS-29817](https://issues.jenkins-ci.org/browse/JENKINS-29817) -
    Contribute Job Property Contents to the project environment
    (polling, etc.)

-   ![(plus)](images/rfe.svg)
    [JENKINS-29867](https://issues.jenkins-ci.org/browse/JENKINS-29867) -
    Permissions engine + global option for disabling the Injected vars
-   ![(error)](images/bug.svg)
    Fix several issues discovered by FindBugs

#### 1.91.4 (Aug 4th, 2015)

-   ![(error)](images/bug.svg)
    Fix
    [JENKINS-27665](https://issues.jenkins-ci.org/browse/JENKINS-27665)
    and
    [JENKINS-27363](https://issues.jenkins-ci.org/browse/JENKINS-27363) -
    Prevent NPEs when handling null lists of sensitive variables
    (passwords, etc.)
-   ![(error)](images/bug.svg)
    Fix
    [JENKINS-29667](https://issues.jenkins-ci.org/browse/JENKINS-29667) -
    Text description in the plugin manager was bold

#### 1.91.3 (May 15th, 2015)

-   ![(error)](images/bug.svg)
    Fix
    [JENKINS-28409](https://issues.jenkins-ci.org/browse/JENKINS-28409) -
    File descriptor leak in "Inject Passwords" build wrapper

-   ![(error)](images/bug.svg)
    Fix
    [JENKINS-28188](https://issues.jenkins-ci.org/browse/JENKINS-28188) -
    StringIndexOutOfBoundsException when no root cause for the build

#### 1.91.2 (Apr 14th, 2015)

-   ![(error)](images/bug.svg)
    Fix
    [JENKINS-27496](https://issues.jenkins-ci.org/browse/JENKINS-27496) -
    NPE during submission of EnvInject JobProperty when the form is
    empty (regression, since 1.91)

#### 1.91.1 (Mar 11th, 2015)

-   ![(error)](images/bug.svg)
    Fix
    [JENKINS-27342](https://issues.jenkins-ci.org/browse/JENKINS-27342) -
    NPE in EnvInjectPluginAction.transformEntry() when there is no
    sensible variables (regression, since 1.91)

#### 1.91 (Mar 8th, 2015)

* Fix
[JENKINS-19852](https://issues.jenkins-ci.org/browse/JENKINS-19852) -
NPE during submission of EnvInject JobProperty configurations w/o admin
permissions  
* Fix
[JENKINS-19222](https://issues.jenkins-ci.org/browse/JENKINS-19222) -
EnvInject undefines NODE\_NAME environment variable on master  
* Fix
[JENKINS-23447](https://issues.jenkins-ci.org/browse/JENKINS-23447) -
Mask sensitive data in injectedEnvVars.txt when displayed on UI or
persisted on disk  
* Fix
[JENKINS-24785](https://issues.jenkins-ci.org/browse/JENKINS-24785) -
BUILD\_CAUSE is always UPSTREAMTRIGGER in multi-configuration jobs

  

|                                                                                                     |
|-----------------------------------------------------------------------------------------------------|
| There are showstopper bugs reported to this version. Please avoid the upgrade till the next release |

#### 1.90

* [JENKINS-24130](https://issues.jenkins-ci.org/browse/JENKINS-24130) -
Added an option to mask PasswordParameters  
* Execute Groovy scripts to inject environment variables in Build
Wrappers  
* Fix
[JENKINS-22126](https://issues.jenkins-ci.org/browse/JENKINS-22126) -
Override build parameters only when explicitly requested  
* EnvInject escapes a sensitive data from other Environment
contributors  
* Fix
[JENKINS-22169](https://issues.jenkins-ci.org/browse/JENKINS-22169) -
Migration from [Setenv
Plugin](https://wiki.jenkins.io/display/JENKINS/Setenv+Plugin)
overwrites existing envinject configurations

#### 1.89

* Don't mask stacktraces on errors  
* Upgrade to envinject-lib-1.19

#### 1.88

* Fix
[JENKINS-16316](https://issues.jenkins-ci.org/browse/JENKINS-16316) -
Changes to global variables not honored  
* Log both source and resolved unreachable properties file paths  
* Fix
[JENKINS-14144](https://issues.jenkins-ci.org/browse/JENKINS-14144) -
use a WorkspaceListener to avoid injecting a temporary

#### 1.87

* Actually load script from master

#### 1.86

* Fix reopen
[JENKINS-13348](https://issues.jenkins-ci.org/browse/JENKINS-13348) -
EnvInject overriding WORKSPACE variable

#### 1.85

* Retain line break characters in property values.

#### 1.84

* Add logger to Groovy script evaluation

#### 1.83

* Fix reopen
[JENKINS-16316](https://issues.jenkins-ci.org/browse/JENKINS-16316) -
Global variables not updated

#### 1.82

* Fix
[JENKINS-16566](https://issues.jenkins-ci.org/browse/JENKINS-16566) -
poll SCM not work after upgrade the EnvInject Plugin to 1.81  
* Fix
[JENKINS-16575](https://issues.jenkins-ci.org/browse/JENKINS-16575) -
SEVERE: Failed to record SCM polling - java.lang.NullPointerException -
EnvInject Plugin  
* Update to envinject-lib 1.15

#### 1.81

* Fix
[JENKINS-16239](https://issues.jenkins-ci.org/browse/JENKINS-16239) -
NPE in getRootDir  
** Update to envinject-lib 1.14  
* Remove Injected from Labels  
* Fix
[JENKINS-16380](https://issues.jenkins-ci.org/browse/JENKINS-16380) -
Starting with \>1.73 global environment variables overrule build
specific parameter variables  
* Fix
[JENKINS-16372](https://issues.jenkins-ci.org/browse/JENKINS-16372)
-Password parameter is malformed

#### 1.80

* Fix
[JENKINS-16233](https://issues.jenkins-ci.org/browse/JENKINS-16233) -
EnvInject plugin using a cached value for ${WORKSPACE}

#### 1.79

* Fix
[JENKINS-16316](https://issues.jenkins-ci.org/browse/JENKINS-16316) -
Global variables not updated  
* Fix
[JENKINS-16399](https://issues.jenkins-ci.org/browse/JENKINS-16399) -
SCM documentation is incorrect

#### 1.78

* Fix
[JENKINS-16219](https://issues.jenkins-ci.org/browse/JENKINS-16219)
regression from
[JENKINS-14437](https://issues.jenkins-ci.org/browse/JENKINS-14437) -
Version 1.77 injects all **environment** variables into ANT command line
(-D)

#### 1.77

* Fix
[JENKINS-14437](https://issues.jenkins-ci.org/browse/JENKINS-14437) -
envinject fails to "really" set/override build parameters

#### 1.76

* Fix
[JENKINS-16016](https://issues.jenkins-ci.org/browse/JENKINS-16016) -
Global passwords are visible for matrix configuration builds

#### 1.75

* Add currentJob and currentBuild variables for Groovy scripts

#### 1.74

* Correct spelling of Password from Password ( merged from pull
request)

#### 1.73

* Fix
[JENKINS-15658](https://issues.jenkins-ci.org/browse/JENKINS-15658) -
EnvInject Undefines Jenkins Global properties referencing WORKSPACE
variable  
* Fix
[JENKINS-15664](https://issues.jenkins-ci.org/browse/JENKINS-15664) -
Help text for EnvInject plugin is wrong

#### 1.72

* Fix
[JENKINS-15146](https://issues.jenkins-ci.org/browse/JENKINS-15146) -
EnvInject unsets empty string properties returned in maps

#### 1.71

* Support for new job types  
* Replace "white-list" strategy for supported job types by use of
BuildableItemWithBuildWrappers interface to access the job's
buildWrapper list.

#### 1.70

* Fix
[JENKINS-15071](https://issues.jenkins-ci.org/browse/JENKINS-15071) -
Editing the description of Jobs throws an Error 500 message  
* Update to envinject-lib 1.13

#### 1.69

* Fix
[JENKINS-14930](https://issues.jenkins-ci.org/browse/JENKINS-14930) -
Can't overload/update PYTHONPATH

#### 1.68

* Fix
[JENKINS-14897](https://issues.jenkins-ci.org/browse/JENKINS-14897) -
ConfigFileProvider variable is not seen by EnvInject  
* Require Jenkins 1.444+

#### 1.67

* Fix
[JENKINS-14761](https://issues.jenkins-ci.org/browse/JENKINS-14761) -
Backslash broken on Properties Content injection  
* Fix
[JENKINS-14768](https://issues.jenkins-ci.org/browse/JENKINS-14768) -
Backslash is escaped from injected $WORKSPACE property on slave node

#### 1.66

* Fix
[JENKINS-14686](https://issues.jenkins-ci.org/browse/JENKINS-14686) -
Fail evaluating Groovy script before job run

#### 1.65

* Add feature - Mask job password parameter value

#### 1.64

* Fix
[JENKINS-14645](https://issues.jenkins-ci.org/browse/JENKINS-14645) -
Executing external job results in an ClassCastException (ExternalRun
cannot be cast to AbstractBuild)

#### 1.63

* Fix reponed
[JENKINS-14371](https://issues.jenkins-ci.org/browse/JENKINS-14371) -
NullPointerException in EnvInjectBuilderContributionAction.buildEnvVars
during SCM poll  
* Fix
[JENKINS-14459](https://issues.jenkins-ci.org/browse/JENKINS-14459) -
Failed to record SCM polling with envinject plugin

#### 1.62

* Fix
[JENKINS-14371](https://issues.jenkins-ci.org/browse/JENKINS-14371) -
NullPointerException in EnvInjectBuilderContributionAction.buildEnvVars
during SCM poll

#### 1.61

* Fix
[JENKINS-14367](https://issues.jenkins-ci.org/browse/JENKINS-14367) -
Global & node level self-referencing variables should work

#### 1.60

* Fix
[JENKINS-14290](https://issues.jenkins-ci.org/browse/JENKINS-14290) -
Support for Ivy project type

#### 1.59

* Fix
[JENKINS-14271](https://issues.jenkins-ci.org/browse/JENKINS-14271) -
EnvInject claims global env vars are unresolved on slave

#### 1.58

* Fix
[JENKINS-14284](https://issues.jenkins-ci.org/browse/JENKINS-14284) -
Single backslashes are droped or used as escape char

#### 1.57

* Fix
[JENKINS-14232](https://issues.jenkins-ci.org/browse/JENKINS-14232) -
EnvInject not handling properties file with logical lines

#### 1.56

* Load environment variables from the node (therefore, you are able to
redefine classic variables such as the PATH variable at node level)  
* Upgrade to envinject-lib 1.8

#### 1.55

* Technical release: Merge pull request - Make EnvInjectPluginAction
implements EnvironmentContributingAction

#### 1.54

* A few minor changes to keep jdk5 compatibility

#### 1.53

* Fix regression on backward compatibility

#### 1.52

* Remove env variables presence in build serialization file

#### 1.51

* Add a complement to fix
[JENKINS-12423](https://issues.jenkins-ci.org/browse/JENKINS-12423) -
Password masked by Mask Passwords are visible when using envinject
plugin (no display environment variables when the job is running)

#### 1.50

* Update to envinject-lib 1.7 (Enable to external plugin to retrieve
envVars)

#### 1.49

* Merge pull request - use UnsupportedOperationException instead of
UnsupportedMediaException  
* Merge pull request - added Japanese localization

#### 1.48

* Fix
[JENKINS-12878](https://issues.jenkins-ci.org/browse/JENKINS-12878) -
manage old data fails with RuntimeException: Failed to serialize
hudson.model.Actionable\#actions for class hudson.model.FreeStyleBuild

#### 1.47

* Fix
[JENKINS-13566](https://issues.jenkins-ci.org/browse/JENKINS-13566) -
EnvInject is messing around with my TEMP variable when username contains
a dollar sign (windows)  
* Update to envinject-lib 1.5

#### 1.46

* Fix interaction with multijob plugin

#### 1.45

* Add /export/txt, /export/xml and /export/json for the exportation

#### 1.44

* Fix EnvInject Password process

#### 1.43

* Fix reopened
[JENKINS-12108](https://issues.jenkins-ci.org/browse/JENKINS-12108) -
EnvInject failure using multi-configuration jobs

#### 1.42

* Fix
[JENKINS-13183](https://issues.jenkins-ci.org/browse/JENKINS-13183) -
SCM variables resolution

#### 1.41

* Fix
[JENKINS-13157](https://issues.jenkins-ci.org/browse/JENKINS-13157) -
EnvInject cannot inject variables whose contents contain a dollar sign
($)

#### 1.40

* Fix
[JENKINS-13167](https://issues.jenkins-ci.org/browse/JENKINS-13167) -
Cannot use EnvInject in a Maven 2 Jenkins project

#### 1.39

* Fix
[JENKINS-13155](https://issues.jenkins-ci.org/browse/JENKINS-13155) -
Parameters are not working in EnvInject plugin 1.38

#### 1.38

* Fix
[JENKINS-13119](https://issues.jenkins-ci.org/browse/JENKINS-13119) -
Add feature: Set an environment variable based on value of user passed
parameter

#### 1.37

* Fix
[JENKINS-13085](https://issues.jenkins-ci.org/browse/JENKINS-13085) -
Environment Variable Injection doesn't work when project run on slave
node that sets the same variable

#### 1.36

* Fix
[JENKINS-13041](https://issues.jenkins-ci.org/browse/JENKINS-13041) -
PATH variable is being injected from master on multiconfiguration jobs

#### 1.35

* Fix
[JENKINS-13022](https://issues.jenkins-ci.org/browse/JENKINS-13022) -
EnvInject 1.33 doesn''t seem to work with Jenkins 1.454

#### 1.34

* Change Environment contributors resolution

#### 1.33

* Move global passwords injection to the 'Prepare job environment'
section  
* Add 'Inject job passwords'

#### 1.32

* Fix
[JENKINS-12944](https://issues.jenkins-ci.org/browse/JENKINS-12944) -
Env Inject Plugin doesn't substitute ${WORKSPACE} variable at all when
used in 'Preparing an environment for the job'  
* Fix
[JENKINS-12963](https://issues.jenkins-ci.org/browse/JENKINS-12963) -
EnvInject plugin causes job to use JAVA\_HOME instead of configured JDK

#### 1.31

* Fix
[JENKINS-12936](https://issues.jenkins-ci.org/browse/JENKINS-12936) -
EnvInject Plugin 1.30 does not display all properties on job config page

#### 1.30

* Fix
[JENKINS-12905](https://issues.jenkins-ci.org/browse/JENKINS-12905) -
PATH variable is not injected

#### 1.29

* Fix
[JENKINS-12876](https://issues.jenkins-ci.org/browse/JENKINS-12876) -
Version 1.20 and later removed all comments and new-lines  
* Add test to
[JENKINS-12841](https://issues.jenkins-ci.org/browse/JENKINS-12841) -
EnvInject Plugin 1.26 does not substitute ${WORKSPACE} correctly

#### 1.28

* Fix reponed
[JENKINS-12108](https://issues.jenkins-ci.org/browse/JENKINS-12108) -
EnvInject failure using multi-configuration jobs  
* Fix
[JENKINS-12841](https://issues.jenkins-ci.org/browse/JENKINS-12841) -
EnvInject Plugin 1.26 does not substitute ${WORKSPACE} correctly

#### 1.27

* Add a build context to EnvInjectContributor

#### 1.26

* Fix
[JENKINS-12704](https://issues.jenkins-ci.org/browse/JENKINS-12704) -
WORKSPACE variable for concurrent builds are not defined properly.

#### 1.25

* Fix
[JENKINS-12809](https://issues.jenkins-ci.org/browse/JENKINS-12809) -
Injecting from file removes backslashes on ${WORKSPACE}

#### 1.24

* Add the ability to inject password values  
* Fix
[JENKINS-12423](https://issues.jenkins-ci.org/browse/JENKINS-12423) -
Password masked by Mask Passwords are visible when using envinject
plugin

#### 1.23

* Fix
[JENKINS-12788](https://issues.jenkins-ci.org/browse/JENKINS-12788) -
Missing variables when executing EnvInject script

#### 1.22

* Fix NullPointerException for a setenv or a envfile migration

#### 1.21

* Fix
[JENKINS-12746](https://issues.jenkins-ci.org/browse/JENKINS-12746) -
envinject strips off "\\" each time config page is opened

#### 1.20

* Move properties content serialization to a Map (avoid carriage return
issues)  
* Fix usage of build variables for the 'Prepare environment jobs'  
* Fix Injected environment vars when EnvInject is not configured

#### 1.19

* Fix reopened
[JENKINS-12423](https://issues.jenkins-ci.org/browse/JENKINS-12423) -
Password masked by Mask Passwords are visible when using envinject
plugin

#### 1.18

* Fix
[JENKINS-12691](https://issues.jenkins-ci.org/browse/JENKINS-12691) -
Property variable not set if using a property file

#### 1.17

* Fix migration from the setenv and envfile plugin

#### 1.16

* Fix external EnvInjectContribution section display  
* Fix text typo

#### 1.15 (technical release)

* Refactoring + fix wrong commit

#### 1.14

* Fix
[JENKINS-12416](https://issues.jenkins-ci.org/browse/JENKINS-12416) -
Environment is not injected for each configuration in a
multi-configuration project

#### 1.13

* Add the extension point EnvInject Job Property Contributor to enable
other plugins to contribute to this plugin for the prepare job step

#### 1.12

* Fix
[JENKINS-12423](https://issues.jenkins-ci.org/browse/JENKINS-12423) -
Password masked by Mask Passwords are visible when using envinject
plugin

#### 1.11

* Update to envinject-lib 1.2

#### 1.10

* Update to envinject-lib 1.1  
* Fix bug on the build end

#### 1.9

* Export reuse code to a dedicated library: envinject-lib 1.0  
* Fix typo

#### 1.8

* Switch properties section and scripts section for the prepare step

#### 1.7

* Fix
[JENKINS-12293](https://issues.jenkins-ci.org/browse/JENKINS-12293) -
Incorrect inserting environment variables on linux slave

#### 1.6

* Recording EnvVars contributing by other plugins at the end of the
build

#### 1.5

* Fix
[JENKINS-12252](https://issues.jenkins-ci.org/browse/JENKINS-12252) -
envinject does not fail the build after Environment Script Content fails

#### 1.4

* Fix portability in the history build.xml file (using the build
reference instead of the absolute path of the EnvInjected file)  
* Add the ability to load a properties file at node startup (at master
and/or at slave startup)

#### 1.3

* Move 'Triggered Build Cause' to Jenkins build variables option  
* Add an export option to export environment variables to TXT, XML or
JSON

#### 1.2

* Fix NullPointerException when master has 0 executors

#### 1.1

* Fix
[JENKINS-12108](https://issues.jenkins-ci.org/browse/JENKINS-12108) -
EnvInject failure using multi-configuration jobs  
* Fix
[JENKINS-12084](https://issues.jenkins-ci.org/browse/JENKINS-12084)
-Variables resolved alphabetically rather than order found in
content/file. ${WORKSPACE} not always getting resolved as result  
* Accept script parameters to script path sections

#### 1.0

* Fix
[JENKINS-12027](https://issues.jenkins-ci.org/browse/JENKINS-12027) -
${WORKSPACE} variable doesn't use my specific workspace directory
anymore in a build step  
* Make it stable for production

#### 0.26

* Enhance display log  
* Refactoring for fixing properties variables propagation

#### 0.25

* Fix
[JENKINS-11763](https://issues.jenkins-ci.org/browse/JENKINS-11763) -
Can not set some 'special' variable names the first time  
* Technical Refactoring

#### 0.24

* Make it extensible by an another plugin.

#### 0.23

* Fixed
[JENKINS-11595](https://issues.jenkins-ci.org/browse/JENKINS-11595) -
Some env variables can not be used to inject, for example $NODE\_NAME

#### 0.22

* Add the ability for the 'prepare environment' feature to load the
properties file and the script file from the master node even if the
build runs on a slave node.

#### 0.21

* Add global properties and node properties process

#### 0.20

* Fix
[JENKINS-11439](https://issues.jenkins-ci.org/browse/JENKINS-11439) -
Can't add "environment inject" to
[pre-scm-buildstep](https://wiki.jenkins.io/display/JENKINS/pre-scm-buildstep)
plugin.

#### 0.19

* Fix
[JENKINS-11181](https://issues.jenkins-ci.org/browse/JENKINS-11181)
-Variables failing to be set if referencing previous variable

#### 0.18

* Fix
[JENKINS-11067](https://issues.jenkins-ci.org/browse/JENKINS-11067) -
Pre-SCM and Post-SCM environment scripts do not use Pre-SCM and Post-SCM
properties

#### 0.17

* Fix
[JENKINS-11066](https://issues.jenkins-ci.org/browse/JENKINS-11066) -
Property Variables "Randomly" Not Working/Getting Unset

#### 0.16

* Fix
[JENKINS-11063](https://issues.jenkins-ci.org/browse/JENKINS-11063) -
Multiple "Inject Environment Variables" Build Steps All Show Same
Content

#### 0.15

* Backslashes in file path for windows platform are managed  
* Added feature 'unset variable' when the variable is unresolved

#### 0.14

* Integrate domi pull request (add trigger causes as environment
variables)  
* Fix
[JENKINS-10980](https://issues.jenkins-ci.org/browse/JENKINS-10980) -
EnvInject to optinally trim trailing spaces

#### 0.13.1

* Improve reporting errors

#### 0.13

* Fix
[JENKINS-10919](https://issues.jenkins-ci.org/browse/JENKINS-10919) -
Variables are shared between jobs

#### 0.12

* Fixed
[JENKINS-10916](https://issues.jenkins-ci.org/browse/JENKINS-10916) -
Properties not injected and build marked as failed

#### 0.11.1 (technical release)

* Remove unused maven dependency

#### 0.11

* Fix
[JENKINS-10894](https://issues.jenkins-ci.org/browse/JENKINS-10894) -
Inject environment variables with property file not working

#### 0.10

* Add the ability to unset system variable at node level  
* Add an option to 'Keep Jenkins system variables'  
* Fix
[JENKINS-10845](https://issues.jenkins-ci.org/browse/JENKINS-10845) and
[JENKINS-10877](https://issues.jenkins-ci.org/browse/JENKINS-10877) -
master/slave sync / Job variables/parameters are missing  
* Complete the fix to
[JENKINS-10847](https://issues.jenkins-ci.org/browse/JENKINS-10847)

#### 0.9

* Fix partially
[JENKINS-10847](https://issues.jenkins-ci.org/browse/JENKINS-10847) and
[JENKINS-10845](https://issues.jenkins-ci.org/browse/JENKINS-10845) -
Enivronment is not separated  
* The ability to 'Keep system variables' has been removed. The feature
will be restored later (and moved at slave/node level (not at job
level).

#### 0.8

* Fixed variables restauration for Maven jobs

#### 0.7

* Fixed variables propagation between typologies  
* Fixed variables restore for all typologies

#### 0.6

* Add a 'Keep Jenkins Build variables' option  
* Add 'EnvInject' as prefix for plugin log messages  
* Sort the EnvInject variables table

#### 0.5

* Remove advanced sections

#### 0.4

* Add a build wrapper  
* Fix serialization fiels for JobProperty  
* Add a migration procedure for setenv and envfile plugins

#### 0.3

* Add variables resolution in elements

#### 0.2.1

* Fix environment script content process

#### 0.2

* Can inject environment variables as a build step.

#### 0.1

* Initial versio

 

