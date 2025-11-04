Car Wash GUI (CS241 Assignment 2)

Overview
Operating Systems — Car Wash Assignment

Faculty of Computers and Artificial Intelligence, Cairo University
Academic Year: 2025-2026

Minimal code submission for the Operating Systems course.

Files

How to build
------------
Open PowerShell and run:

```powershell
cd 'C:\Users\ahmed\OneDrive\Desktop\3rdYear\OS\assignment2'
javac -Xlint:unchecked *.java
```

How to run
----------
Run the GUI:

```powershell
java CarWashGUI
```

Run the model test (headless):

```powershell
java CarWashModelTest
```

Next steps
----------
- Add unit tests using JUnit for more structured test reporting.
- Move simulation parameters out to a config file.
- Improve accessibility and layout responsiveness.

Notes
-----
All Swing GUI updates are performed on the Event Dispatch Thread. The model uses concurrent collections and per-pump locks to avoid race conditions. The controller runs the simulation in background threads and notifies the model when state changes.
