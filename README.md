# UE CompLab Monitoring System

Java (Swing) system for monitoring the computer laboratory. It has two apps in one project:

| App | Runs on | What it does |
|---|---|---|
| **Student kiosk** | Student laptop | Fullscreen, locked login after Windows starts. After login: saves **time in**, shows the **Workstation Check** (Monitor / Keyboard / Mouse + Other concerns), then unlocks the PC and shows a small session bar. **Log out** saves **time out** and locks the PC again. |
| **Admin console** | Admin laptop (also runs MySQL) | Live dashboard of every PC, session logs (replaces the paper logbook), PC issue reports (mark in progress / resolved), student accounts. Refreshes every 5 seconds. |

The login and Workstation Check screens are built from the Figma file *UE CompLab – Login (Modern)*.
The admin screens use the same colors, fonts and components.

```
[Student laptop]  ───── same Wi-Fi / phone hotspot ─────  [Admin laptop]
 Student kiosk (Java)                                      Admin console (Java)
                                                           MySQL  ← the only database
        └──────────── JDBC to admin-IP:3306 ─────────────────────▲
```

---

## 1. Open the project (both laptops, or just yours while developing)

1. Install **JDK 17 or newer** (e.g. Eclipse Temurin). Tick "Add to PATH" during install.
2. IntelliJ IDEA → **File › Open** → choose this folder (the one with `pom.xml`) → *Open as Project*.
3. Wait for Maven to finish downloading (needs internet **once** — it downloads the MySQL driver).
4. Two run configurations appear at the top right: **Student App** and **Admin App**.

While developing, `config.properties` has `kiosk.lock=false`, so the student app opens as a normal window you can close.

## 2. Set up the database (ADMIN laptop)

1. Install **XAMPP**, open the XAMPP Control Panel and **Start MySQL**.
2. Open `http://localhost/phpmyadmin` → **SQL** tab → paste everything from `schema.sql` → **Go**.
   This creates the `comlab` database, sample data and the `labuser` account the apps use.
3. Right-click `scripts/admin-open-firewall.bat` → **Run as administrator**. It opens port 3306 and prints this laptop's IP address. **Write the IP down.**
4. If XAMPP's MySQL refuses connections from the other laptop: XAMPP › MySQL **Config** › `my.ini`, make sure there is no `bind-address=127.0.0.1` line under `[mysqld]` (or change it to `0.0.0.0`), then restart MySQL.

Demo accounts (password for every student is `1234`):

| Who | Login | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Student | `20241120810` (Jay Lord Gaspi) | `1234` |
| Student | `20231234567` (Maria Santos) | `1234` |

## 3. Build the .jar

In IntelliJ: **Maven** panel (right side) › Lifecycle › **package**. This creates `target/comlab.jar` (everything inside, including the MySQL driver).

Make a folder on each laptop, e.g. `C:\CompLab\`, and copy into it:

- `target/comlab.jar`
- `config.properties`
- everything in `scripts/`

## 4. Set up the STUDENT laptop

1. Connect both laptops to the **same network**. A phone hotspot is safest; school Wi-Fi often blocks laptop-to-laptop traffic.
2. Edit `C:\CompLab\config.properties`:
   ```
   db.host=192.168.43.10      ← the admin laptop's IP from step 2.3
   pc.id=PC-14
   kiosk.lock=true
   ```
3. Test: double-click `start-student.bat`. The login should cover the whole screen.
   To close it: on the login screen press **Ctrl + Shift + F12**, type `letmeout` (change it in `kiosk.exitPassword`).
4. Make it start after Windows boots: double-click `install-autostart.bat`.
5. Optional but recommended for the panel: double-click `kiosk-lockdown.reg`, then sign out and back in.
   This removes Task Manager / Lock / Sign out from Ctrl+Alt+Del for this Windows user.
   Undo it any time with `kiosk-unlock.reg`.
6. To skip the Windows password screen at boot (so the kiosk appears straight after loading), enable automatic sign-in for the demo Windows account (`netplwiz` → untick "Users must enter a user name and password").

## 5. Start the ADMIN laptop

Double-click `start-admin.bat` (or run **Admin App** in IntelliJ), log in with `admin` / `admin123`.

## 6. Demo script for the panel (about 3 minutes)

1. Restart the student laptop → after Windows loads, the **locked login** appears. Show that Alt+F4 does nothing, and that Alt+Tab or the Windows key only let you away for a moment — the kiosk pulls itself back to the front within a quarter of a second.
2. On the admin dashboard, PC-14 shows **Available** (green).
3. Log in on the student laptop → Workstation Check appears. Admin: PC-14 turns **orange / In use** with the student's name within 5 s. **Sessions** shows the new time-in.
4. Tick Monitor and Keyboard, leave **Mouse** unticked, fill *Other concerns* (category, priority, description, photo) → **Start session**.
   Admin › **Reports**: the "Pre-check: Mouse not working" report and the concern appear. Open one and **Mark in progress**.
5. Student uses the PC; the floating bar shows the timer. Click **Report issue** to send another report during the session.
6. Student clicks **Log out** → the PC locks again. Admin › Sessions shows the **time out** and duration.
7. Bonus: log in again, then on the admin dashboard click PC-14 → **End session**. The student PC returns to the login screen within 10 s.
8. **Export CSV** in Sessions gives the lab log as a spreadsheet.

## 7. Troubleshooting (demo day)

| Message on the student laptop | Fix |
|---|---|
| *Can't reach the lab server at …* | Same hotspot? MySQL started? Firewall script run? `ping <admin-ip>` from the student laptop. Check `db.host`. |
| *MySQL on the admin laptop does not allow remote logins* | Run the `CREATE USER` / `GRANT` / `FLUSH` lines at the end of `schema.sql` again. |
| *Database 'comlab' not found* | Run `schema.sql` on the admin laptop. |
| *The MySQL driver is missing* | Run the built `comlab.jar`, or reload Maven in IntelliJ. |
| Kiosk won't close | Ctrl+Shift+F12 works on the **login screen** (log out first if a session is running). |
| IP changed | Edit `db.host` in `config.properties` and restart the kiosk — no rebuild needed. |

## 8. How the code is organized

```
src/main/java/ph/ue/comlab/
├── Launcher.java            entry point: "student" or "admin"
├── AppConfig.java           reads config.properties
├── model/Models.java        Student, Admin, LabSession, Report, PcStatus, SessionRow, Stats
├── db/                      all SQL lives here (JDBC)
│   ├── Database.java        connection + friendly error messages + SHA-256
│   ├── StudentDao.java      login, admin login, add student, reset password
│   ├── SessionDao.java      time in / time out, equipment check, session log
│   ├── ReportDao.java       create / list / update reports, photos
│   └── PcDao.java           heartbeat (online status), dashboard data
├── ui/                      design system from the Figma file
│   ├── Theme.java           colors + fonts (tokens from Figma)
│   ├── Flex.java            auto-layout (row/column/gap/padding) like Figma
│   ├── UI.java              Input Field, Button/Primary, Button/Secondary, pills, cards…
│   ├── Icons.java           line icons
│   ├── LoginPanel.java      Figma "Login – Desktop"
│   ├── TopNav.java          Figma "Top nav"
│   ├── ConcernForm.java     Figma "Other concerns"
│   └── DataTable.java       styled table for the admin pages
├── student/
│   ├── StudentApp.java      the flow: login → check → use → logout
│   ├── KioskGuard.java      fullscreen lock (no close, no Alt+Tab escape)
│   ├── WorkstationCheckPanel.java   Figma "Workstation Check – Desktop"
│   ├── SessionBar.java      floating bar while the PC is in use
│   └── ReportDialog.java    "Report issue" during the session
└── admin/
    ├── AdminApp.java        login, sidebar, 5-second refresh
    ├── DashboardPage.java   stats + live PC map + latest reports
    ├── SessionsPage.java    lab log, end session, export CSV
    ├── ReportsPage.java     reports + detail panel + photo
    └── StudentsPage.java    accounts
```

Database tables (for the ERD): `students`, `admins`, `pcs`, `sessions`, `equipment_checks`, `reports`. See `schema.sql`.

## 9. Customizing

- **Real UE logo:** save it as `src/main/resources/icons/ue-logo.png` (square).
- **Building photo on the login hero:** save it as `src/main/resources/icons/hero.jpg`.
- **Exact Figma icons:** export an icon from Figma as PNG @2x and save it as `src/main/resources/icons/<name>.png` (names: `idcard`, `lock`, `eye`, `help`, `monitor`, `keyboard`, `mouse`, `upload`, `send`, `logout`, `clock`, `alert`, `check`, `arrow-right`, `chevron-down`). Any PNG found there replaces the built-in drawing.
- **Colors / fonts:** `ui/Theme.java`.

## 10. Limits to mention honestly

- No normal program can block **Ctrl+Alt+Del** — Windows reserves it. The kiosk handles everything else in Java, and `kiosk-lockdown.reg` removes the escape options from the Ctrl+Alt+Del screen. If the app is ever killed, `start-student.bat` relaunches it in about one second.
- The student and admin laptops should have the same time zone (both Asia/Manila). All times are taken from the MySQL server clock, so the two laptops' clocks don't need to match.

## Documentation

| Folder | Contents |
|---|---|
| `docs/diagrams/` | System flowchart, ERD and sequence diagram (`.drawio` + `.png`), class diagrams (`.puml` + `.png`) |
| `docs/presentation/` | `UE-CompLab-Proposal.pptx` (proposal) and `UE-CompLab-Presentation.pptx` (final defense) |

Open `.drawio` files at [app.diagrams.net](https://app.diagrams.net) and `.puml` files at [plantuml.com](https://www.plantuml.com/plantuml).

## Team

- Dela Cruz, Justin — Project Manager, Developer
- Gaspi, Jay Lord F. — UI/UX Designer, Developer
- Timbol, Alexandria T. — UI/UX Designer, Documentation

## Screen size

Every window opens at **1920×1080** (maximized) and is drawn at real screen pixels, so the layout looks the same on every laptop regardless of Windows display scaling. To make everything larger, add `ui.scale=1.25` to `config.properties`.

## Windows apps (.exe)

Build **ComLab Student.exe** and **ComLab Admin.exe** (Java bundled inside, so target PCs don't need Java):

1. IntelliJ: **Maven** panel, then **comlab-monitor → Lifecycle → package** (creates `target\comlab.jar`).
2. Double-click **`scripts\build-app.bat`**. It creates:
   - `dist\ComLab Student\` — copy to the student laptop / lab PCs
   - `dist\ComLab Admin\` — copy to the admin laptop
3. Edit `config.properties` **next to the .exe** (e.g. `db.host` on the student laptop).
4. Student laptop kiosk: in the `ComLab Student` folder run `install-autostart.bat` (it uses `start-student.bat`, which keeps the .exe running).
