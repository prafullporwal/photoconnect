# =============================================================================
# PhotoConnect Jenkins controller image
# =============================================================================
# Extends the official Jenkins LTS image (JDK 21 variant — matches our
# Spring Boot 3.4 / Java 21 baseline) with three additions:
#
#   1. Docker CLI         — so pipelines can talk to the host Docker daemon
#                           (mounted as a volume in docker-compose.ci.yml).
#                           Needed for `agent { docker { ... } }` syntax.
#   2. Plugins, baked in  — installed at IMAGE BUILD TIME from plugins.txt.
#                           No clicking through the UI to install things.
#   3. JCasC config, baked in — boots fully configured: admin user, job,
#                                security settings, all from casc.yaml.
#
# Rebuild flow:
#   docker compose -f docker-compose.ci.yml build jenkins
#   docker compose -f docker-compose.ci.yml up -d
# =============================================================================

FROM jenkins/jenkins:lts-jdk21

# ---- Docker CLI -------------------------------------------------------------
# We install only the CLI (not the daemon) — we'll talk to the host's daemon
# via the mounted socket. Switch to root for apt, then back to the jenkins
# user (the image's default) before doing anything else.

USER root

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        ca-certificates curl gnupg lsb-release \
 && install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg \
        | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
 && chmod a+r /etc/apt/keyrings/docker.gpg \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" \
        > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli \
 && rm -rf /var/lib/apt/lists/*

# ---- Git safe.directory ----
# Modern git refuses to operate on a repo whose .git ownership doesn't match
# the current user (CVE-2022-24765). Our bind-mounted repo at
# /workspace/photoconnect comes in with host-side ownership that doesn't
# match the jenkins user inside the container, so the multibranch scan fails
# with "fatal: detected dubious ownership". Writing this into /etc/gitconfig
# (--system) lives in the IMAGE, so it survives volume resets.
RUN git config --system --add safe.directory '*'

USER jenkins

# ---- Plugins ----------------------------------------------------------------
# jenkins-plugin-cli (ships with the base image) reads a flat file of plugin
# IDs and installs them with all transitive deps. Doing this here means a
# fresh `docker compose up` always boots with the SAME plugin set — no drift.

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

# ---- JCasC config -----------------------------------------------------------
# The CASC_JENKINS_CONFIG env var (set in docker-compose.ci.yml) points here.
# Anything you want Jenkins to know on startup goes in casc.yaml.

COPY casc.yaml /usr/share/jenkins/ref/casc.yaml
