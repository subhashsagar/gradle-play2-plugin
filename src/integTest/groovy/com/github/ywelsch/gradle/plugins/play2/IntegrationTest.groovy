package com.github.ywelsch.gradle.plugins.play2

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.internal.consumer.DefaultGradleConnector
import org.junit.After
import org.junit.Test

abstract class IntegrationTest {
    def projectName
    def playVersion
    def scalaVersion
    def lang
    def projectDir
    def buildFile
    def output

    public IntegrationTest(projectName, lang, playVersion, scalaVersion) {
        this.projectName = projectName
        this.playVersion = playVersion
        this.scalaVersion = scalaVersion
        this.lang = lang
        projectDir = new File("samples/playframework-$playVersion/samples/$lang/$projectName")
        buildFile = new File(projectDir, 'build.gradle')
    }

    /*@Parameterized.Parameters(name = "{index}: {0}:{1}:{2}:{3}")
    public static Collection playProjects() {
        def res = []

        ['2.2.0', '2.2.1', '2.2.2', '2.2.3', '2.2.4', '2.2.5',
         '2.3.0', '2.3.1', '2.3.2', '2.3.3', '2.3.4', '2.3.5'].each { playVersion ->
            ['java', 'scala'].each { lang ->
                new File("samples/playframework-$playVersion/samples/$lang").list().each { projectName ->
                    def scalaVersions = ['2.10']
                    if (playVersion.startsWith('2.3.')) {
                        scalaVersions << '2.11'
                    }
                    scalaVersions.each { scalaVersion ->
                        res << [projectName, lang, playVersion, scalaVersion].toArray()
                    }
                }
            }
        }

        res
    }*/


    String customSettings() {
        def res = ''

        if (lang == 'java' && ['computer-database', 'forms', 'zentasks'].contains(projectName)) {
            res += '''
                play2.ebeanEnabled = true
                '''
        }

        if (lang == 'java' && projectName == 'computer-database-jpa') {
            res += '''
                dependencies {
                    compile "com.typesafe.play:play-java-jpa_$play2.scalaVersion:$play2.version"
                    compile 'org.hibernate:hibernate-entitymanager:3.6.9.Final'
                }

                // hack to get hibernate to work with gradle
                sourceSets.main.output.resourcesDir = sourceSets.main.output.classesDir
                sourceSets.test.output.resourcesDir = sourceSets.test.output.classesDir
                '''
        }

        if (lang == 'scala' && projectName == 'computer-database') {
            res += '''
                dependencies {
                    compile "com.typesafe.play:anorm_$play2.scalaVersion:$play2.version"
                    compile "com.typesafe.play:play-jdbc_$play2.scalaVersion:$play2.version"
                }
                '''
        }

        if (lang == 'scala' && projectName == 'zentasks') {
            res+= '''
                dependencies {
                    compile "com.typesafe.play:anorm_$play2.scalaVersion:$play2.version"
                    compile "com.typesafe.play:play-jdbc_$play2.scalaVersion:$play2.version"
                }
                '''
        }

        def applyLess = '''
                buildscript {
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        classpath "com.github.houbie:lesscss-gradle-plugin:1.0.3-less-1.7.0"
                    }
                }

                apply plugin: 'lesscss'

                def lessResources = "$buildDir/resources/lessResources"

                lessc {
                    sourceDir 'app/assets'
                    include '**/*.less'
                    exclude '**/_*.less'
                    destinationDir = "$lessResources/public"
                    encoding = "utf-8"
                }

                sourceSets.main.output.dir(lessResources, builtBy: 'lessc')
                '''

        def applyCoffee = '''
                apply plugin: 'coffeescript-base'

                def coffeeResources = "$buildDir/resources/coffeeResources"

                repositories {
                    maven {
                        url "http://repo.gradle.org/gradle/javascript-public"
                    }
                }

                task compileCoffee(type: CoffeeScriptCompile) {
                    source fileTree('app/assets') {
                        include '**/*.coffee'
                    }
                    destinationDir file("$coffeeResources/public")
                }

                sourceSets.main.output.dir(coffeeResources, builtBy: 'compileCoffee')
                '''

        def applyJS = '''
                def appJSResources = "$project.buildDir/resources/appJSResources"

                project.tasks.create('copyAppJSResources', Sync) {
                    from fileTree('app/assets') {
                        include '**/*.js'
                    }
                    into "$appJSResources/public"
                }

                project.sourceSets.main.output.dir(appJSResources, builtBy: 'copyAppJSResources')
                '''

        def applyMinJS = '''
                buildscript {
                    repositories {
                        jcenter()
                    }
                    dependencies {
                        classpath "com.eriwen:gradle-js-plugin:1.12.1"
                    }
                }

                apply plugin: 'com.eriwen.gradle.js'

                def appJSResources = "$project.buildDir/resources/appJSResources"

                minifyJs {
                    source = file('app/assets/javascripts/main.js')
                    dest = file("$appJSResources/public/javascripts/main.min.js")
                    sourceMap = file("$appJSResources/public/javascripts/main.sourcemap.json")
                    closure {
                        warningLevel = 'QUIET'
                    }
                }

                project.sourceSets.main.output.dir(appJSResources, builtBy: 'minifyJs')
                '''

        def applyLessMin = '''
                buildscript {
                    repositories {
                        mavenCentral()
                    }
                    dependencies {
                        classpath "com.github.houbie:lesscss-gradle-plugin:1.0.3-less-1.7.0"
                        classpath 'com.eriwen:gradle-css-plugin:1.11.1'
                    }
                }

                apply plugin: 'lesscss'

                def lessResources = "$buildDir/resources/lessResources"

                lessc {
                    sourceDir 'app/assets'
                    include '**/*.less'
                    exclude '**/_*.less'
                    destinationDir = "$lessResources/public"
                    encoding = "utf-8"
                }

                sourceSets.main.output.dir(lessResources, builtBy: 'lessc')

                apply plugin: 'css'

                def appCssResources = "$buildDir/resources/appCssResources"

                minifyCss {
                    source = file("$lessResources/public/stylesheets/main.css")
                    dest = file("$appCssResources/public/stylesheets/main.min.css")
                }

                tasks.minifyCss.dependsOn lessc

                sourceSets.main.output.dir(appCssResources, builtBy: 'minifyCss')
                '''

        if (projectName == 'zentasks') {
            res+= applyLess + applyCoffee
        }

        if (projectName == 'comet-live-monitoring') {
            res+= applyLessMin + applyMinJS
        }

        res
    }

    @After
    public void cleanUp() {
        new File(projectDir, 'build').deleteDir()
        new File(projectDir, '.gradle').deleteDir()
        //buildFile.delete()
    }

    def classesDir = new File('build/classes/main').absolutePath
    def resourcesDir = new File('build/resources/main').absolutePath

    @Test
    public void runTestTask() {
        /*buildFile.text = genericPrelude() + """
            apply plugin: 'com.github.ywelsch.play2-$lang'

            play2 {
                version = '$playVersion'
                scalaVersion = '$scalaVersion'
            }

            tasks.withType(ScalaCompile) {
                scalaCompileOptions.fork = true

                scalaCompileOptions.useAnt = false
                //scalaCompileOptions.forkOptions.jvmArgs = ['-XX:MaxPermSize=512m']
                scalaCompileOptions.forkOptions.memoryMaximumSize = '1g'
            }
        """ + customSettings()*/
        //def initFile = new File('samples/init.gradle')
        //Assert.assertTrue initFile.exists()
        runTasks(['-u'/*, '--init-script', initFile.absolutePath*/], 'test')
    }

    String genericPrelude() {
        '''
        buildscript {
            dependencies {
                classpath files(new File(System.getProperty('testContextProjectDir'), 'build/classes/main'))
                classpath files(new File(System.getProperty('testContextProjectDir'), 'build/resources/main'))
            }
        }

        repositories {
            mavenCentral()
            maven {
                url 'http://repo.typesafe.com/typesafe/releases'
            }
        }
        '''
    }

    void runTasks(List<String> arguments = [], String... tasks) {
        ProjectConnection conn
        try {
            GradleConnector gradleConnector = GradleConnector.newConnector()
                    .forProjectDirectory(projectDir)
                    .useGradleVersion('2.1')
                    .useGradleUserHomeDir(new File('samples'))
            ((DefaultGradleConnector)gradleConnector).embedded(true) // hack: cast to internal API to prevent new daemon from spawning
            conn = gradleConnector.connect()
            ByteArrayOutputStream stream = new ByteArrayOutputStream()
            def builder = conn.newBuild()
            if (arguments) {
                builder.withArguments(*arguments)
            }
            builder.forTasks(tasks).withArguments('-DtestContextProjectDir=' + new File('.').absolutePath).setStandardOutput(stream).run()
            output = stream.toString()
        }
        finally {
            conn?.close()
        }
    }
}