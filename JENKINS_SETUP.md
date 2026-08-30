# Jenkins setup

How to build this CI from nothing. Two jobs and one credential:

| Job | Type | Purpose |
| --- | --- | --- |
| `next_login_javatestcase` | Freestyle | The normal suite. Reports failures to xpath_healer. |
| `heal-verify` | Pipeline | The RED/GREEN builds xpath_healer triggers to prove a heal. |

Captured from a working Jenkins **2.568.2** on macOS.

---

## 0. Prerequisites

On the Jenkins machine:

```bash
brew install openjdk@21 maven python@3 git
# Google Chrome must also be installed - Selenium Manager fetches the matching
# chromedriver by itself, so there is nothing else to install for the browser.
java -version && mvn -v && python3 --version
```

Plugins (Manage Jenkins → Plugins). Versions are what this was built against; newer is fine:

| Plugin | Version | Needed for |
| --- | --- | --- |
| Git | 5.10.1 | cloning the repo |
| JUnit | 1424 | parsing surefire reports |
| Timestamper | 1.30 | timestamps in console output |
| Build Timeout | 1.41 | the 15-minute cap |
| Pipeline (workflow-aggregator) | 608 | the `heal-verify` job |
| Plain Credentials | 199 | the webhook secret |

---

## 1. The credential

`Jenkinsfile.heal-verify` looks this up by ID, and the build fails at startup if it is missing.

1. **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**
2. Kind: **Secret text** (it defaults to "Username with password" — change it)
3. **Secret**: the `JENKINS_WEBHOOK_SECRET` value from xpath_healer's `.env.local`
4. **ID**: `xpath-healer-secret` — exactly this, it is looked up by name
5. Description: anything. **Create**.

> The ID field is sometimes hidden behind an **Advanced** button.

---

## 2. Job 1 — `next_login_javatestcase`

**New Item → Freestyle project → name it `next_login_javatestcase`**

### Parameters

Tick **This project is parameterised**, then add four:

| Kind | Name | Default | Notes |
| --- | --- | --- | --- |
| String | `BASE_URL` | `http://localhost:3000` | where next_login is served |
| Boolean | `HEADLESS` | ticked | untick only to watch the browser |
| String | `XPATH_HEALER_URL` | `http://localhost:3002/api/v1/webhooks/jenkins` | blank disables reporting |
| Password | `XPATH_HEALER_SECRET` | the webhook secret | masked in the console |

### Source Code Management

- **Git**, Repository URL `https://github.com/yajay0411/next_login_javatestcase.git`
- Branch specifier: `*/main`
- No credentials needed — the repo is public.

### Build Environment

- ☑ **Add timestamps to the Console Output**
- ☑ **Abort the build if it's stuck** → Absolute → **15** minutes

### Build Steps → Execute shell

```sh
# No errexit: Maven is expected to fail sometimes, and we must still report it.
set -u

# A Jenkins shell on macOS does not inherit the login PATH, so Homebrew tools are invisible.
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
if [ -d /opt/homebrew/opt/openjdk@21 ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "Target: $BASE_URL   headless=$HEADLESS"
java -version
mvn -B -v | head -1

# Selenium Manager downloads the chromedriver matching the installed Chrome.
rc=0
mvn -B clean test -DbaseUrl="$BASE_URL" -Dheadless="$HEADLESS" || rc=$?

# Only failures are reported: a green build has nothing to heal.
if [ "$rc" -ne 0 ]; then
  BUILD_RESULT=FAILURE
  export BUILD_RESULT XPATH_HEALER_URL XPATH_HEALER_SECRET
  python3 ci/notify_xpath_healer.py || echo "[notify] reporter itself failed; ignoring"
fi

# The build's own verdict is Maven's, never the reporter's.
exit $rc
```

**The PATH block is not decoration.** Without it `mvn: command not found`, the suite runs zero
tests, and the build reports success — indistinguishable from a real pass.

### Post-build Actions

- **Publish JUnit test result report** → `target/surefire-reports/TEST-*.xml`

### Discard old builds

General → ☑ **Discard old builds** → Max # of builds to keep: **20**

---

## 3. Job 2 — `heal-verify`

xpath_healer triggers this twice per heal: `PHASE=red` on the failing commit (must reproduce
the failure) and `PHASE=green` on the heal branch (must pass the whole suite).

**New Item → Pipeline → name it `heal-verify`**

### Parameters — add these BEFORE the first build

| Kind | Name | Default |
| --- | --- | --- |
| String | `GIT_REF` | *(empty)* |
| String | `HEAL_RUN_ID` | *(empty)* |
| Choice | `PHASE` | `red` then `green`, one per line |
| String | `REPO` | `yajay0411/next_login_javatestcase` |

> **Do not skip this.** A declarative pipeline's own `parameters {}` block does not register
> until the job has run once, and xpath_healer calls `buildWithParameters` on a job that has
> never built. Without them the trigger fails with **HTTP 400** and the heal dies at GATE 3.

### Pipeline

- Definition: **Pipeline script from SCM**
- SCM: **Git**, URL `https://github.com/yajay0411/next_login_javatestcase.git`, branch `*/main`
- **Script Path**: `Jenkinsfile.heal-verify`

The pipeline checks out `GIT_REF` itself, so the branch above only selects which copy of the
Jenkinsfile to read.

### Discard old builds

Max # of builds to keep: **30**

---

## 4. Verify it

```bash
# 1. Start what the tests drive
cd ../next_login && npm run dev          # http://localhost:3000

# 2. Start the healer
cd ../xpath_healer && npm run inngest &  # durable workflow server
npm run dev                              # http://localhost:3002

# 3. Build
open http://localhost:8080/job/next_login_javatestcase/
# "Build with Parameters" -> Build
```

A green suite ends there — nothing is reported, by design. To exercise the healer, break a
locator in `src/test/java/com/nextlogin/pages/LoginPage.java` (a stale `id` is realistic),
push, and build again. The console should end with:

```
[notify] FAILURE: N failed of 17, N DOM(s) attached -> http://localhost:3002/...
[notify] 202 {"ok":true,...,"published":N}
```

Then watch <http://localhost:3002/heals>.

---

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| `mvn: command not found`, 0 tests, build "passes" | the PATH block is missing from the shell step |
| Trigger returns **400** | `heal-verify` has no parameters — add them in §3 |
| Trigger returns **404** | job name does not match `JENKINS_VERIFY_JOB` in xpath_healer |
| Verify build fails instantly on `credentials(...)` | the `xpath-healer-secret` credential is missing or misnamed |
| `[notify] could not deliver: nodename nor servname provided` | `XPATH_HEALER_URL` points at `host.docker.internal`; use `localhost` on a native Jenkins |
| Webhook returns **401** | `XPATH_HEALER_SECRET` ≠ `JENKINS_WEBHOOK_SECRET` in the healer |
| Heal stops at `skipped / no_dom_captured` | `FailureCaptureListener` not registered in `testng.xml` |
| Console frozen at `Running TestSuite` | normal — surefire buffers, and each broken locator costs a 30s wait |

## API equivalents

Anything above can be done over REST with a user + API token
(**People → you → Security → API Token**):

```bash
J=http://localhost:8080; A='user:token'
CRUMB=$(curl -sg -u "$A" "$J/crumbIssuer/api/json" \
  | python3 -c "import json,sys;d=json.load(sys.stdin);print(d['crumbRequestField']+':'+d['crumb'])")

curl -sg -u "$A" "$J/job/next_login_javatestcase/config.xml" -o job.xml   # export
curl -sg -u "$A" -H "$CRUMB" -H 'Content-Type: application/xml' \
  --data-binary @job.xml "$J/createItem?name=my-copy"                     # import

curl -sg -u "$A" -H "$CRUMB" -X POST \
  "$J/job/next_login_javatestcase/buildWithParameters"                    # build
```

Note `curl -g` throughout: without it the shell expands the `[...]` in Jenkins' `tree=`
queries and the request silently malforms.
