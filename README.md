#ee-log-helper
eclipse plugin to generate plain old entry/exit logging

This plugin has two main functions:
* 1. generate new logging boilerplate for new classes and methods
* 2. updating existing log statements after refactoring

##1. Log Statement creation
###1.1. general
logs can be created for the whole File or for a single method. The respective commands can be found ins the source menu:
![image of source menu](doc/menu_contributions.png)
or launched using the shortcuts:
* <kbd>CTRL</kbd>+<kbd>ALT</kbd>+<kbd>L</kbd> add Logging to current method
* <kbd>CTRL</kbd>+<kbd>ALT</kbd>+<kbd>SHIFT</kbd>+<kbd>L</kbd> add Logging to current File
* <kbd>CTRL</kbd>+<kbd>ALT</kbd>+<kbd>U</kbd> Update Logging in current File

###1.2. Log Type
for performance reasons, the evaluation if a log statement should be executed can be done in two different places:
1. at the beginning of every method (per-invocation)
2. at the class creation, once per instance lifetime (per-instance)
the respective mode can be annotated on the class, so that the developers intend is clearly communicated to the generator and to other developers.

###1.3. Log Style
the method name that needs to be passed to many Logger calls can be handled in two different ways:
1. as String Literal
2. as variable at the start of the method
Depending on how the plugin is configured, it will use either style.

##2. Log Statement update
###2.1. general 
the plugin will scan all methods for calls to the java.util.logging.Logger. It knows about its methods and will update:
* Class name
* Method name
* return objects
respectively.

###2.2. Log Type
update will not interfere with the log style at all. It will just replace/add/remove parameter of existing log statements. Parameters might be added/removed when the input parameter and/or return type of a method changes to correct the "entering" and/or "exiting" statement(s).

###2.3. Log Style
The update will handle this on a per method basis: if it finds a "LOG_METHOD" variable of type String, it will use this. If not, it will use the String Literal.
