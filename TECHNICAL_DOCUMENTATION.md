# OFFICIAL FINAL PROJECT TECHNICAL DOCUMENTATION
## Computer Shop Management System

Technical System Manual, Deployment Guide & Defense Framework

Course: ITE-298 Information Management (Database Systems 1 and 2)

Submitted by:
[Team member names to be filled in]

---

## 1. System Overview

### 1.1 Primary Purpose

The Computer Shop Management System is a counter-controlled PC rental management system for computer shops. It replaces the pisonet model, in which each PC is governed by a physical coin-operated timer box that is easy to tamper with, produces no usable records, and gives the owner no central view of the shop. It also replaces manual tracking methods such as paper logs and mental computation of time and charges. In place of coin boxes and paperwork, the system provides a central counter dashboard showing every PC at a glance, live countdown timers on each customer PC with automatic reminders, billing computed automatically from hours multiplied by the hourly rate, and two-way communication between the counter and customer PCs through popup notices and broadcast messages.

### 1.2 Tech Stack Architecture

- **Frontend (Client Interface): Java Swing** — JFrame, JPanel, JButton, JLabel, JTextField, JComboBox, JOptionPane. All three windows (Login, Dashboard, CustomerTimer) were laid out with the NetBeans Design view, which generates the `initComponents()` GUI code, with hand-written runtime code layered on top for styling, window sizing behavior, and layout overrides.
- **Backend / Application Logic: Java 26** — Four classes. `Login` presents the login window, validates credentials against the database, and opens the correct next window based on the account role. `connectDB` is a single shared helper that opens MySQL connections for the whole program. `Dashboard` is the counter window: it manages sessions (Start, Extend, Stop), refreshes all PC cards and the sales total every second, auto-stops expired sessions, and sends broadcast messages. `CustomerTimer` is the customer window: it resolves its PC identity from the login name, polls its active session every second, renders the live countdown with reminder popups, and displays counter broadcasts.
- **Database Management System: MySQL via XAMPP.** The schema is in Third Normal Form (3NF) with primary keys on every table and a foreign key from sessions to PCs.
- **JDBC Connector: mysql-connector-j-9.4.0.** The driver JAR is stored in the project's `lib` folder and placed on the compile and run classpath.
- **Local Development Environment: XAMPP Stack (Apache/MySQL/phpMyAdmin) on localhost.** Only the MySQL engine and the phpMyAdmin interface are used by this system. The database runs on the same machine as the application.

### 1.3 Key System Features

- **Role-Based Login** — Two roles exist: counter staff (admin) and customer PCs (pc1, pc2, pc3). Credentials are verified against the `login_table`. The account's `role` value decides which window opens: staff accounts open the counter dashboard, customer accounts open the customer timer, whose PC identity is then resolved by matching the username to the PC name.
- **Live Counter Dashboard** — Shows every PC as a card with its current status (AVAILABLE or IN USE) and a live countdown, refreshed from the database every 1 second.
- **Session Management (Start / Extend / Stop)** — Three actions manage the `sessions` table. Start creates a new session row, Extend pushes that row's `end_time` forward and grows its amount, and Stop marks the session done and archived.
- **Real-Time Customer Countdown** — Each customer PC shows a live countdown refreshed every 1 second, with automatic popups at 30 minutes remaining, 5 minutes remaining, and time's up.
- **Automatic Session Expiry (Auto-Stop)** — The dashboard's 1-second refresh detects sessions whose end time has passed and marks them done automatically, freeing the PC without any click.
- **Broadcast Messaging** — The counter can send one message to every active customer PC at once. This uses a flag column on the `pcs` table that each customer screen reads on its own tick and clears itself after showing.
- **Soft Deletion / Record Archiving** — Sessions are never deleted. When a session ends, its status changes to `'done'` and `is_archived` becomes 1. Rows remain in the database for record-keeping and reporting.

---

## 2. Database Design & Schema Normalization (3NF)

Database name: `java_database` (MySQL via XAMPP, accessed at `localhost:3306`).

### 2.1 Schema Entities & Attributes

- **login_table** — Governs system login only.
  - `id` — INT AUTO_INCREMENT — PK (Primary Key)
  - `username` — VARCHAR(50) UNIQUE NOT NULL — attribute (unique login name)
  - `password` — VARCHAR(100) NOT NULL — attribute (stored as plain text)
  - `role` — VARCHAR(20) NOT NULL DEFAULT 'counter' — attribute (`counter` opens the dashboard, `customer` opens the PC timer)
- **pcs** — The three shop computers.
  - `id` — INT AUTO_INCREMENT — PK (Primary Key)
  - `pc_name` — VARCHAR(20) UNIQUE NOT NULL — attribute (PC1, PC2, PC3)
  - `hourly_rate` — DECIMAL(10,2) NOT NULL DEFAULT 25.00 — attribute (reserved per-PC rate; the running code currently applies one shared rate constant instead — see Section 5, Rate Handling)
  - `broadcast_pending` — TINYINT(1) NOT NULL DEFAULT 0 — attribute (0 = nothing waiting, 1 = broadcast waiting for this PC)
  - `broadcast_message` — VARCHAR(255) NOT NULL DEFAULT '' — attribute (the waiting broadcast text)
- **sessions** — One row per rental session, written by the counter.
  - `id` — INT AUTO_INCREMENT — PK (Primary Key)
  - `pc_id` — INT NOT NULL — FK (Foreign Key, references `pcs.id`)
  - `hours` — INT NOT NULL — attribute (purchased hours, 1–6)
  - `amount` — DECIMAL(10,2) NOT NULL — attribute (billed cost, frozen at sale time)
  - `start_time` — DATETIME NOT NULL — attribute (set from the database clock)
  - `end_time` — DATETIME NOT NULL — attribute (set from the database clock)
  - `status` — VARCHAR(20) NOT NULL DEFAULT 'active' — attribute (`active` or `done`)
  - `is_archived` — TINYINT NOT NULL DEFAULT 0 — attribute (0 = live, 1 = archived)

### 2.2 Normalization Breakdown (1NF to 3NF)

- **First Normal Form (1NF):** Every column holds atomic (single-valued) data — one fact per cell, no lists or repeating groups. Every table has a designated primary key (`id`).
- **Second Normal Form (2NF):** Meets 1NF. Every non-key attribute depends on the whole primary key. All primary keys are single-column surrogate ids, so no partial dependency on part of a key is possible; each session's hours, amount, times, and status describe that session row and nothing else.
- **Third Normal Form (3NF):** Meets 2NF. No transitive dependencies — no attribute depends on another non-key attribute. Concretely: the hourly price is stored exactly once per PC in `pcs.hourly_rate` rather than repeated inside every `sessions` row, and PC names live only in `pcs.pc_name` rather than being copied into `sessions`. (The `amount` stored per session is not a violation: it is the frozen historical charge, computed as hours multiplied by the rate at sale time, so old bills stay correct even if the rate ever changes.)

### 2.3 Crow's Foot ERD Cardinality & Relationships

- **pcs to sessions (One-to-Many / 1:M):** One PC can have zero or many session records over time (0..M). Each session record belongs to one and only one PC (1..1), enforced by `sessions.pc_id` referencing `pcs.id`.
- **login_table:** Standalone entity with no foreign-key relationship to any other table; it governs system login only.

*Figure 1. Crow's Foot ERD — Computer Shop Management System schema (PK/FK and 1:M cardinality between pcs and sessions; login_table standalone). The reader is expected to paste the actual ERD image here separately.*

---

## 3. Logic Specifications (Step-by-Step Data Flow)

### 3.1 Feature: Counter Login

- **Client Form Submission (UI):** Staff types a username and password into the two fields of the Login window and clicks the Login button (or presses Enter, which clicks it). Empty input is rejected immediately with a prompt and never reaches the database.
- **Server-Side Validation:** The username is trimmed of surrounding spaces. The password is read from the password field and converted to a comparable string.
- **Database Execution (SQL):**

```sql
SELECT * FROM login_table WHERE username = ? AND password = ?
```

Both values are bound to `?` placeholders through a `PreparedStatement`, so typed input can never execute as SQL code. If the `role` column is absent (strict instruction-sheet tables), the code defaults the role to counter.

- **Feedback & Redirection:** No matching row produces "Invalid username or password!". A match produces "Login Successful!", then opens the counter `Dashboard` for staff roles or the customer `CustomerTimer` (carrying the username) for customer roles, and closes the Login window. A dead database connection produces "Database Error: cannot connect to java_database. Check XAMPP." instead of a crash.

### 3.2 Feature: Starting a Session

Staff selects 1–6 hours on a PC card and clicks Start. The code refuses busy PCs ("already in use") and unknown PCs before touching the database, computes `amount = hours × 25.00`, then executes:

```sql
INSERT INTO sessions (pc_id, hours, amount, start_time, end_time, status, is_archived)
VALUES (?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? HOUR), 'active', 0)
```

Start and end times come from the database clock (`NOW()`), never the Java clock, so counter and customer screens always agree. Feedback is a confirmation popup naming the PC, hours, and charge (for example, "PC1 started: 2 hr (P50.00)."), and the dashboard repaints immediately. This INSERT is the CREATE operation of the system.

### 3.3 Feature: Extending a Session

Staff picks additional hours on an in-use PC and clicks Extend. The code looks up that PC's live session id from its in-memory map and refuses PCs with no active session. It then executes:

```sql
UPDATE sessions SET hours = hours + ?, amount = amount + ?, end_time = DATE_ADD(end_time, INTERVAL ? HOUR)
WHERE id = ?
```

The `WHERE id = ?` clause restricts the change to exactly one session row; without it, every session in the table would be extended. This is the UPDATE operation. Feedback confirms the added hours and added charge, and the countdown continues from the pushed-forward end time.

### 3.4 Feature: Stopping a Session (Soft Deletion / Archive)

Staff clicks Stop on an in-use PC. The code resolves the live session id and executes:

```sql
UPDATE sessions SET status = 'done', is_archived = 1 WHERE id = ?
```

No `DELETE` statement exists anywhere in the program. The row is preserved with `done`/`1` flags, so sales history and record-keeping keep working while the PC returns to AVAILABLE at once. This flag-based preservation is the ARCHIVE operation. Feedback is a "stopped. Now available." confirmation.

### 3.5 Feature: Automatic Session Expiry (Auto-Stop)

Once per second, before repainting, the dashboard's refresh tick runs one statement over the whole table:

```sql
UPDATE sessions SET status = 'done', is_archived = 1 WHERE status = 'active' AND is_archived = 0 AND end_time <= NOW()
```

Any session whose end time has passed is closed with the same done/archived values a manual Stop writes. Rows already done are excluded by the `WHERE` clause and stay untouched. Because the comparison uses the database clock on both sides (sessions were also written with `NOW()`), there is no Java-versus-MySQL clock mismatch. The normal per-PC refresh that follows then finds no active row and flips each expired card to AVAILABLE with Start re-enabled. This mechanism only runs while the counter window is open, since the timer lives in that process.

### 3.6 Feature: Customer Countdown & Popups

Each customer window resolves its PC once at login (`SELECT id FROM pcs WHERE UPPER(pc_name) = UPPER(?)`, so `pc1` finds `PC1`), then polls every second:

```sql
SELECT id, end_time FROM sessions WHERE pc_id = ? AND status = 'active' AND is_archived = 0 ORDER BY id DESC LIMIT 1
```

Remaining time is `end_time` minus the current time, formatted as H:MM:SS. No active row shows `0:00:00` with "Waiting for counter to start session." Three one-time popups fire per session: "30 minutes remaining." at the 30-minute mark, "5 minutes remaining. Please save your work." at the 5-minute mark, and "Time's Up! Please go to the counter." at zero. Duplicate popups are prevented by three boolean flags (`warned30`, `warned5`, `timesUpShown`) that reset whenever a new session id appears. Closing the customer window stops only the display; the session's `end_time` lives in MySQL, so reopening shows the true remaining time.

### 3.7 Feature: Broadcast Message

Staff clicks the Broadcast button in the dashboard header and types a message into the "Broadcast Message" dialog. Cancel or empty text does nothing. Otherwise one statement flags every PC that currently has a live session:

```sql
UPDATE pcs SET broadcast_pending = 1, broadcast_message = ? WHERE id IN (SELECT pc_id FROM sessions WHERE status = 'active' AND is_archived = 0)
```

A subquery (a query inside a query) first lists the PCs with live sessions; the outer statement flags exactly those rows. The affected-row count returned by the update is the number of PCs reached, so staff sees "Broadcast sent to X PC(s).", or "No active PCs to broadcast to." when the shop is empty. Each customer screen reads its own flag on its next tick:

```sql
SELECT broadcast_pending, broadcast_message FROM pcs WHERE id = ?
```

On flag `1` it shows the text under the title "Notice from Counter", then clears its own row (`UPDATE pcs SET broadcast_pending = 0, broadcast_message = '' WHERE id = ?`) so the message appears exactly once. Clearing happens on the customer side because only that screen knows the moment its human actually saw the message.

---

## 4. System Deployment & Setup Guide (Imperative Mood)

Follow these steps to deploy and evaluate the system on a local machine:

- **Environment Setup:** Open the XAMPP Control Panel on your computer and click Start next to the MySQL service module.
- **Database Creation:** Open your web browser and navigate to `http://localhost/phpmyadmin/`. Click Databases in the top menu bar, type `java_database` in the Database Name field, and click Create.
- **Schema Import:** Select `java_database` in the left sidebar, click the SQL tab, and run the following statements in order. Create the login table first:

```sql
CREATE TABLE IF NOT EXISTS login_table (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'counter'
);
```

Create the PC table next, including the two broadcast columns:

```sql
CREATE TABLE IF NOT EXISTS pcs (
  id INT AUTO_INCREMENT PRIMARY KEY,
  pc_name VARCHAR(20) UNIQUE NOT NULL,
  hourly_rate DECIMAL(10,2) NOT NULL DEFAULT 25.00,
  broadcast_pending TINYINT(1) NOT NULL DEFAULT 0,
  broadcast_message VARCHAR(255) NOT NULL DEFAULT ''
);
```

Create the sessions table last, because it references `pcs`:

```sql
CREATE TABLE IF NOT EXISTS sessions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  pc_id INT NOT NULL,
  hours INT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  is_archived TINYINT NOT NULL DEFAULT 0,
  FOREIGN KEY (pc_id) REFERENCES pcs(id)
);
```

Insert the seed data (one counter account, three customer accounts, three PCs at the shared rate):

```sql
INSERT IGNORE INTO login_table (username, password, role) VALUES
  ('admin', '12345', 'counter'),
  ('pc1', '12345', 'customer'),
  ('pc2', '12345', 'customer'),
  ('pc3', '12345', 'customer');
INSERT IGNORE INTO pcs (pc_name, hourly_rate) VALUES
  ('PC1', 25.00), ('PC2', 25.00), ('PC3', 25.00);
```

- **Project Setup:** Open the project folder `ComputerShopManagement` in Apache NetBeans. Confirm that `mysql-connector-j-9.4.0.jar` is listed under the project's Libraries (it ships in the project's `lib` folder). The project compiles under Java 26 with `Login` as the main class.
- **System Launch:** Right-click the project and click Run, or run `Login.java` directly. The login window opens centered on screen.
- **Default Credentials:**
  - Admin Username: admin
  - Admin Password: 12345
  - Customer PC: pc1 / 12345, pc2 / 12345, pc3 / 12345
- **Smoke Test:** Log in as admin, start PC1 for 2 hours, confirm the card flips to IN USE with a live countdown and the sales total reads P50.00. Log in as pc1 in a second window and confirm the same countdown appears there within one second. Click Stop on the counter and confirm both screens return to the waiting state.

---

## 5. Core Competencies & Security Matrix

- **Input Protection:** Every SQL statement in the program uses `PreparedStatement` with `?` placeholders, and every user-typed value (usernames, passwords, hours selections, broadcast text) is bound separately rather than pasted into query strings. Pasted-together SQL would let a hostile user type database commands into a text box and have them executed (SQL injection). Placeholders remove that possibility because bound values are always treated as plain data, never as code. Typed input is also trimmed, and empty login or broadcast input is rejected before any database call runs.
- **Error Handling:** Every database call is wrapped in a try-catch block. A dead connection produces a friendly `JOptionPane` dialog ("cannot connect to java_database. Check XAMPP.") instead of a crash, and query failures surface the database's own message in a dialog on user actions. The silent once-per-second background ticks log to the console only, so the operator is never flooded with a popup every second.
- **Session Security:** The counter application requires a database-backed login; no counter function is reachable without passing the `login_table` check. Customer PCs identify themselves by the login name matched to the PC name, so no per-PC secrets need to be stored or remembered beyond the shared customer credentials.
- **Data Integrity:** The `sessions.pc_id` foreign key guarantees every session belongs to a real PC. Soft deletion preserves all historical records: ending a session flips flags instead of deleting, and no hard `DELETE` statement exists anywhere in the codebase. Session timestamps are written from the database clock on both creation and expiry comparison, so counter and customer views can never disagree about time.
- **Real-Time Synchronization:** Both the counter and every customer screen poll the shared database once per second. The database is the single source of truth: the counter writes session state, customer screens read it, and neither side keeps its own copy of time. No networking or socket code is required.
- **Rate Handling:** All three PCs use the same hourly rate (P25.00), implemented as a constant in the application code. The hourly_rate column exists in the pcs table to support different rates per PC in future versions. When multiple rates are needed, the code will read the column instead of using a constant.

---

## 6. Final Deliverable Assembly & Workspace Checklist

Project Application Name: Computer Shop Management System
Course & Section: Information Management 2 (BSIT)

### Step 1: UI Consistency & Source Code Hygiene

Tick off each item when preparing the final submission:

- [ ] All four Java files (`connectDB.java`, `Login.java`, `Dashboard.java`, `CustomerTimer.java`) compile with no errors.
- [ ] All forms load correctly in NetBeans Design view (`Login.form`, `Dashboard.form`, `CustomerTimer.form` open with no layout errors).
- [ ] Database contains the three required tables (`login_table`, `pcs`, `sessions`) with seed data (admin account, pc1–pc3 accounts, PC1–PC3 machines).
- [ ] Login, Dashboard, and Customer Timer all launch successfully (admin opens the counter, pc1–pc3 open their timers).
- [ ] All CREATE (session Start), READ (login check, dashboard refresh, customer tick, sales total), UPDATE (Extend, Stop, auto-stop, broadcast flag/clear), and ARCHIVE (done + `is_archived = 1`, never deleted) operations tested end-to-end.
- [ ] No hardcoded test data or fake records in the code — everything comes from the database (the single documented exception is the shared hourly-rate constant described in Section 5).
- [ ] Raw database errors replaced with friendly dialogs on every user action; background ticks stay quiet.
- [ ] Segoe UI typography and the obsidian-rose palette applied consistently across all three windows.

Verified Application Files: `connectDB.java`, `Login.java`, `Dashboard.java`, `CustomerTimer.java`

### Step 2: Database Schema Script Generation

- [ ] All table creations and the foreign key constraint exported into a single master schema script.
- [ ] Seed INSERTs for the admin account, the three customer accounts, and the three PCs included in the same script.
- [ ] Script tested top-to-bottom on a fresh `java_database` with no errors.

Master Schema File Name: `setup.sql` (to be generated from Section 4 on submission day)

### Step 3: Document Package Assembly

- [ ] Setup guide written strictly in step-by-step imperative mood with the live default credentials.
- [ ] Master documentation cleared of placeholder text or empty headings before printing.
- [ ] Documentation file tree mirrors the active repository directory structure.
- [ ] Crow's Foot ERD diagram generated from the actual schema and embedded in Section 2.3.

Master Technical Manual File Name: `TECHNICAL_DOCUMENTATION.md`

STUDENT CONFIRMATION: I verify that the code repository, database schema, and documentation assets checked above match our final production submission package.

Student Signature: _______________________ Date: _______________
