import dev.jeka.core.tool.JkClass;
import dev.jeka.core.tool.JkDefClasspath;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.plugins.jacoco.JkPluginJacoco;
import dev.jeka.plugins.springboot.JkPluginSpringboot;

@JkDefClasspath("dev.jeka:springboot-plugin:3.0.0.RC11")
@JkDefClasspath("../dev.jeka.plugins.jacoco/jeka/output/dev.jeka.jacoco-plugin.jar")
class Build extends JkClass {

    final JkPluginSpringboot springboot = getPlugin(JkPluginSpringboot.class);

    final JkPluginJacoco jacoco = getPlugin(JkPluginJacoco.class);

    @Override
    protected void setup() {
        jacoco.enabled = true;
        jacoco.xmlReport = true;
        springboot.setSpringbootVersion("2.5.5");
        springboot.javaPlugin().getProject().simpleFacade()
            .setCompileDependencies(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-web")
            )
            .setTestDependencies(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-test")
                    .withLocalExclusions("org.junit.vintage:junit-vintage-engine")
            );
    }

    @JkDoc("Cleans, tests and creates bootable jar.")
    public void cleanPack() {
        clean(); springboot.javaPlugin().pack();
    }

    // Clean, compile, test and generate springboot application jar
    public static void main(String[] args) {
        JkInit.instanceOf(Build.class, args).cleanPack();
    }

}