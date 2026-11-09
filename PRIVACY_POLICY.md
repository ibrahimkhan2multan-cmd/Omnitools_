# Privacy Policy for OmniTools

**Last updated:** November 4, 2026

OmniTools ("the App") is a free utility app by **ibrahimkhan2multan-cmd** ("the developer"). This policy explains what the App does with your information. The short version: **the App does not collect, send or share your data.**

## 1. No data collection

- The App does **not** declare the Android `INTERNET` permission, so it cannot send data over the network.
- No accounts, no sign-in, no advertising, no analytics, no crash-reporting service, no payments.
- The developer receives no information about you or your use of the App.

## 2. Data that stays on your device

These are stored only in the App's private storage and are removed when you clear the App's data or uninstall it:

- Notes you write in the Notepad, flashcards you create, tasks, and habit-tracker ticks and streaks.
- Favorite tools, recently used tools and your theme choice.
- QR/barcode scan history (if you keep it enabled; you can delete items or everything).
- The running timer's end time, so the alarm can still ring if the App is closed.

Generated passwords and UUIDs are created on your device and are not saved.

## 3. Permissions and why they are used

| Permission | Purpose |
|---|---|
| Camera | Only when you open the QR/barcode scanner. Frames are analysed on-device with Google ML Kit's bundled model; nothing is saved or sent. |
| Notifications | To show the "Time's up" alarm notification for the timer, and the playback controls (previous, play/pause, next, stop) while the Music Player is playing. |
| Foreground service (media playback), Wake lock, Vibrate | To play the alarm sound and vibrate while the timer rings, and to keep the Music Player playing when the App is in the background. Vibrate is also used for the optional tap feedback, which can be turned off in Settings. |
| Full-screen intent | To show the "Time's up" screen over the lock screen when a timer you set finishes. |
| Receive boot completed | To re-arm a running timer after the phone restarts. |

Every permission is used only for the feature listed. If you deny one, the rest of the App keeps working.

## 4. Files you choose

Tools such as PDF, image, ZIP and code editor tools open or save files only through Android's system file picker, and only the files you pick. Processing happens on your device. Files are not uploaded anywhere.

## 5. HTML/JS runner

The HTML/JS runner executes code you type or open inside a sandboxed web view with network access blocked. It cannot read your files or contacts.

## 6. Clipboard, sharing and links

When you copy or share a result, it goes to the system clipboard or to the app you choose. Other apps may be able to read the clipboard on some Android versions. Scanned links are never opened automatically; you are asked first, and the destination site's own policy applies once it opens.

## 7. Children

The App is a general utility, collects no personal information, and is not directed at children in a way that differs from adults.

## 8. Changes

If the App's behaviour changes (for example, a feature that needs network access), this policy will be updated before that release and the date above changed.

## 9. Contact

Questions: open an issue at https://github.com/ibrahimkhan2multan-cmd/Omnitools_/issues
