![Build Status](https://github.com/jerkar/jacoco-plugin/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.jeka/jacoco-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.jeka/protobuf-plugin) <br/>

# Jeka library/plugin for Jacoco

Plugin to use the [Jacoco](https://www.eclemma.org/jacoco) coverage tool in your Java builds

## How to use

Just declare the plugin in your build class.  

```java
@JkDefClasspath("dev.jeka.plugins:protobuf:[version]")
public class Build extends JkClass {
    
    JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);
    
    JkPluginJacoco jacoco = getPlugin(JkPluginJacoco.class);

    ...
}
```
The plugin will configure java project in such tests are launched with jacoco agent. 
Jacoco reports are output in output/jacoco dir.

### Programmatically

You can use directly `JkJacoco` in build code to perform lower level actions.

### Bind Jacoco dynamically

You can invoke Jacoco plugin from command line on a Jeka project that does declare this plugin in its build class.

`jeka @dev.jeka.plugins:jacoco:[version] jacoco# java#pack`

To get help and options :
`jeka jacoco#help`

### Example

See example [here](dev.jeka.plugins.jacoco-sample)


## How to build this project

This project uses Jeka wrapper, you don't need to have Jeka installed on your machine. simply execute `./jekaw cleanPack`
from the root of this project.
