// =============================================================================
// PhotoConnect — root Jenkinsfile (declarative pipeline)
// =============================================================================
// Discovered by the "PhotoConnect" multibranch job (see ci/jenkins/casc.yaml).
// Jenkins finds this file at the repo root of every branch and runs it.
//
// Stages:
//   1. Build   — compile + package all 7 Maven modules, skip tests (fast fail
//                on compilation errors before we spend time on tests).
//   2. Test    — `mvn verify` runs unit tests (surefire) AND integration tests
//                (failsafe + Testcontainers).
//
// Always-run post-steps publish JUnit XML and JaCoCo coverage to the build page.
//
// Agent:
//   Ephemeral `maven:3.9-eclipse-temurin-21` container (matches our JDK 21
//   baseline). Spawned per build, torn down after. Two important mounts:
//     - the host's Docker socket — so Testcontainers can spin up its own
//       sibling containers (postgres, redis) on the host daemon
//     - a named volume for ~/.m2 — so we don't re-download Maven deps every
//       single build (otherwise builds are ~5min slower than they need to be)
// =============================================================================

pipeline {

    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21'
            // Args passed to `docker run`:
            //   -u root:root        run as root so we can touch /var/run/docker.sock
            //   -v docker.sock      Testcontainers needs the HOST daemon
            //   -v jenkins-m2       persistent Maven cache (named volume — Docker
            //                       auto-creates it on first use)
            args '-u root:root ' +
                 '-v /var/run/docker.sock:/var/run/docker.sock ' +
                 '-v jenkins-m2:/root/.m2'
        }
    }

    options {
        // Prefix every log line with a timestamp (timestamper plugin).
        timestamps()
        // Hard ceiling — a runaway build dies after 30 min instead of forever.
        timeout(time: 30, unit: 'MINUTES')
        // Keep only the last 20 builds. Older logs/artifacts get GC'd.
        buildDiscarder(logRotator(numToKeepStr: '20'))
        // No parallel builds of the same branch — they'd fight over the Maven
        // cache and over Testcontainers' fixed ports.
        disableConcurrentBuilds()
    }

    environment {
        //   -B / --batch-mode  : no interactive prompts (CI isn't a TTY)
        //   -ntp               : no "Downloading from central" progress spam
        MAVEN_CLI_OPTS = '-B -ntp'
    }

    stages {

        stage('Build') {
            steps {
                echo 'Compiling all modules, skipping tests for speed...'
                sh "mvn ${MAVEN_CLI_OPTS} clean package -DskipTests"
            }
        }

        stage('Test') {
            steps {
                echo 'Running unit + integration (Testcontainers) tests...'
                // -fae = "fail at end": keep building OTHER modules even if one
                // module's tests fail, so the report shows every failure, not
                // just the first encountered.
                sh "mvn ${MAVEN_CLI_OPTS} verify -fae"
            }
        }
    }

    post {
        always {
            // JUnit picks up BOTH unit (surefire) and integration (failsafe) XML.
            // allowEmptyResults so a pure-compile failure doesn't compound by
            // also failing here with "no test reports found".
            junit testResults: '**/target/surefire-reports/*.xml,**/target/failsafe-reports/*.xml',
                  allowEmptyResults: true

            // JaCoCo coverage report. Only modules that activate the
            // jacoco-maven-plugin produce a jacoco.exec — currently
            // auth-service and photographer-service. Others contribute nothing
            // and that's fine.
            jacoco execPattern: '**/target/jacoco.exec',
                   classPattern: '**/target/classes',
                   sourcePattern: '**/src/main/java',
                   exclusionPattern: '**/*Application.class,**/dto/**,**/config/**'

            // Wipe the workspace. The agent container is destroyed anyway, but
            // this clears the workspace dir on the controller's volume.
            cleanWs()
        }
        success {
            echo "Build #${BUILD_NUMBER} green."
        }
        failure {
            echo "Build #${BUILD_NUMBER} red. Open the Tests tab for the failing test, or scroll the console log."
        }
    }
}
