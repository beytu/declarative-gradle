package org.gradle.experimental.settings.internal

import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class DefaultWorkspaceSettingsTest extends Specification {
    @TempDir
    File projectDir

    def settings = Mock(Settings)
    def rootProject = Mock(ProjectDescriptor)
    def project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    def workspace = project.objects.newInstance(DefaultWorkspaceSettings, settings)

    def "sets the workspace name"() {
        when:
        workspace.build("foo") {

        }

        then:
        1 * settings.getRootProject() >> rootProject
        1 * rootProject.setName("foo")
    }

    def "adds projects to settings when projects are added to the workspace"() {
        given:
        def bar = Mock(ProjectDescriptor)
        def baz = Mock(ProjectDescriptor)
        def qux = Mock(ProjectDescriptor)
        def fuzz = Mock(ProjectDescriptor)

        when:
        workspace.build("foo") {

        }
        workspace.projects() {
            subproject("bar")
            subproject("baz") {
                subproject("qux")
            }
            directory("buzz") {
                subproject("fuzz")
            }
        }

        then:
        1 * settings.getRootProject() >> rootProject
        1 * settings.getRootDir() >> projectDir
        1 * rootProject.setName("foo")
        1 * settings.include(":bar")
        1 * settings.include(":baz")
        1 * settings.include(":baz:qux")
        1 * settings.include(":fuzz")

        and:
        1 * settings.project(":bar") >> bar
        1 * settings.project(":baz") >> baz
        1 * settings.project(":baz:qux") >> qux
        1 * settings.project(":fuzz") >> fuzz

        and:
        1 * bar.setProjectDir(new File(projectDir, "bar"))
        1 * baz.setProjectDir(new File(projectDir, "baz"))
        1 * qux.setProjectDir(new File(projectDir, "baz/qux"))
        1 * fuzz.setProjectDir(new File(projectDir, "buzz/fuzz"))

        and:
        0 * settings._
    }

    def "cannot configure the build twice"() {
        when:
        workspace.build('foo') {

        }

        then:
        1 * settings.getRootProject() >> rootProject

        when:
        workspace.build('foo') {

        }

        then:
        thrown(UnsupportedOperationException)

        when:
        workspace.build('bar') {
        }

        then:
        thrown(UnsupportedOperationException)
    }

    def "can autodetect projects in a multi-project build"() {
        given:
        createBuildFileIn("foo")
        createBuildFileIn("foo/bar")
        createBuildFileIn("baz/qux")
        def foo = Mock(ProjectDescriptor)
        def bar = Mock(ProjectDescriptor)
        def qux = Mock(ProjectDescriptor)

        when:
        workspace.projects {
            subproject("foo") {
                it.autodetect = true
            }
            directory("baz") {
                it.autodetect = true
            }
        }

        then:
        _ * settings.getRootProject() >> rootProject
        _ * settings.getRootDir() >> projectDir
        1 * settings.include(":foo")
        1 * settings.include(":foo:bar")
        1 * settings.include(":qux")

        and:
        1 * settings.project(":foo") >> foo
        1 * settings.project(":foo:bar") >> bar
        1 * settings.project(":qux") >> qux
    }

    def "does not autodetect projects when not configured a multi-project build"() {
        given:
        createBuildFileIn("foo")
        createBuildFileIn("foo/bar")
        createBuildFileIn("baz/qux")
        def foo = Mock(ProjectDescriptor)
        def bar = Mock(ProjectDescriptor)
        def baz = Mock(ProjectDescriptor)

        when:
        workspace.projects {
            subproject("foo") {
                autodetect = true
            }
            subproject("baz")
        }

        then:
        _ * settings.getRootProject() >> rootProject
        _ * settings.getRootDir() >> projectDir
        1 * settings.include(":foo")
        1 * settings.include(":foo:bar")
        1 * settings.include(":baz")
        0 * settings.include(":baz:qux")

        and:
        1 * settings.project(":foo") >> foo
        1 * settings.project(":foo:bar") >> bar
        1 * settings.project(":baz") >> baz
    }

    private void createBuildFileIn(String path) {
        Path subdir = projectDir.toPath().resolve(path)
        Files.createDirectories(subdir)
        Path buildFile = subdir.resolve("build.gradle.kts")
        Files.createFile(buildFile)
    }
}