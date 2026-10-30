# Contributing

When contributing to this repository, please first discuss the change you wish
to make via [issue](https://github.com/Despical/PixelPainter/issues/new)
with the owner of this repository before making a change.

Please note we have a code of conduct, please follow it in all your interactions
with the project.

## Development Setup

Use Java 25 and the checked-in Gradle wrapper:

```text
./gradlew clean build javadocJar --console=plain
```

On Windows, use `gradlew.bat`. The build runs the JUnit suite, produces the
shaded plugin JAR, and generates Javadocs when requested. See
[README.md](README.md) for commands, configuration and development requirements.

Follow `.editorconfig`: four spaces for Java, two for YAML, UTF-8 and LF endings.
Document classes, public methods, records and package responsibilities in English.
Use `@author Despical` and the existing creation-date style for new types.
Describe thread ownership, validation, persistence and cancellation where relevant.
Run `git diff --check` before submitting.

Keep Bukkit world access on the server thread. Image work and file I/O belong
on the bounded workers. Changes to journals must preserve interrupted-operation
recovery and must never silently discard existing records. Extend tests when a
change affects those contracts. Preserve the GPLv3-or-later license headers.

Report vulnerabilities privately using [SECURITY.md](SECURITY.md).

## Pull Request Process

If you want to help this project and don't know where to start, check out the
[currently opened issues](https://github.com/Despical/PixelPainter/issues)
before creating any PRs. Because there are people that could use your help.

* Ensure you didn't use tabs! Please use spaces for indentation.
* Respect the code style.
* Do not increase the version numbers in any example files and the README.md to
  the new version that this Pull Request would represent.
* Create minimal diffs - disable on save actions like reformat source code or
  organize imports. If you feel the source code should be reformatted create a
  separate PR for this change.

## Issue Process

When you submit an issue, there will be shown a template for that. Please do not
discard except if you aren't reporting a bug, but rather suggesting code
improvements, pointing out spelling mistakes etc, you can discard the template.

* Ensure your issue is not a duplicate of any other.
* Please keep the issues section for actual bugs or code problems, not for support.
* Make sure you're using the latest version, chances are that the problem has
  already been solved.
* Don't post your issue in other issues, please keep everything nicely organized
  and make a separate one.

## Additional Resources

* [General GitHub documentation](https://help.github.com/)
* [GitHub pull request documentation](https://help.github.com/articles/creating-a-pull-request/)
