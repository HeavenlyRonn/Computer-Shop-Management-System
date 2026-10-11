# CODE WALKTHROUGH — Computer Shop Management System

Local learning reference for the students who wrote this project.
Not for grading. Read this to understand every line of our code.

## HOW TO READ THIS FILE

- NetBeans has a Design view. It lets you drag and drop buttons and
  text fields. When you do that, NetBeans writes a lot of code for
  you automatically. That code lives in a method called
  `initComponents()`. It is always long and ugly. It is NOT our code.
  Every NetBeans project has it. You do not need to memorize it. You
  only need to understand the pattern: create a component, set its
  color, set its font, set its text, place it on the screen.
- Our own code lives in the methods BELOW or ABOVE `initComponents()`.
  Those are the parts we wrote. Those are the parts the panel will
  ask about. Study those carefully.
- In this document, auto-generated sections are marked
  [NETBEANS-GENERATED] and our code is marked [HAND-WRITTEN].

---

# FILE: connectDB.java (22 lines)

## 1. What this file does

This file is the key to the database. Any part of the program that
needs to talk to MySQL asks this file for a connection. A connection
is like a phone line between our Java program and the MySQL server.
Without it, no login check, no Start, no timer can work.

## 2. How it fits in the system

`Login.java`, `Dashboard.java`, and `CustomerTimer.java` all call
`connectDB.getConnection()`. This file depends on nothing in our
project. It only depends on the MySQL connector library
(`mysql-connector-j-9.4.0.jar` in the `lib` folder).

## 3. Auto-generated vs hand-written

- [HAND-WRITTEN] Everything. All 22 lines. NetBeans did not generate
  any of this file. It follows the "calling database connection"
  instruction sheet exactly.

## 4. Imports — line by line

    Line 1: import java.sql.Connection;
    → Brings in the Connection class. A Connection object is the open
      phone line to MySQL. We need it as the return type of our
      method.

    Line 2: import java.sql.DriverManager;
    → Brings in the DriverManager class. This is the helper that
      actually dials the phone number and opens the connection. We
      need it to call `getConnection()`.

    Line 3: import java.sql.SQLException;
    → Brings in the SQLException class. This is the error object Java
      throws when something goes wrong with the database (wrong
      password, MySQL is off, table missing). We need it to catch
      that error.

## 5. Fields (class-level variables)

    private static final String URL = "jdbc:mysql://localhost:3306/java_database";
    → What it is: a String (text) constant named URL.
    → What it holds: the address of our database. Read it in parts:
      `jdbc:mysql` = use the MySQL driver. `localhost` = the database
      runs on this same computer. `3306` = the door number (port)
      MySQL listens on. `java_database` = the name of our database.
    → Why we need it: every connection needs to know where to dial.

    private static final String USER = "root";
    → What it is: a String constant named USER.
    → What it holds: the MySQL username. `root` is the default boss
      account in XAMPP.
    → Why we need it: MySQL asks "who are you?" before it lets us in.

    private static final String PASSWORD = "";
    → What it is: a String constant named PASSWORD.
    → What it holds: empty text. XAMPP MySQL has no password by
      default, so this is empty.
    → Why we need it: MySQL also asks for a password. Ours is empty.

    (`static` means the value belongs to the class itself, not to one
    object. `final` means it can never change. Together they mean
    "one fixed value shared by everyone.")

## 6. Methods — one section per method

### Method: public static Connection getConnection()  [HAND-WRITTEN]

**What it does:**
It dials MySQL and hands back an open connection. If dialing fails,
it prints an error and hands back `null` (which means "nothing").

**Called from:**
Every database action: `Login` login check, Dashboard
Start/Extend/Stop/refresh, CustomerTimer tick.

**Line-by-line:**

    Line 14: try {
    → "Try the next lines. If something breaks, jump to the catch
      block instead of crashing the program."

    Line 15: return DriverManager.getConnection(URL, USER, PASSWORD);
    → Ask the DriverManager to open a connection using our address,
      username, and password. Hand that open connection back to
      whoever called this method (`return` = give back).

    Line 16: } catch (SQLException e) {
    → "If the try block threw a database error, catch it here and
      store the error details in a variable called e."

    Line 17: System.out.println("Database Connection Failed!");
    → Print a simple message to the console so we can see the failure
      in the NetBeans Output window.

    Line 18: e.printStackTrace();
    → Print the full technical details of the error (which line
      failed, why). This is for debugging only.

    Line 19: return null;
    → Hand back `null` (nothing). The caller must check for this. Our
      callers do: `if (c == null)` shows a "cannot connect" message.

**Key ideas in this method:**
- One door for the whole program. If the address or password ever
  changes, we edit one file only.
- `null` means failure. Every caller checks `if (c == null)` first.
- Errors are printed, never hidden. That is why the panel can ask
  "what happens if XAMPP is off?" and we can answer: a message box.

## 7. What would break if we deleted this file?

Everything database-related: login, Start/Extend/Stop, timers, sales
total. Nothing would compile because all three windows call it.

## 8. Defense one-liners

- "What does connectDB do?" → "It opens and returns the MySQL
  connection. One method the whole program shares."
- "Where is the database address?" → "In the URL field:
  localhost, port 3306, database java_database."
- "What is the username and password?" → "root with an empty
  password, the XAMPP default."
- "What happens if MySQL is off?" → "getConnection returns null, and
  each screen shows a 'cannot connect' message."
- "Why static?" → "So any class can call connectDB.getConnection()
  without creating an object first."

---

# FILE: Login.java (297 lines)

## 1. What this file does

This file is the login window. It shows a white card on a dark
background with two boxes (username, password) and a Login button.
When the user clicks Login, it checks the `login_table` in MySQL. If
the name and password match, it opens the Counter Dashboard for
staff or the Customer Timer for customers, then closes itself.

## 2. How it fits in the system

It is the front door. The program starts here (`main` method). It
calls `connectDB.getConnection()` to check credentials. On success it
creates either a `Dashboard` (role is counter/admin) or a
`CustomerTimer` (role is customer), passing the username along.

## 3. Auto-generated vs hand-written

- [NETBEANS-GENERATED] `initComponents()` (about lines 73–192, roughly
  120 lines) plus the `main` look-and-feel block and the variables
  block at the bottom (lines 286–296). Expect long repetitive
  property-setting code. Not ours.
- [HAND-WRITTEN] Constructor (lines 12–17), `styleExtras` (19–44),
  `connectDB` (46–60), the three small event handlers (194–196,
  198–251, 253–255), and the `main` launch lines (279–283).

## 4. Imports — line by line

    Line 1: import java.sql.*;
    → Brings in ALL JDBC classes at once (the `*` means "everything
      in that package"): Connection, PreparedStatement, ResultSet,
      SQLException. We need these for the login SELECT query.

    Line 2: import javax.swing.*;
    → Brings in ALL Swing window classes at once: JFrame, JPanel,
      JLabel, JTextField, JButton, JOptionPane, and more. We need
      these to build the window and popups.

    (Other packages like java.awt are used with full names in the
    code, for example `new java.awt.Color(...)`, so they need no
    import line.)

## 5. Fields (class-level variables)

    private static final Logger logger = Logger.getLogger(Login.class.getName());
    → What it is: a Logger object (a notebook for writing error
      records).
    → What it holds: the logger for this class.
    → Why we need it: NetBeans puts this in every form. Our `main`
      uses it to record an error if the Nimbus look-and-feel fails.

    private static final long serialVersionUID = 1L;
    → What it is: a long number.
    → What it holds: version number `1`. A JFrame is "serializable"
      (Java can save it as bytes). Java wants every serializable
      class to carry a version number. Without it the compiler shows
      a warning.
    → Why we need it: only to keep compilation warning-free. It does
      nothing else.

    ( Nine component fields like RegUsernameTextField and SubmitReg
    are declared at the bottom in the [NETBEANS-GENERATED] variables
    block. They are the text boxes, labels, panels, and button you
    see on screen. NetBeans declares them so our code can use them.)

## 6. Methods — one section per method

### Method: public Login() — the constructor  [HAND-WRITTEN]

**What it does:**
A constructor is a special method that runs once when an object is
born. `new Login()` runs these 4 lines. They build the window,
beautify it, center it, and test the database.

**Called from:**
`main` below, and from Dashboard/CustomerTimer Logout buttons
(`new Login()`).

**Line-by-line:**

    Line 13: initComponents();
    → Run the NetBeans-generated builder. This creates every label,
      box, and button and places them. Always first.

    Line 14: styleExtras();
    → Run our own beautify method (colors, borders, sizing). Always
      after initComponents, so our settings win.

    Line 15: setLocationRelativeTo(null);
    → Put the window in the center of the screen. `null` means
      "relative to nothing", which Java reads as "center on screen".

    Line 16: connectDB();
    → Test the database right away, so a dead MySQL shows an error
      immediately instead of at first login click.

**Key ideas in this method:**
- Order matters: build (NetBeans) → beautify (ours) → center →
  test DB.
- The constructor does no database reading. It only tests the line.

### Method: private void styleExtras()  [HAND-WRITTEN]

**What it does:**
Makes the window pretty and resizable. Sets the title, allows
resizing, sets the smallest allowed size, paints the dark background,
adds the thin dark margin, stretches the white card to fill the
window, styles the input boxes, and packs the window snugly.

**Called from:**
The constructor, once.

**Line-by-line:**

    Line 20: setTitle("Computer Shop - Login");
    → Writes "Computer Shop - Login" in the window's title bar.

    Lines 21–22: (comment) explains the next line. Comments are notes
    for humans. Java ignores them.

    Line 23: setResizable(true);
    → Allows the user to drag the window bigger or smaller.
      initComponents set it to false (from the .form file), so we
      switch it back on here. We do it here and not in the generated
      code because NetBeans would overwrite a generated edit.

    Line 24: setMinimumSize(new java.awt.Dimension(400, 400));
    → The window can never shrink below 400 wide and 400 tall.
      This protects the card from being squashed.

    Line 25: getContentPane().setBackground(new java.awt.Color(24, 20, 22));
    → Paints the frame's background dark obsidian (red 24, green 20,
      blue 22). This color shows in the margin and fills new space
      when the user enlarges the window.

    Line 26: (comment) "Thin dark margin around the card."

    Line 27: backgroundPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    → A Border is a frame around a panel. An EmptyBorder is invisible
      padding. This adds 16 pixels of empty space on all four sides
      inside the dark panel. That is the thin dark strip you see
      around the white card.

    Lines 28–29: (comment) explains the fill trick below.

    Line 30: backgroundPanel.setLayout(new java.awt.BorderLayout());
    → A Layout is the rule a panel uses to arrange its children.
      BorderLayout is a simple rule: one big component fills the
      whole middle (CENTER). We replace the frozen NetBeans layout
      with this rule at runtime. The .form file is untouched.

    Line 31: backgroundPanel.removeAll();
    → Take everything out of the dark panel first, so we can put the
      card back under the new rule.

    Line 32: backgroundPanel.add(loginCard, java.awt.BorderLayout.CENTER);
    → Put the white card back, in the middle. Under BorderLayout it
      grows whenever the window grows. No more dark void.

    Line 34: (comment) "Soft card edge + comfortable input padding."

    Line 35: loginCard.setBorder(BorderFactory.createLineBorder(new java.awt.Color(217, 198, 206), 1));
    → Gives the white card a 1-pixel mauve outline so its edge is
      visible against the dark background.

    Lines 36–38: Build one shared border for both input boxes: a
    1-pixel mauve line outside + 12 pixels of inner left/right
    padding so typed text does not touch the edge.

    Line 39: RegUsernameTextField.setBorder(fieldBorder);
    Line 40: RegPasswordField.setBorder(fieldBorder);
    → Put that shared border on the username box and the password
      box.

    Line 41: SubmitReg.setFocusPainted(false);
    → Removes the dotted focus rectangle around the button. Pure
      looks. Changes nothing else.

    Line 42: (comment) "Snug fit to content; constructor centers
    afterward."

    Line 43: pack();
    → "Shrink-wrap the window around its contents." The window opens
      exactly as big as the card plus margin — no giant empty area.

**Key ideas in this method:**
- Generated code sets things one way; our method overrides them
  afterward. Last writer wins.
- `pack()` = snug size. `setMinimumSize` = never smaller than this.
  `setResizable(true)` = user may go bigger.

### Method: private void connectDB()  [HAND-WRITTEN]

**What it does:**
Loads the MySQL driver and makes one test call to
`connectDB.getConnection()`. Prints "Database connected!" if it
works, or shows an error popup if it fails. It closes the test
connection right away. (Note: this method has the same name as the
`connectDB` class. Same name, different thing: the class is the
helper file, this method is our quick test. Java can tell them
apart.)

**Called from:**
The constructor, once at startup.

**Line-by-line:**

    Line 48: Class.forName("com.mysql.cj.jdbc.Driver");
    → Load the MySQL driver into memory. A driver is a translator
      between Java and MySQL. Without loading it, Java cannot speak
      to MySQL. (Our teacher's sheet lists this exact line.)

    Line 49: java.sql.Connection test = connectDB.getConnection();
    → Ask our helper file for a connection and store it in a
      variable called `test`. This is only a test line.

    Line 50: if (test != null) {
    → If we got a real connection (not null), go inside.

    Line 51: System.out.println("Database connected!");
    → Print success to the console. KEEP this line — it is
      intentional, not leftover debugging.

    Line 52: try { test.close(); } catch (Exception ex) { }
    → Close the test line right away. We only wanted to check the
      line works. The empty catch means "if closing fails, ignore
      it" — closing failure here is harmless.

    Lines 53–54: } else { show message "DB Connection Error: cannot
    connect to java_database." }
    → If the helper returned null, show a popup telling the user to
      check XAMPP.

    Lines 56–59: catch (Exception e) { print details; show popup with
    the error message. }
    → If anything above exploded (driver missing, etc.), show the
      reason in a popup instead of crashing.

**Key ideas in this method:**
- Test early, fail loudly. Better to see the error at startup than
  at the first button click.
- The test connection is always closed. Real queries open their own
  fresh connections later.

### Method: private void initComponents()  [NETBEANS-GENERATED]

**What it does:**
Builds every visible piece: dark panel, white card, two labels,
two input boxes, one button. Sets each one's color, font, and text.
Lays them out with GroupLayout (NetBeans' automatic arranger).
Ends with `pack()`. About 120 lines (73–192). Do NOT edit — the
Design view owns this method.

**Grouped walkthrough (pattern, not every line):**
- Lines 75–83: `new` each component (empty objects, nothing shown
  yet).
- Lines 85–87: window settings — close the program on X, title
  text, resizable false (we override this later in styleExtras).
- Lines 89–91: paint the two panels (obsidian card-back, rose-mist
  card).
- Lines 93–125: for each label, box, and button — set font (Segoe
  UI, size, bold or not), set text color, set displayed text, and
  connect buttons/boxes to their handler methods
  (`addActionListener(this::...)` means "when clicked or Enter is
  pressed, run that method").
- Lines 127–161: card interior arrangement — title, subtitle,
  USERNAME label, username box (38px tall), PASSWORD label,
  password box, Login button (42px tall), with fixed gaps (32px
  sides, 28px top/bottom).
- Lines 163–189: outer arrangement — card centered in the dark
  panel with stretchy gaps, dark panel filling the whole window.
  (Superseded at runtime by our BorderLayout; kept frozen for the
  Design view.)
- Line 191: `pack()` — first snug sizing (we pack again after our
  borders).

### Method: private void RegUsernameTextFieldActionPerformed(...)  [HAND-WRITTEN]

**What it does:**
Runs when the user presses Enter inside the username box. It just
moves the typing cursor to the password box. One line.

**Called from:**
Pressing Enter in the username field (wired in initComponents).

**Line-by-line:**

    Line 195: RegPasswordField.requestFocusInWindow();
    → "Put the blinking cursor into the password box." Small comfort
      so the user can keep typing without touching the mouse.

### Method: private void SubmitRegActionPerformed(...)  [HAND-WRITTEN]

**What it does:**
The heart of login. Reads the two boxes, rejects empty input, asks
MySQL "is there a row with this username AND password?", opens the
right next window on success, or shows "Invalid username or
password!" on failure.

**Called from:**
Clicking the Login button, or pressing Enter in the password box
(which clicks the button for us).

**Line-by-line:**

    Line 199: String username = RegUsernameTextField.getText().trim();
    → Read whatever the user typed in the username box. `.trim()`
      cuts spaces off both ends. If the user types "  admin  ", this
      variable becomes "admin".

    Line 200: String password = new String(RegPasswordField.getPassword());
    → `getPassword()` returns a char array (a list of characters),
      not a String. This is for security: arrays can be wiped from
      memory. We wrap it with `new String(...)` to convert it into a
      normal String we can work with. If the user types "12345",
      this variable becomes "12345".

    Line 202: if (username.isEmpty() || password.isEmpty()) {
    → If either box is empty (`||` means OR), go inside.

    Lines 203–204: Show popup "Please enter username and password."
    and `return` (stop the method immediately — do not touch the
    database for empty input).

    Line 207: String sql = "SELECT * FROM login_table WHERE username = ? AND password = ?";
    → The question we will ask MySQL: "give me every column (`*`)
      from login_table where both match." The `?` marks are
      placeholders (empty slots). We fill them next. This exact
      query comes from our instruction sheet.

    Line 210: Connection c = connectDB.getConnection();
    → Open a fresh database connection for this one login attempt.

    Lines 211–214: if (c == null) { show "cannot connect... Check
    XAMPP." and return. }
    → If MySQL is off, stop here with a clear message instead of
      crashing.

    Line 215: try (PreparedStatement pst = c.prepareStatement(sql)) {
    → It uses a PreparedStatement. This is a way to send a SQL query
      safely. Instead of pasting user input directly into the query,
      we put `?` placeholders and bind the values separately. This
      stops users from typing SQL code to cheat the system. (`try
      (...)` means "close this automatically when done" — this is
      called try-with-resources.)

    Line 216: pst.setString(1, username);
    Line 217: pst.setString(2, password);
    → Fill slot 1 with the typed username, slot 2 with the typed
      password. Slots are numbered from 1, not 0.

    Line 218: try (ResultSet rs = pst.executeQuery()) {
    → Send the filled query to MySQL and store the answer table in
      `rs` (ResultSet = the answer table). Also auto-closed.

    Line 219: if (rs.next()) {
    → Move to the first answer row. Returns true if a row exists.
      So: "if at least one matching row was found" = login data is
      correct. Go inside. Otherwise jump to the else (line 241).

    Line 220: String role = "counter";
    → Assume staff unless the row says otherwise.

    Lines 221–229: try { read column "role"; if it has real text, use
    it } catch { keep "counter". }
    → Some instruction-sheet tables have only username+password (no
      role column). Reading a missing column throws an error. We
      catch it and stay with "counter". This keeps us compatible
      with the strict instruction table AND the role table.

    Line 230: Show popup "Login Successful!".

    Line 231: if ("customer".equalsIgnoreCase(role)) {
    → If the role text is "customer" (any capital letters allowed),
      go inside. Otherwise it is staff.

    Lines 232–234: new CustomerTimer(username); center it; show it.
    → Open the customer countdown window, passing the username so it
      knows which PC it is (pc1 → PC1).

    Lines 236–238: new Dashboard(); center it; show it.
    → Staff path: open the counter dashboard.

    Line 240: this.dispose();
    → Close the login window and free its memory. `this` = this
      window. `dispose()` = close and clean up.

    Lines 241–243: else { show "Invalid username or password!" }
    → No matching row: wrong name, wrong password, or both.

    Lines 245–247: finally { try { c.close(); } ... }
    → `finally` means "always run this, success or failure". Close
      the connection so MySQL is not left with open lines piling up.

    Lines 248–250: catch (SQLException e) { show "Database Error: "
    + the reason. }
    → If the query itself broke (table deleted, SQL typo), show the
      database's own error message instead of crashing.

**Key ideas in this method:**
- Never trust typed input: trim it, reject empties, bind it with
  `?` placeholders. This is how we prevent SQL injection (typing
  database commands into the login box to break in).
- One connection per attempt, always closed in `finally`. Open
  lines never pile up.
- The `role` column is read defensively so both table shapes work.

### Method: private void RegPasswordFieldActionPerformed(...)  [HAND-WRITTEN]

**What it does:**
Runs when Enter is pressed in the password box. Clicks the Login
button for the user. One line.

**Called from:**
Pressing Enter in the password field.

**Line-by-line:**

    Line 254: SubmitReg.doClick();
    → "Pretend the Login button was clicked." Runs the big login
      method above. Enter-to-login comfort.

### Method: public static void main(String args[])  [MOSTLY NETBEANS-GENERATED]

**What it does:**
The program's starting point. Sets the Nimbus look (NetBeans'
modern gray style), then opens the Login window on the screen.
Short version: try Nimbus → if missing, log it → create Login,
center, show. The only hand-written-flavored lines are 279–283
(create, center, `setVisible(true)` = show the window).

**Called from:**
Nowhere in code — Java runs `main` automatically when the program
starts. (`EventQueue.invokeLater` means "run this on the window
thread", the special thread Swing uses for anything on screen.)

## 7. What would break if we deleted this file?

The program could not start (`main` lives here) and nobody could
log in. Dashboard and CustomerTimer would lose their Logout target.

## 8. Defense one-liners

- "How does login check credentials?" → "SELECT from login_table
  with username and password as ? placeholders, bound separately."
- "Why PreparedStatement and not plain text?" → "Placeholders stop
  SQL injection — typed input can never run as code."
- "How do you read the password box?" → "getPassword gives a char
  array; I wrap it in new String to compare it."
- "What decides Dashboard vs CustomerTimer?" → "The role column:
  customer opens the timer, anything else opens the counter."
- "What if the table has no role column?" → "I catch that and
  default to counter, so both table shapes work."

---

# FILE: Dashboard.java (822 lines)

## 1. What this file does

This file is the counter window for staff. It shows 3 PC cards
(PC1, PC2, PC3) side by side. Each card shows AVAILABLE or IN USE,
a live countdown, an hours picker (1–6), and Start / Extend / Stop
buttons. The top bar shows today's running sales total and a
Broadcast button that sends one message to every PC that is in use.
Everything refreshes every second from the database.

## 2. How it fits in the system

`Login` opens it for staff logins. It reads/writes the `pcs` and
`sessions` tables through `connectDB`. Its Start writes rows that
`CustomerTimer` later reads — the counter is the writer, the
customer screens are readers. Logout returns to `Login`.

## 3. Auto-generated vs hand-written

- [NETBEANS-GENERATED] `initComponents()` (about lines 386–740,
  roughly 355 lines: 33 component creations + all property blocks +
  three PC-panel layouts + frozen outer GroupLayout) and the
  variables block (lines 788–821). Not ours; Design view owns it.
  (The Broadcast button is NOT in there — it is created by hand in
  `styleExtras`, because the frozen .form has no such button.)
- [HAND-WRITTEN] Everything else, about 340 lines: constructor,
  `styleExtras` (with the BorderLayout + GridLayout runtime
  overrides and the hand-made Broadcast button), `initHourBoxes`,
  `loadPCs`, `startAutoRefresh`,
  `refreshState`, `autoStopExpired`, `refreshOne`,
  `updateSalesTotal`, `formatHMS`, `selectedHours`,
  `doStart/doExtend/doStop`, `doBroadcast`, and 11 one-line button
  handlers.

## 4. Imports — line by line

    Line 1: import java.sql.*;
    → All JDBC classes (Connection, PreparedStatement, ResultSet,
      SQLException, Timestamp). We run SELECT, INSERT, and UPDATE
      queries here, so we need all of them.

    Line 2: import javax.swing.*;
    → All Swing classes (JFrame, JPanel, JButton, JOptionPane,
      Timer, DefaultComboBoxModel...). We need these for the window,
      the 1-second timer, the hour dropdowns, and popups.

## 5. Fields (class-level variables)

    private static final double HOURLY_RATE = 25.0;
    → What it is: a fixed decimal number.
    → What it holds: 25 pesos per hour. Same for all 3 PCs (approved
      class decision).
    → Why we need it: cost = hours × this rate. One place to change
      the price.

    private static final long serialVersionUID = 1L;
    → Same version-number story as in Login. Silences a compiler
      warning. Does nothing else.

    private final HashMap<String, Integer> pcIds = new HashMap<>();
    → What it is: a HashMap (a dictionary: word → number).
    → What it holds: "PC1" → database id, "PC2" → id, "PC3" → id.
      Loaded once from the `pcs` table.
    → Why we need it: the screen thinks in names ("PC1"), the
      database thinks in id numbers. This translates between them.

    private final HashMap<String, Integer> activeSessionIds = new HashMap<>();
    → What it is: another dictionary.
    → What it holds: "PC1" → current session row id, but ONLY while
      that PC is in use. Refreshed every second.
    → Why we need it: Extend and Stop must know WHICH session row to
      UPDATE. This map remembers it.

    private javax.swing.Timer refreshTimer;
    → What it is: a Swing Timer object.
    → What it holds: our 1-second heartbeat. A Timer is like an alarm
      clock that rings over and over. Ours rings every 1000 ms and
      each ring refreshes the whole screen from the database.
    → Why we need it: without it the countdowns would freeze and
      expired sessions would never clear by themselves.

    private javax.swing.JButton broadcastButton;
    → What it is: a JButton object (a clickable button), declared
      here but created later in `styleExtras`.
    → What it holds: the "Broadcast" button in the top bar.
    → Why we need it: the frozen .form has no Broadcast button, so
      we declare the variable by hand and build the button in code.
      Without this field, `styleExtras` could not keep a reference
      to the button it creates.

## 6. Methods — one section per method

### Method: public Dashboard() — the constructor  [HAND-WRITTEN]

**What it does:**
Builds the window, beautifies it, centers it, fills the hour boxes,
loads the PC ids, paints the first state, then starts the heartbeat.

**Called from:**
`Login` after a staff login.

**Line-by-line:**

    Line 14: initComponents();
    → NetBeans builds all 33 components first.

    Line 15: styleExtras();
    → Our beautify + fill/stretch overrides (see below).

    Line 16: setLocationRelativeTo(null);
    → Center on screen (after pack inside styleExtras).

    Line 17: initHourBoxes();
    → Fill the three 1–6 dropdowns.

    Line 18: loadPCs();
    → Read PC ids from the database into the dictionary.

    Line 19: refreshState();
    → Paint the first correct state immediately (no 1-second wait).

    Line 20: startAutoRefresh();
    → Start the 1-second heartbeat.

**Key ideas in this method:**
- Build → beautify → center → load data → paint once → heartbeat.
- The first `refreshState()` means the screen is never blank.

### Method: private void styleExtras()  [HAND-WRITTEN]

**What it does:**
Same window setup as Login (resizable, minimum 900×560, dark
background, 16px margin, snug `pack()`), plus the Dashboard-only
stretch rebuild: the frozen card interior is re-parented at runtime
into a header strip (title left, sales + logout right) and a
`GridLayout(1, 3, 12, 12)` row so the 3 PC cards always share the
width evenly, compact or maximized.

**Called from:**
The constructor, once.

**Line-by-line (important lines only; borders/focus lines follow the
Login pattern — 1px mauve outlines, no focus rectangles):**

    Lines 25–29: setResizable(true) with the same Design-save-safe
    comment as Login; minimum 900×560; dark content background.
    → Same reasoning as Login. Minimum is wide because 3 cards must
      fit side by side.

    Line 31: backgroundPanel EmptyBorder(16,16,16,16) — the thin
    dark strip.
    Lines 32–36: backgroundPanel → BorderLayout, removeAll, add
    mainCard CENTER — card fills the window (same trick as Login).
    Lines 37–55: build headerPanel (BorderLayout: title WEST, sales
    + logout packed RIGHT in a small FlowLayout helper) and pcRow
    (GridLayout 1 row × 3 columns, 12px gaps); move the existing
    title/sales/logout/pc panels into them with add(). No new
    components are created — the same on-screen pieces just get new
    parents. `new` here only makes the two invisible wrapper
    panels. `GridLayout(1,3,12,12)` means "1 row, 3 equal columns,
    12 pixels between them".
    Lines 46–52: build the Broadcast button by hand (the frozen
    .form has no such button, so `new` must appear here, not in
    initComponents): rose background like Logout, white Segoe UI 13
    bold text, no focus rectangle, wired with
    addActionListener(this::broadcastButtonActionPerformed) to its
    handler, then placed in the header's right strip between the
    sales label and Logout.
    → Same visual style as Logout so it blends in. Wiring with
      `this::...` means "when clicked, run that method" — the same
      pattern NetBeans uses for the other buttons.

    Lines 52–58: mainCard gets BorderLayout (header NORTH, pcRow
    CENTER) plus a compound border: mauve line outside + 20/24px
    inner padding (replaces the frozen 24px GroupLayout side gaps).
    Lines 72–76: rate labels all show "P25/hr" from the single
    HOURLY_RATE constant.
    Line 78: pack() — snug open size; constructor centers after.

**Key ideas in this method:**
- Re-parenting (moving components into new wrapper panels) keeps
  every listener, color, and font. Only the arrangement rule
  changes.
- GridLayout forces equal widths — that is what fixes the
  "cards cluster left" defect.

### Method: private void initHourBoxes()  [HAND-WRITTEN]

**What it does:**
Fills the three hours dropdowns with the choices 1–6. Three lines,
one per PC.

**Called from:**
The constructor, once.

**Line-by-line:**

    Line 82: String[] hours = {"1","2","3","4","5","6"};
    → Make a small list of six text choices.

    Lines 83–85: pc1Hours.setModel(new DefaultComboBoxModel<>(hours)); (×3)
    → A ComboBoxModel is the list behind a dropdown. We hand the
      same six choices to each PC's dropdown.

### Method: private void loadPCs()  [HAND-WRITTEN]

**What it does:**
Reads the `pcs` table once and fills the `pcIds` dictionary. Warns
if PC1–PC3 are missing (setup SQL not run).

**Called from:**
The constructor, once.

**Line-by-line:**

    Line 89: String sql = "SELECT id, pc_name FROM pcs ORDER BY pc_name";
    → "Give me every PC's id and name, sorted A–Z."

    Lines 91–95: open connection; if null, popup "cannot
    connect... Check XAMPP" and return.
    → Same null-check pattern as everywhere.

    Lines 96–97: try (PreparedStatement ...; ResultSet rs =
    pst.executeQuery()) {
    → Send the query, store the answer table. Both auto-close.

    Lines 98–101: pcIds.clear(); while (rs.next()) {
    pcIds.put(rs.getString("pc_name"), rs.getInt("id")); }
    → Empty the dictionary, then walk every answer row (`while`
      repeats until rows run out) and store name → id.

    Lines 102–104: finally { close connection } — always clean up.
    Lines 105–107: if PC1/PC2/PC3 not all present → popup "pcs table
    is missing PC1-PC3. Run the setup SQL first."
    Lines 108–110: catch SQLException → popup with the database's own
    error message.

**Key ideas in this method:**
- `while (rs.next())` = "for every row in the answer". `if` =
  one row; `while` = all rows.
- This runs ONCE. The per-second refresh does not re-read this
  table.

### Method: private void startAutoRefresh()  [HAND-WRITTEN]

**What it does:**
Creates the 1-second heartbeat and starts it. Three lines.

**Called from:**
The constructor, once (and the timer then calls `refreshState()`
forever until Logout stops it).

**Line-by-line:**

    Line 114: refreshTimer = new javax.swing.Timer(1000, e -> refreshState());
    → Make a Timer that rings every 1000 milliseconds (1 second).
      Each ring runs `refreshState()`. The `e -> ...` part is a
      tiny nameless function (lambda) meaning "when you ring, do
      this".

    Line 115: refreshTimer.start();
    → Start the ringing.

### Method: private void refreshState()  [HAND-WRITTEN]

**What it does:**
One full screen repaint from the database: auto-stop expired
sessions first, then refresh each of the 3 cards, then the sales
total. Runs every second.

**Called from:**
Constructor (first paint) and the timer (every second).

**Line-by-line:**

    Line 119: autoStopExpired();
    → First, close out anything whose end_time passed (see next
      method). Must run BEFORE reading state to display.

    Lines 120–122: refreshOne("PC1", ...); refreshOne("PC2", ...);
    refreshOne("PC3", ...);
    → Refresh each card: pass its name plus its 6 screen pieces
      (status label, countdown label, 3 buttons) so the helper can
      update them.

    Line 123: updateSalesTotal();
    → Recompute today's sales label last.

### Method: private void autoStopExpired()  [HAND-WRITTEN]

**What it does:**
Closes every expired session in ONE database statement: any row
still marked active whose end_time is already past becomes done +
archived. This is what flips PCs back to AVAILABLE with no click.
It preserves the row (sets flags, never deletes) so ARCHIVE rubric
credit is kept.

**Called from:**
`refreshState()`, i.e. every second.

**Line-by-line:**

    Line 127: String sql = "UPDATE sessions SET status = 'done',
    is_archived = 1 WHERE status = 'active' AND is_archived = 0 AND
    end_time <= NOW()";
    → "For every row that is still active, not archived, and whose
      end time is now or past (`NOW()` = the database's current
      clock), mark it done and archived." Rows already done are
      untouched — the WHERE filters them out.

    Lines 128–140: open connection (silent return on null — no popup
    every second, or the user would drown in popups); executeUpdate()
    (`executeUpdate` = run a query that CHANGES rows instead of
    asking a question; it returns how many rows changed, which we
    ignore); always close; on SQLException print "AutoStop Error"
    to console only (quiet by design on a 1-second loop).

**Key ideas in this method:**
- One statement fixes ALL PCs at once. No per-PC code needed.
- The write uses the database clock (`NOW()`) on both sides
  (INSERT also used `NOW()`), so no Java-vs-MySQL clock mismatch.
- It only works while the counter window is OPEN — the timer lives
  in this process. That was our investigation finding.

### Method: private void refreshOne(...)  [HAND-WRITTEN]

**What it does:**
Refreshes ONE PC card: asks "does this PC have an active session?",
computes remaining time, paints IN USE or AVAILABLE, and enables
the right buttons (Start only when free; Extend/Stop only when
busy). Remembers the session id for Extend/Stop.

**Called from:**
`refreshState()` — three times, once per PC.

**Line-by-line:**

    Lines 146–155: look up the pcId from the dictionary. If missing
    (setup SQL never run), paint "NO DB", show 0:00:00, disable all
    three buttons, return.
    → Fail visibly instead of crashing.

    Line 156: SELECT id, end_time FROM sessions WHERE pc_id = ? AND
    status = 'active' AND is_archived = 0 ORDER BY id DESC LIMIT 1
    → "Give me the newest active session for this ONE pc." `LIMIT 1`
      = at most one row back.

    Lines 157–178: open connection (silent return on null);
    bind pcId; run; if a row exists → store session id in
    activeSessionIds; remaining = end_time minus the computer's
    current clock (floor at 0, never negative); paint "IN USE" in
    rose + the H:MM:SS countdown; Start OFF, Extend/Stop ON.

    Lines 179–187: else (no row) → forget the session id; paint
    "AVAILABLE" in muted gray + 0:00:00; Start ON, Extend/Stop OFF.

    Lines 188–195: always close; on error print "Refresh Error" only
    (quiet loop — real errors surface on button clicks).

**Key ideas in this method:**
- The screen NEVER stores time itself. It always recomputes from
  `end_time`. Close and reopen — the countdown is still right.
- Button states ARE the business rules: you cannot Start a busy PC
  because the button is off.

### Method: private void updateSalesTotal()  [HAND-WRITTEN]

**What it does:**
Recomputes "Today's Sales" as the SUM of every session started
today, and paints the label. Runs every second.

**Called from:**
`refreshState()`.

**Line-by-line:**

    Line 199: SELECT COALESCE(SUM(amount), 0) AS total FROM
    sessions WHERE DATE(start_time) = CURDATE()
    → "Add up the amount column for rows started today." `SUM` =
      add up. `COALESCE(...,0)` = "if there are no rows, give 0
      instead of empty". `CURDATE()` = the database's today date.
      Old days stay in the table (rubric history) but never count
      here — that is our "reset view".

    Lines 200–216: open, run, read the "total" column as a decimal,
    paint `Today's Sales: Pxx.xx` (`%.2f` = always 2 decimals, so
    50 shows as 50.00). Quiet on failure.

### Method: private String formatHMS(long millis)  [HAND-WRITTEN]

**What it does:**
Turns milliseconds (thousandths of a second) into text like
`1:05:09`. Pure math, no database.

**Called from:**
`refreshOne` (counter) and `tick` (customer timer equivalent).

**Line-by-line:**

    Line 220: long s = millis / 1000; → whole seconds.
    Line 221: long h = s / 3600; → whole hours (3600 seconds each).
    Line 222: long m = (s % 3600) / 60; → leftover minutes (`%` =
    remainder after division).
    Line 223: long sec = s % 60; → leftover seconds.
    Line 224: return String.format("%d:%02d:%02d", h, m, sec);
    → Build "H:MM:SS". `%02d` means "two digits, pad with 0" so 5
      minutes shows as 05, never as 5.

### Method: private int selectedHours(String pcName)  [HAND-WRITTEN]

**What it does:**
Reads the hours dropdown for the named PC and returns it as a
number 1–6. Returns 1 if anything is strange (safety default).

**Called from:**
`doStart` and `doExtend`.

**Line-by-line:**
Pick the right dropdown by name (PC1 → pc1Hours, PC2 → pc2Hours,
else pc3Hours); read its selected text; `Integer.parseInt` converts
text "2" into number 2; guard `if (h < 1 || h > 6) return 1` rejects
out-of-range values; any exception → return 1.

### Method: private void doStart(String pcName)  [HAND-WRITTEN]

**What it does:**
Starts a session: refuses if the PC is busy, reads hours, computes
cost (hours × 25), INSERTs one session row starting NOW and ending
hours later, pops a confirmation, repaints.

**Called from:**
The three Start buttons (each passes its PC name).

**Line-by-line:**

    Lines 248–256: look up pcId (missing → error popup, return);
    refuse if activeSessionIds already has this PC ("already in
    use" popup, return). Guards BEFORE touching the database.

    Line 257: int hours = selectedHours(pcName); → 1–6 from dropdown.
    Line 258: double amount = hours * HOURLY_RATE; → e.g. 2 × 25 =
    50.00. Same rate for all PCs (approved decision).

    Line 259: INSERT INTO sessions (pc_id, hours, amount, start_time,
    end_time, status, is_archived) VALUES (?, ?, ?, NOW(),
    DATE_ADD(NOW(), INTERVAL ? HOUR), 'active', 0)
    → "Create one session row: this PC, these hours, this cost,
      starting at the database clock NOW, ending hours later
      (`DATE_ADD` = date plus; `INTERVAL ? HOUR` = the picked
      hours), marked active and not archived."

    Lines 260–276: open (null → popup, return); bind pcId, hours,
    amount, hours; `executeUpdate()` writes the row; always close;
    popup "PC1 started: 2 hr (P50.00)."; `refreshState()` repaints
    immediately (no 1-second wait).

    Lines 277–279: catch SQLException → popup with the reason.

**Key ideas in this method:**
- Guard first (busy? unknown PC?), write second.
- Times come from the DATABASE clock (`NOW()`), never the Java
  clock — so counter and customer always agree.
- CREATE rubric point lives here (this INSERT).

### Method: private void doExtend(String pcName)  [HAND-WRITTEN]

**What it does:**
Adds more hours to the running session: UPDATEs that one row
(hours grow, amount grows, end_time moves later), pops a
confirmation, repaints.

**Called from:**
The three Extend buttons.

**Line-by-line:**

    Lines 283–287: look up the live session id; none → "has no
    active session" popup, return.
    Line 288: int add = selectedHours(pcName); → extra hours 1–6.
    Line 289: double addAmount = add * HOURLY_RATE; → extra cost.
    Line 290: UPDATE sessions SET hours = hours + ?, amount = amount
    + ?, end_time = DATE_ADD(end_time, INTERVAL ? HOUR) WHERE id = ?
    → "On THIS one row only (`WHERE id = ?`), grow hours, grow
      cost, push the end later by the added hours."

    Lines 291–310: open, bind add/addAmount/add/sessionId, run,
    close, popup "extended: +X hr (P...).", repaint. Catch →
    error popup.

**Key ideas in this method:**
- `WHERE id = ?` is the whole safety: exactly one row changes.
  Forgetting the WHERE would extend EVERY session — the classic
  mistake. Ours always has it.
- UPDATE rubric point lives here (and in doStop).

### Method: private void doStop(String pcName)  [HAND-WRITTEN]

**What it does:**
Ends the session NOW: marks that one row done + archived (row
kept, never deleted — ARCHIVE credit), pops confirmation, repaints.
The PC becomes AVAILABLE immediately.

**Called from:**
The three Stop buttons.

**Line-by-line:**

    Lines 314–318: look up the live session id; none → popup, return.
    Line 319: UPDATE sessions SET status = 'done', is_archived = 1
    WHERE id = ?
    → "On THIS one row: mark done and archived." Same values the
      auto-stop uses. The row stays in the table (history + sales
      total keep working).

    Lines 320–336: open, bind id, run, close, popup "stopped. Now
    available.", repaint. Catch → error popup.

### Method: private void broadcastButtonActionPerformed(...)  [HAND-WRITTEN]

Forwards the Broadcast click to `doBroadcast()`. Called by the
Broadcast button (wired by hand in `styleExtras`, since the frozen
.form has no such button). One line — same thin-handler pattern as
the Start/Extend/Stop buttons.

### Method: private void doBroadcast()  [HAND-WRITTEN]

**What it does:**
Asks staff for one message, then flags every PC that is currently
in use so each customer screen pops the message on its next tick.
Reports how many PCs were reached, or says there was nobody to
reach.

**Called from:**
`broadcastButtonActionPerformed` (the Broadcast button).

**Line-by-line:**

    Line 352: String message = JOptionPane.showInputDialog(this,
    "Enter message for all active PCs:", "Broadcast Message",
    JOptionPane.PLAIN_MESSAGE);
    → Open a small typing dialog. `showInputDialog` shows a box with
      a text field and OK/Cancel buttons. The first text is the
      question inside, "Broadcast Message" is the dialog's title
      bar, PLAIN_MESSAGE means no icon. Whatever staff types comes
      back as a String. If they press Cancel, it comes back as
      `null` (nothing).

    Lines 353–355: if (message == null || message.trim().isEmpty())
    { return; }
    → If staff pressed Cancel (null) OR typed only spaces (empty
      after trimming), stop here silently. No database touch, no
      popup. An empty broadcast to nobody would be pointless.

    Line 356: UPDATE pcs SET broadcast_pending = 1,
    broadcast_message = ? WHERE id IN (SELECT pc_id FROM sessions
    WHERE status = 'active' AND is_archived = 0)
    → "On every PC that is currently in use, raise the broadcast
      flag and store this message." The inner part (a subquery — a
      query inside a query) finds the ids of PCs with live
      sessions. The outer part flags exactly those PC rows. `pcs`
      has no status column of its own, so the subquery is how we
      ask "who is active?". The message goes into the `?` slot,
      bound safely like a login (no pasting), so quotes or strange
      characters in the text cannot break the SQL.

    Lines 357–362: open connection (null → "cannot connect"
    popup, return). Same null-check pattern as everywhere.

    Lines 363–370: prepare the UPDATE; bind the trimmed message;
    int count = pst.executeUpdate();
    → `executeUpdate` runs a changing query AND tells us how many
      rows changed. That number IS the number of PCs reached — one
      changed row per active PC, because the UPDATE touches each
      flagged PC row exactly once. If 0 → popup "No active PCs to
      broadcast to." Otherwise → popup "Broadcast sent to X
      PC(s)." with the real count. Always closed in `finally`.

    Lines 374–376: catch (SQLException e) → popup "Database Error: "
    + the reason (for example, if the two new columns were never
    added with ALTER TABLE, the database says "unknown column" and
    staff sees it instead of silence).

**Key ideas in this method:**
- A subquery lets one statement answer two questions: "who is
  active?" (inner) and "flag them" (outer). No per-PC loop in Java.
- The affected-row count is free feedback: the database already
  counts what it changed, so we never need a separate SELECT.
- Cancel/empty input returns BEFORE any SQL runs — the database
  never sees an accidental click.

**Defense one-liners (broadcast):**
- "How does one UPDATE reach many PCs?" → "The WHERE uses a
  subquery that lists every PC with a live session."
- "How do you know how many PCs got it?" → "`executeUpdate`
  returns the changed-row count — one per flagged PC."
- "Why not loop over PCs in Java?" → "One statement is faster and
  cannot half-finish; the database does it atomically."

### Handlers (11, all one-liners)  [HAND-WRITTEN]

`logoutButtonActionPerformed` — stop the heartbeat (`refreshTimer`
would otherwise tick forever on a closed window), open Login
centered, close this window. Called by the Logout button.

`pc1Start/pc2Start/pc3StartActionPerformed` → `doStart("PCx")`.
`pc1Extend/...` → `doExtend("PCx")`. `pc1Stop/...` →
`doStop("PCx")`. Each handler only forwards its PC name. Called by
its button (wired in initComponents).

`broadcastButtonActionPerformed` → `doBroadcast()`. Same thin
forwarder. Called by the hand-made Broadcast button (wired in
`styleExtras`, not initComponents).

### Method: private void initComponents()  [NETBEANS-GENERATED]

About 355 lines (386–740). Same pattern as Login's, times three PC
panels: create 33 components → window settings → paint panels/labels
(title 20 bold, sales rose 14, PC names 16, countdowns 20, buttons)
→ three identical PC-panel interior layouts (name, rate, status,
countdown, hours box 30px, three 36px buttons) → frozen outer
GroupLayout (card 920 wide, centered with stretchy gaps) → `pack()`.
Runtime supersedes the outer part (our BorderLayout + GridLayout);
internals are untouched. Variables block lines 788–821 declares all
33 components. (The Broadcast button is not counted here — it is
built by hand in `styleExtras`, outside initComponents.)

## 7. What would break if we deleted this file?

Staff could log in but land nowhere. No Start/Extend/Stop, no
countdowns, no sales total, no auto-stop. Customer timers would
wait forever (nobody writes sessions).

## 8. Defense one-liners

- "How does Start work?" → "INSERT one sessions row with
  start NOW and end NOW plus hours, status active."
- "How does Extend avoid touching other rows?" → "UPDATE with
  WHERE id = the live session id from my map."
- "How does Stop differ from delete?" → "It sets done and archived
  to 1. The row stays — that is our ARCHIVE."
- "How do expired sessions clear themselves?" → "Every second,
  one UPDATE closes all rows whose end_time passed, then the
  screen repaints."
- "Where does the sales total come from?" → "SUM of amount for
  rows started today. Old rows stay but never count."
- "How does Broadcast reach every active PC?" → "One UPDATE flags
  all PC rows whose id is in the live-sessions subquery."
- "What if nobody is in use?" → "The update changes 0 rows, so
  staff sees 'No active PCs to broadcast to.'"

---

# FILE: CustomerTimer.java (320 lines)

## 1. What this file does

This file is the customer window. One PC, one big countdown. It
asks the database every second "do I have an active session?", shows
the remaining time live, pops reminders at 30 and 5 minutes, pops
"Time's Up" at zero, shows counter broadcasts ("Notice from
Counter") when staff sends one, and shows "Waiting for counter to
start session." when there is nothing running.

## 2. How it fits in the system

`Login` opens it for customer logins, passing the username
(`new CustomerTimer(username)`). It READS `pcs` (once, to find
its PC id) and `sessions` plus its own broadcast flag (every
second). Its only write in the whole file is clearing its own
broadcast flag after showing a broadcast — everything else is
reading. The counter's Start is what makes its numbers move. Back
button returns to `Login`.

## 3. Auto-generated vs hand-written

- [NETBEANS-GENERATED] `initComponents()` (about lines 145–248,
  roughly 100 lines) and the variables block (lines 260–268). Same
  create-paint-arrange pattern as Login.
- [HAND-WRITTEN] Both constructors (13–15, 17–38), `resolvePcId`
  (46–72), `tick` (74–129), `checkBroadcast` (131–161),
  `clearBroadcast` (163–179), `formatHMS` (181–187), back handler
  (301–309).

## 4. Imports — line by line

    Line 1: import java.sql.*;
    → All JDBC classes. We run SELECT queries (find my PC, find my
      active session) every second.

    Line 2: import javax.swing.*;
    → All Swing classes. Window, labels, Timer (heartbeat), and
      JOptionPane popups.

## 5. Fields (class-level variables)

    private static final long serialVersionUID = 1L;
    → Version number 1. Same warning-silencer story as Login.

    private Integer pcId = null;
    → What it is: a whole-number object, empty at first (`Integer`
      with capital I can hold "no value", unlike plain `int`).
    → What it holds: this PC's database id (found once via
      resolvePcId). Stays null if the username matches no PC.
    → Why we need it: every tick asks "active session for pc_id =
      ?" — this is the ?.

    private Integer lastSessionId = null;
    → What it holds: the session row id currently on screen.
    → Why we need it: popups must fire ONCE per session. When a NEW
      session id appears, we reset all popup flags.

    private boolean warned30 = false;
    private boolean warned5 = false;
    private boolean timesUpShown = false;
    → What they are: true/false switches, all start false.
    → What they hold: "did I already show the 30-min / 5-min /
      Time's Up popup for THIS session?"
    → Why we need them: without them, the 5-minute popup would pop
      every second for 5 minutes straight.

    private javax.swing.Timer refreshTimer;
    → Same alarm-clock idea as Dashboard's. Rings every second,
      each ring runs `tick()`.

## 6. Methods — one section per method

### Method: public CustomerTimer()  [HAND-WRITTEN]

**What it does:**
No-argument version. Just calls the real constructor with a dummy
name. Exists so NetBeans-style code (`new CustomerTimer()`) still
works.

**Called from:**
Anywhere that creates the window without a username (rare; login
always passes one).

**Line-by-line:**

    Line 14: this("customer");
    → "Run my OTHER constructor with the name 'customer'." `this`
      here means "this same object, other version".

### Method: public CustomerTimer(String username)  [HAND-WRITTEN]

**What it does:**
Builds the window, sets resizable + minimum + dark background +
margin + fill (same recipe as Login), labels who is logged in,
finds its PC id, paints once, starts the heartbeat.

**Called from:**
`Login` after a customer login, with the typed username.

**Line-by-line:**

    Line 18: initComponents(); → NetBeans builds everything.
    Lines 19–25: setResizable(true) (same Design-safe override
    comment as Login), minimum 340×280, dark content background,
    16px margin, BorderLayout override with loginCard in CENTER
    (same fill trick as Login — frozen GroupLayout superseded,
    .form untouched).
    Line 26: backButton.setFocusPainted(false); → no dotted focus
    rectangle (looks only).
    Line 27: loginCard mauve outline (same as Login's card).
    Lines 28–30: if a real username was passed, label shows
    "Logged in as pc1" (example). Empty name → keep default text.
    Line 31: (comment) "Snug fit to content, then center."
    Line 32: pack(); → shrink-wrap open size.
    Line 33: setLocationRelativeTo(null); → center on screen.
    Line 34: resolvePcId(username); → find my database id now.
    Line 35: tick(); → first paint immediately (no 1-second wait).
    Lines 36–37: make the 1-second Timer calling tick(), start it.

**Key ideas in this method:**
- Same open recipe everywhere: build → override → margin → fill
  → pack → center → load → paint → heartbeat.

### Method: private void resolvePcId(String username)  [HAND-WRITTEN]

**What it does:**
Translates the login name ("pc1") into the database PC id once. If
nothing matches, labels the window "No PC linked to this account."

**Called from:**
The constructor, once.

**Line-by-line:**

    Lines 47–49: empty/null name → return immediately (nothing to
    look up).
    Line 50: SELECT id FROM pcs WHERE UPPER(pc_name) = UPPER(?)
    → "Give me the id whose PC name matches, ignoring capital
      letters." `UPPER` converts to capitals first, so "pc1" finds
      "PC1". The `?` is safely bound, same anti-cheat pattern.
    Lines 51–68: open connection (null → silent return; screen
    already shows waiting text); bind trimmed name; run; if a row
    found store its id in pcId; always close; on error print
    "Resolve PC Error" only.
    Lines 69–71: still null afterward → noteLabel becomes "No PC
    linked to this account."

### Method: private void tick()  [HAND-WRITTEN]

**What it does:**
One heartbeat: no PC id → show zeros; else ask for my active
session. None → waiting text. Found → live countdown + the three
one-time popups (30 min, 5 min, Time's Up).

**Called from:**
Constructor (first paint) and the timer (every second).

**Line-by-line:**

    Lines 75–78: pcId == null → timer "0:00:00", return. (resolvePcId
    already labeled the reason.)
    Line 79: SELECT id, end_time FROM sessions WHERE pc_id = ? AND
    status = 'active' AND is_archived = 0 ORDER BY id DESC LIMIT 1
    → "My newest active session, if any." Same shape as the
      counter's per-PC query.
    Lines 80–94: open (null → silent return); bind pcId; run; if a
    row: read session id + end_time; remaining = end minus computer
    clock now (floor at 0, never negative display).
    Lines 95–100: if this session id differs from lastSessionId (or
    first ever) → remember it + reset all three popup switches.
    This is what makes popups once-per-SESSION instead of
    once-forever.
    Line 101: paint the H:MM:SS countdown (via formatHMS below).
    Line 102: noteLabel → "Session in progress."
    Lines 103–105: remaining is 0 and Time's Up not yet shown → mark
    shown, popup "Time's Up! Please go to the counter."
    Lines 106–108: else 5 minutes or less (but above 0), not yet
    warned → mark warned, popup "5 minutes remaining. Please save
    your work."
    Lines 109–111: else between 30 and 5 minutes, not yet warned →
    mark warned, popup "30 minutes remaining."
    (Order matters: zero is checked first, then 5, then 30 — each
    `else if` skips the rest once one fires.)
    Lines 113–120: else branch (no active row) → forget session id,
    reset all switches (so the NEXT session warns fresh), show
    0:00:00 + "Waiting for counter to start session."
    Lines 121–127: always close; on error print "Timer Refresh
    Error" only (quiet loop by design).

    Line 128: checkBroadcast();
    → Run the broadcast check (next method) every tick, after the
      session and popup checks above. This one line links the
      countdown logic to the broadcast logic.

**Flag-clear-after-OK — read this before the next method:**
The broadcast flag is cleared on the CUSTOMER side (in
`clearBroadcast`, after the customer clicks OK), not on the
counter side. Why? Only the customer screen knows the exact moment
its human actually SAW the message. The counter sends to many PCs
at once and cannot know when each one displayed it. Clearing
locally after OK is what makes delivery exactly-once: seen means
cleared, and cleared means it never pops again.

**Key ideas in this method:**
- Almost-read-only screen. It never INSERTs, and its only UPDATE
  anywhere is the broadcast flag-clear (next methods). All real
  authority stays at the counter.
- Flags + session-id tracking = popups fire exactly once per
  session. This is the pattern to memorize for the panel.
- Closing this window stops only the DISPLAY. The session's
  end_time lives in MySQL, so time keeps running. Re-login shows
  the   true remaining time.

### Method: private void checkBroadcast()  [HAND-WRITTEN]

**What it does:**
Asks "is a counter broadcast waiting for MY pc?" If yes, pops it
as "Notice from Counter", then clears the flag so it shows exactly
once.

**Called from:**
`tick()`, every second (line 128), after the session and popup
checks.

**Line-by-line:**

    Lines 132–134: if (pcId == null) return; → no PC linked, nothing
    to check for. (resolvePcId already labeled the window.)

    Line 135: SELECT broadcast_pending, broadcast_message FROM pcs
    WHERE id = ?
    → "Give me MY row's flag and stored message." A separate small
      query on purpose: it runs even when there is no active
      session, so a flag set just as a session ended still resolves
      and clears instead of going stale and popping on the NEXT
      session.

    Line 136: String message = null; → empty hands to start; only a
    real flag fills them.

    Lines 137–151: open connection (null → silent return); bind my
    pcId; run; if a row exists AND its flag is 1, copy the stored
    text into `message`. (`rs.getInt(...) == 1` reads the flag as a
    number because TINYINT is stored as 0/1.) Always close.

    Lines 152–155: catch SQLException → print "Broadcast Error"
    only and return (quiet loop — same convention as the tick).

    Lines 156–158: if (message == null) return; → no broadcast
    waiting, back to the countdown, nothing shown.

    Line 159: JOptionPane.showMessageDialog(this, message, "Notice
    from Counter", JOptionPane.INFORMATION_MESSAGE);
    → Pop the message. The third argument ("Notice from Counter")
      is the dialog's title bar. INFORMATION_MESSAGE means the
      normal blue "i" icon. The connection is already closed at
      this point — we never hold a database line open while a human
      stares at a popup.

    Line 160: clearBroadcast(); → flag off, message wiped (next
    method), so this broadcast can never pop twice.

**Key ideas in this method:**
- Read, THEN show, THEN clear — in that order. Showing before
  reading is impossible (nothing to show); clearing before showing
  would lose the message if anything failed between.
- The flag lives per-PC on the `pcs` row, so each screen clears
  only itself. PC1 dismissing never affects PC2's popup.

### Method: private void clearBroadcast()  [HAND-WRITTEN]

**What it does:**
Wipes this PC's broadcast flag and message (both back to empty
defaults). The single UPDATE that makes delivery exactly-once.

**Called from:**
`checkBroadcast()`, right after the customer clicks OK.

**Line-by-line:**

    Line 164: UPDATE pcs SET broadcast_pending = 0,
    broadcast_message = '' WHERE id = ?
    → "On MY row only: flag down, message erased." (`''` is empty
      text.) The `WHERE id = ?` limits it to this PC — the classic
      one-row guard, same as Extend/Stop.

    Lines 165–179: open (null → silent return), bind my pcId, run,
    always close. Catch → print "Broadcast Error" only. No popup
    here: the customer already clicked OK and moved on; an error
    box now would confuse them, and the next tick simply retries.

**Key ideas in this method:**
- This is the file's ONLY write. Everything else here reads.
- If clearing ever fails, the flag stays up and the message pops
  again next tick — annoying but safe (never silently lost).

### Method: private String formatHMS(long millis)  [HAND-WRITTEN]

Identical twin of Dashboard's version (same math, same "H:MM:SS"
output). See Dashboard section for the line-by-line. Kept separate
in each file so neither window depends on the other.

### Method: private void initComponents()  [NETBEANS-GENERATED]

About 100 lines (196–299). Same pattern: create 7 components →
window settings (close on X, title "Customer PC", resizable false —
overridden later) → paint (obsidian panel, rose card, CUSTOMER 20
bold, countdown 36 bold rose, muted labels, rose Back button) →
card interior layout (title, big timer, user, note, 42px button,
28/32px gaps) → frozen centered outer GroupLayout (superseded at
runtime) → `pack()`. Variables block lines 311–319.

### Method: private void backButtonActionPerformed(...)  [HAND-WRITTEN]

**What it does:**
Stops the heartbeat, opens Login centered, closes this window.

**Called from:**
The "Back to Login" button.

**Line-by-line:**

    Lines 251–253: if (refreshTimer != null) refreshTimer.stop();
    → Silence the alarm before leaving. Without this it would tick
      forever on a closed window, wasting database calls.

    Lines 254–257: new Login(); center; show; this.dispose(); (close
    and clean up — same pattern as Login's own dispose).

## 7. What would break if we deleted this file?

Customer logins would crash (Login creates this window). There
would be no customer countdown screen at all.

## 8. Defense one-liners

- "How does the customer know its PC?" → "resolvePcId matches the
  username to pcs case-insensitively, once at startup."
- "How do the 30/5-minute popups fire only once?" → "Three boolean
  flags per session, reset whenever a new session id appears."
- "Does closing the customer window stop the timer?" → "No — only
  the display stops. end_time is in MySQL, so reopening shows the
  true remaining time."
- "Does the customer write to the database?" → "Only one tiny
  write: clearing its own broadcast flag after showing it.
  Everything else is SELECT."
- "What shows when the counter never started anything?" →
  "0:00:00 plus 'Waiting for counter to start session.'"
- "How does a broadcast reach this screen?" → "Each tick reads my
  pcs row's flag. Flag up means pop 'Notice from Counter', then
  clear my flag."
- "What if two broadcasts arrive close together?" → "Last message
  wins on the row; each screen still pops exactly once per flag."

---

## FEATURE FLOW — BROADCAST MESSAGE

How one staff message reaches every active customer screen, start
to finish. (Needs the two new `pcs` columns from the ALTER TABLE:
`broadcast_pending` starts at 0 = "nothing waiting",
`broadcast_message` starts empty.)

1. Staff clicks Broadcast on the counter.
   The new button sits in the top bar between the sales total and
   Logout. One click opens the typing dialog.

2. Staff types a message in the dialog and clicks OK.
   Title bar says "Broadcast Message". Cancel or empty text stops
   here silently — nothing touches the database.

3. App runs one UPDATE that flags every active PC.
   `UPDATE pcs SET broadcast_pending = 1, broadcast_message = ?
   WHERE id IN (SELECT pc_id FROM sessions WHERE status = 'active'
   AND is_archived = 0)`. Idle PCs are untouched. The changed-row
   count tells staff "Broadcast sent to X PC(s)." (or "No active
   PCs" when the shop is empty).

4. Each customer screen sees the flag on its next tick.
   Within about a second, every active PC's `checkBroadcast` reads
   flag = 1 on its own row and pops "Notice from Counter" with the
   stored text. Each screen works alone — PC1 never waits for PC2.

5. Customer dismisses the popup, flag clears on their side.
   Clicking OK runs the clear UPDATE for that PC row only
   (flag back to 0, message wiped). Clearing happens where the
   message was SEEN, because only that screen knows the human saw
   it — this is what makes delivery exactly-once.

6. Counter's next tick shows nothing — feature complete.
   Nothing on the counter changes after sending (the count popup
   was the whole feedback). The next broadcast starts fresh from
   all-zero flags.

WHY THE FLAG LIVES ON THE `pcs` TABLE: each PC needs its OWN copy
of "waiting + what text", and one row per PC already exists. One
UPDATE can then flag every active PC at once (no per-PC loop, no
half-finished sends), and each screen clears only its own row, so
screens never interfere. A separate table would need one row per
(PC, message) plus cleanup logic — more moving parts for zero
benefit at this scale. No history is kept by design: once every
flag is back to 0, no trace of the broadcast remains.
