# Firebase CLI

This project is configured for Firebase CLI database rules deployment.

## Project

- Firebase project: `stepsync-d21c1`
- Realtime Database rules: `rules/database-rules.json`

## Commands

Use `npx.cmd` on Windows PowerShell because `npm.ps1` may be blocked by the local execution policy.

```powershell
npx.cmd firebase-tools login
npx.cmd firebase-tools projects:list
npx.cmd firebase-tools deploy --only database
```

If `firebase` is installed globally and available on `PATH`, the equivalent commands are:

```powershell
firebase login
firebase projects:list
firebase deploy --only database
```
