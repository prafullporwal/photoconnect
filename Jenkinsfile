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
//   3. Deploy  — `docker compose --profile apps up -d --build` against the
//                root compose file. Brings up discovery, config, auth, gateway
//                plus all infra deps. Single shared "photoconnect" project so
//                each build replaces the previous deployment in place.
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

        stage('Deploy') {
            // The maven:3.9-eclipse-temurin-21 agent has Maven + JDK + apt but
            // no docker CLI. We install it on-the-fly from Docker's official
            // apt repo (~20s the first time, ~2s subsequent if apt cache hits).
            // TODO: bake docker-ce-cli + docker-compose-plugin into a custom
            // ci/jenkins/agent.Dockerfile to skip this step entirely.
            steps {
                echo 'Step 1/3 — installing docker CLI + compose plugin into the agent...'
                sh '''
                    if ! command -v docker >/dev/null 2>&1; then
                        # The maven:3.9-eclipse-temurin-21 image is Ubuntu-based,
                        # but switching to a Debian-based JDK image later would
                        # also work. Read the distro id from /etc/os-release so
                        # we point at the right Docker apt repo either way.
                        . /etc/os-release
                        DISTRO_ID="${ID}"            # "ubuntu" or "debian"
                        CODENAME="${VERSION_CODENAME}"
                        ARCH=$(dpkg --print-architecture)

                        apt-get update -qq
                        apt-get install -qq -y --no-install-recommends \\
                            ca-certificates curl gnupg
                        install -m 0755 -d /etc/apt/keyrings
                        curl -fsSL "https://download.docker.com/linux/${DISTRO_ID}/gpg" \\
                            | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
                        chmod a+r /etc/apt/keyrings/docker.gpg
                        echo "deb [arch=${ARCH} signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/${DISTRO_ID} ${CODENAME} stable" \\
                            > /etc/apt/sources.list.d/docker.list
                        apt-get update -qq
                        apt-get install -qq -y --no-install-recommends \\
                            docker-ce-cli docker-compose-plugin
                    fi
                    docker --version
                    docker compose version
                '''

                // Ephemeral RSA keys for JWT signing. Real keys are gitignored
                // (CVE: never commit secrets) so the GitHub clone has none.
                // We mint a fresh pair every deploy. Tokens issued by build N
                // will NOT verify after deploy N+1 — that's the expected tradeoff
                // for local CI; in prod the keys come from AWS Secrets Manager.
                echo 'Step 2/3 — generating ephemeral RSA keys for JWT signing...'
                sh '''
                    mkdir -p auth-service/keys
                    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \\
                        -quiet -out auth-service/keys/private_key.pem
                    openssl rsa -in auth-service/keys/private_key.pem -pubout \\
                        -out auth-service/keys/public_key.pem 2>/dev/null
                    chmod 644 auth-service/keys/*.pem
                '''

                // -p photoconnect: fixed project name so each build replaces
                //   the previous deploy in place (otherwise compose would
                //   derive the project name from the workspace dir, which
                //   Jenkins randomises per branch).
                // --profile apps: opts in to the 4 Java services (gated in
                //   docker-compose.yml so plain `docker compose up` still only
                //   runs the infra deps).
                // --build: rebuild images from the current source on every
                //   deploy. Slow (~3-5 min first time, ~1-2 min subsequent)
                //   but guarantees the running containers match HEAD.
                echo 'Step 3/3 — docker compose up...'
                sh 'docker compose -p photoconnect --profile apps up -d --build'

                echo '''
====================================================================
PhotoConnect deployed locally. Hit these from your host browser:

  api-gateway        http://localhost:8080            (edge — most traffic goes here)
  auth-service       http://localhost:8081/actuator/health
  discovery-service  http://localhost:8761            (Eureka dashboard)
  config-service     http://localhost:8888/actuator/health

Tail logs:    docker compose -p photoconnect --profile apps logs -f
Stop:         docker compose -p photoconnect --profile apps down
====================================================================
'''
            }
        }
    }

    post {
        always {
            // Maven runs in this agent as root (see args '-u root:root') but the
            // Jenkins controller runs as jenkins (UID 1000). Without this chown,
            // the controller can't chmod or delete root-owned files on the next
            // build's checkout — it fails with "Operation not permitted" and
            // aborts the whole build before any stage runs. Chowning ensures the
            // workspace (including the auth-service/keys we deliberately keep
            // via the EXCLUDE below) is cleanable by UID 1000 next time around.
            sh 'chown -R 1000:1000 . 2>/dev/null || true'

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

            // Wipe most of the workspace, but PRESERVE auth-service/keys so
            // the running app containers' bind-mount target stays valid even
            // after the build ends. Recreating a container with a missing
            // mount source would fail. Keys are tiny and overwritten every
            // deploy anyway.
            // notFailBuild: occasionally a built JAR is still in the kernel's
            // file cache when cleanWs runs and a single file's delete fails.
            // Don't flip the build red over that — the next build's checkout
            // overwrites the workspace anyway.
            cleanWs(notFailBuild: true,
                    patterns: [[pattern: 'auth-service/keys/**', type: 'EXCLUDE']])
        }
        success {
            echo "Build #${BUILD_NUMBER} green."
        }
        failure {
            echo "Build #${BUILD_NUMBER} red. Open the Tests tab for the failing test, or scroll the console log."
        }
    }
}
