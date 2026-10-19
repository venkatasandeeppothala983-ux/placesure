# Third-party components

Runtime dependencies are declared with exact versions in pom.xml; their licenses remain in the shaded JAR's META-INF resources.

- OpenJDK/JDK HTTP server: runtime provided by the installed JDK; not redistributed as part of this project.
- Jackson Databind/Core/Annotations 2.18.3: Apache License 2.0.
- MySQL Connector/J 8.4.0: GPLv2 with Universal FOSS Exception; see the upstream connector distribution for its licensing terms and applicable exception.
- Chart.js 4.4.8: MIT; license bundled at frontend/js/Chart-LICENSE.md.
- Inter fonts: SIL Open Font License 1.1; license bundled at frontend/assets/Inter-LICENSE.txt.
- JUnit Jupiter 5.11.4: Eclipse Public License 2.0 (test only).
- Playwright: Apache License 2.0 (optional test tooling; no browser binaries are redistributed in the project archive).

Original project UI, inline icons, Java application code, sample data, and documentation were authored for this PlaceSure project. No real student records or external company logos are included. Before redistribution or commercial deployment, review all upstream licenses, particularly database-driver distribution requirements.
