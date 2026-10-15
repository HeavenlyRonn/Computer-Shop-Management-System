# Defense Guide — ERD, Project Functions & How to Present

## 1. What Is Our ERD?

ERD stands for Entity-Relationship Diagram: a drawing of our database where each box is a table (an "entity") and each connecting line is a relationship enforced by a foreign key. Ours has five boxes:

- **login_table** — login accounts (admin + pc1–pc3 with roles).
- **pcs** — the three physical machines (PC1, PC2, PC3) plus per-PC broadcast flags.
- **sessions** — one row per rental: which PC, which customer, hours, amount, start/end times, status flags.
- **customers** — walk-in customers the counter chooses to record.
- **session_log** — append-only audit rows (START, EXTEND, STOP, UNDO_EXTEND) per session.

The three relationships, read in Crow's Foot order (one side, then the many side):

- `pcs (1)` ────< `sessions (M)` — one PC accumulates many session rows; each session belongs to exactly one PC.
- `customers (1)` ────< `sessions (M)` — one customer can rent many times; each session links to at most one customer (nullable for anonymous walk-ins).
- `sessions (1)` ────< `session_log (M)` — one session produces many log entries; each entry belongs to exactly one session.
- `login_table` stands alone with no connecting lines — it governs login only and nothing points at it.

## 2. Purpose of the ERD

Three reasons the panel expects one, in the order they care about:

1. **It proves design before code.** Tables, keys, and relationships were decided first; the Java was written against them. A panel can forgive an ugly button but not a missing design.
2. **It is the contract the code obeys.** Every foreign key in the drawing exists in MySQL and is enforced there: a session for a nonexistent PC is rejected by the database, not by our code. Say this sentence verbatim if asked how the diagram relates to the program.
3. **It scopes every rubric row.** Normalization is argued per box, CREATE/READ/UPDATE per arrow-side operation, ARCHIVE per the flags that keep rows instead of deleting them.

## 3. Is the ERD Exposed in the GUI?

**No — and that is the correct answer, not a weakness.** A case-insensitive search of the entire `src/` folder for anything diagram-related (ERD, diagram, schema viewer, entity display) returns zero matches: no window draws, lists, or describes the schema. The application shows *data* (cards, countdowns, logs); the *design* of that data lives in the documentation package, which is exactly where a panel expects it.

Where the ERD CAN be seen (open either live during defense):

| Artifact | Location | Use it when |
|---|---|---|
| `erd_diagram.png` | `docs/` folder | The panel asks "show me your ERD" — open the image full-screen |
| `DATABASE DOCUMMENTATION.docx` / `.pdf` | `docs/` folder | The panel wants the schema tables plus the diagram in one printable file |

Note: the `.md` sources those exports were built from are no longer in this workspace, so quote the exports or the PNG — do not promise files that are not here.

## 4. Project Functions Catalog

Every demonstrable function, each tagged with the rubric criterion it proves:

| Function | Where | What to Show | Proves |
|---|---|---|---|
| Role login (admin → counter, pc1–3 → timers) | Login window | Log in as both roles; wrong password rejected | READ + connection |
| Start session (with optional customer) | Dashboard PC card | 2-hour start → card flips IN USE, total P50.00 | CREATE |
| Extend session | Dashboard PC card | +1 hour → countdown jumps, amount grows | UPDATE |
| Undo Last Extend | Top-bar button | Exact hours/amount subtracted back | UPDATE + Stack |
| Sort by Amount | Top-bar button | Bubble-sorted dialog, highest first | Data structures |
| Find Session | Top-bar button | Type an id → binary-searched record dialog | Data structures |
| View Logs | Top-bar button | START/EXTEND/STOP rows for the session just made | Audit / READ |
| Broadcast | Header button | Message pops on both customer windows within ~1s | UPDATE + flags |
| Countdown + 3 popups | Customer window | 30-min, 5-min, Time's Up behavior (describe; don't wait it out) | Real-time READ |
| Auto-stop expiry | Counter (automatic) | Force-expire via SQL → card flips AVAILABLE alone | ARCHIVE |
| Stop session | Dashboard PC card | done + archived flags, row preserved in phpMyAdmin | ARCHIVE |
| Customer name on card | Dashboard PC card | "IN USE - Juan Dela Cruz" after named start | JOIN of concepts |

## 5. How to Present Smoothly (7-Minute Demo Script)

Run in this order — each step sets up the next, total under seven minutes:

1. **ERD first (60 seconds).** Open `erd_diagram.png` full-screen. Read the three relationships left to right, state the standalone login table, close with the foreign-key-enforcement sentence from Section 2. Design before demo earns patience for everything after.
2. **Login as admin (30 seconds).** Mention SHA-256 hashing in one sentence while typing. Deliberately type one wrong password first only if time allows — the rejection dialog is free evidence.
3. **Start PC1 for 2 hours with a customer name (90 seconds).** Point at the card flip, the name on the card, and the P50.00 total. This single action later feeds Extend, Undo, Sort, Find, Logs, and Stop.
4. **Extend + Undo (60 seconds).** Extend +1, show the jump, then Undo and show the exact step back. Say "Stack, last-in-first-out" once — that word is the whole data-structures point.
5. **Broadcast (45 seconds).** Have the pc1 window visible beside the counter. Send "closing soon" — popup within a second on the customer side. One sentence on flags: set on many rows at once, cleared per screen after showing.
6. **Sort, Find, Logs (60 seconds).** Open each dialog, read one row aloud, close. These are your data-structures evidence — name Bubble Sort and Binary Search out loud as each dialog opens.
7. **Stop + prove archiving (45 seconds).** Stop PC1, flip to phpMyAdmin, show the row with `done`/`1` still present. Close with: "nothing is ever deleted."

## 6. Likely Panel Questions (One-Line Answers)

- "Where is your ERD?" → "In the documentation package (`erd_diagram.png`), not in the app — the app shows data, the docs show design."
- "Why isn't the ERD in the GUI?" → "Operators need cards and countdowns, not schema drawings; the diagram is a design artifact for review, not an operations screen."
- "How do tables relate?" → Recite the three one-to-many lines plus standalone login_table from Section 1.
- "Prove soft delete." → Stop a session, then `SELECT` the row showing `done`/`1` still present; note zero `DELETE` statements exist in the sources.
- "Prove no SQL injection." → Every query uses `?` placeholders with bound values; typed input can only ever be data.
- "What happens with XAMPP off?" → Every user action shows a friendly dialog; background ticks stay silent — then offer to demo it only if they insist.
- "Who is who in the team?" → Answer from the roles table; never invent teammates.

## 7. Presentation Do's and Don'ts

- Do narrate rubric words out loud (CREATE, soft delete, Stack, Crow's Foot) — panelists tick what they hear named.
- Do keep phpMyAdmin open in a second tab from the start; every claim ends with "and here is the row."
- Do restart from a known database state before the demo (one active session maximum avoids confusing popups).
- Don't wait out a real countdown — describe the 30/5/0 popups, then force-expire via SQL if they want the Time's Up path.
- Don't open code files unless asked; when asked, open the DAO method, not the GUI — short methods read better on a projector.
- Don't apologize for scope cuts (receipts, memberships); state them once as documented out-of-scope decisions.
