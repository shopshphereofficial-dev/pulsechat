# PulseChat — Phase 1

A chat app (Kotlin + Jetpack Compose) with:
- **Sign in / sign up with Google**
- **Unique username** claimed on first login
- **Find friends** by username, send/accept requests (like Instagram)

Chat (1-on-1 + group) and voice/video calls come in later phases.

## Backend (Supabase)
Project ref: `vildgvfcyhawojpmtdbe` (region: Mumbai / ap-south-1)

Tables (all with Row Level Security ON):
- `profiles` — id, username (unique), display_name, avatar_url
- `friendships` — requester_id, addressee_id, status (pending/accepted/declined)
- `conversations`, `conversation_members`, `messages` — ready for the chat phase

A trigger auto-creates a `profiles` row whenever a new user signs up.

The app talks to Supabase directly with OkHttp (REST + Auth API) — no SDK,
so there is no Kotlin-version conflict and it runs on old devices too.

## One-time setup you must do (for Google login)

1. Go to **console.cloud.google.com** → create/select a project.
2. **APIs & Services → OAuth consent screen** → External → fill app name + your
   email → Save.
3. **APIs & Services → Credentials → Create credentials → OAuth client ID**
   → Application type **Web application**.
   - Under **Authorized redirect URIs**, add:
     `https://vildgvfcyhawojpmtdbe.supabase.co/auth/v1/callback`
   - Create, then copy the **Client ID** and **Client secret**.
4. In this repo, open
   `app/src/main/java/com/pulsechat/app/data/Supabase.kt` and paste the
   **Client ID** into `GOOGLE_WEB_CLIENT_ID`.
5. In the **Supabase dashboard** → Authentication → Providers → **Google**:
   - Enable it, paste the **Client ID** and **Client secret**.
   - In **Authorized Client IDs**, add the same **Web Client ID** (needed for
     the Android ID-token sign-in flow).
6. Push to `main` → the APK builds automatically.

> The `ANON_KEY` in `Supabase.kt` is the **public** anon key. It is meant to be
> shipped inside client apps and is safe — your data is protected by the RLS
> policies on every table. It is not a secret.

## Device support
minSdk **24 (Android 7.0)** — covers the large majority of phones in use today.

## Build
Push to `main`; download the `pulsechat-apk` artifact from the workflow run.
