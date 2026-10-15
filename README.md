# PocketBill — Invoice & Quote Maker

An offline-first Android app that helps freelancers create professional invoices, export them as PDFs, share them with clients, and track who has paid.

**Current status:** Phase 0, setting up the environment. No app code exists yet. See [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md).

## Project documents

| Document | Purpose |
|---|---|
| [PROJECT_STATUS.md](docs/PROJECT_STATUS.md) | Current phase, task status, exact next action, resume prompt |
| [ROADMAP.md](docs/ROADMAP.md) | All phases and task IDs |
| [ENVIRONMENT.md](docs/ENVIRONMENT.md) | What is installed on the development laptop |
| [DECISIONS.md](docs/DECISIONS.md) | Decision log |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | How the code is organized |
| [TEST_PLAN.md](docs/TEST_PLAN.md) | What is tested and how |
| [KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) | Open problems |
| [RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) | Google Play release steps |
| [MARKET_RESEARCH.md](docs/MARKET_RESEARCH.md) | Competitor and user research |

## Setting up your computer

These steps are for Windows 11. Do them once.

### 1. Fix JAVA_HOME
1. Press the **Start** key, type `environment`, and open **Edit environment variables for your account**.
2. Under **User variables**, select `JAVA_HOME` and click **Edit**.
3. Change the value from `C:\java\bin` to `C:\java`, then click **OK** twice.
4. Close and reopen any terminal windows.

Success looks like this: in a new PowerShell window, `& "$env:JAVA_HOME\bin\java.exe" -version` prints `java version "17.0.12"`.

### 2. Install Android Studio
1. Download it from the official page, https://developer.android.com/studio, and run the installer with the default options.
   Alternatively, in PowerShell, run `winget install --id Google.AndroidStudio -e`.
2. Open Android Studio. In the setup wizard, choose **Standard**. Read and accept the SDK licence agreements yourself, then let it download its components. This can take 20–40 minutes.
3. If it offers the **Android Emulator hypervisor driver**, allow it. This is needed for the emulator.

Success looks like this: the "Welcome to Android Studio" window appears, and the folder `C:\Users\DELL\AppData\Local\Android\Sdk` exists.

### 3. (Recommended) Prepare an Android phone for testing
1. On the phone, go to **Settings → About phone** and tap **Build number** 7 times. Developer options are now enabled.
2. Go to **Settings → System → Developer options** and turn on **USB debugging**. The exact menu names vary by phone brand.
3. Connect the phone by USB and tap **Allow** when it asks to trust this computer.

## Building the app

Instructions will be added in P0-T03, once the project exists.
