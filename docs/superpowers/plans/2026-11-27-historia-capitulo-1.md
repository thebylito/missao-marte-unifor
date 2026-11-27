# Capítulo 1 completo — Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans, task by task, with test-driven-development. Commit/push/merge remain subject to user approval.

**Goal:** Deliver M01, M02 and M03 as a complete playable chapter, with distinct encounters, persistent sequential progression and continuous Android validation.

**Architecture:** Keep the existing fixed-step flight and local campaign. Generalize mission selection and versioned progress, compose the encounter with a scanning sentry and an antenna objective, preserve M01 wrappers and all existing modes.

**Tech Stack:** Java 17, LibGDX, JUnit 5, Android, procedural Blender sprite sheets.

**Spec:** docs/superpowers/specs/2026-11-26-modo-historia.md. User narrowed delivery to Chapter 1 on 27/11; later chapters deferred.

## Global Constraints
- New sibling worktree from origin/main, one PR for this chapter stage; no branches deleted.
- No commit, push, merge, deploy or tag without permission. Headless Blender/emulator only.
- No secrets read or entered; no production mutation.
- M01 saves migrate without losing completion, best score or pending replay. Future formats remain protected.
- Every mission has a viable evasion route. No forced damage, physics teleport or scripted completion in acceptance tests.

## Review Focus
- Landing at base before antenna activation must not strand Rosa or complete M02.
- Leaving the antenna interrupts its continuous 3-second activation; death cannot retain partial activation.
- Completing/replaying one mission preserves every other best score and unlock state.
- Pausing/resizing clears inputs; touch targets remain reachable in landscape and portrait.
- M03 can be completed in either rescue order without filling the corridor with unavoidable hazards.

## Tasks
### Task 1: Mission catalogue and progression
Files: MissaoHistoria, ProgressoDaHistoria, HistoriaArquivo, ServicoHistoria and corresponding tests.
- [x] Add failing tests for M02/M03 locks, non-destructive replay, per-mission best score and v1 save migration.
- [x] Implement three mission descriptors and generic iniciar/concluir methods, keeping M01 methods compatible.
- [x] Read v1 and v2, write v2 with canonical CRC; preserve backup/future-version protections.
- [x] Run nucleus/core history suites, then full baseline tests.

### Task 2: Sentry and antenna objective
Files: SentinelaDeVarredura, EncontroDeResgate, PartidaDePilotagem, Caverna and tests.
- [x] Add failing tests for warning before attack, occlusion, EMP/recovery, objective-gated delivery and interrupted activation.
- [x] Compose optional sentry with the existing patrol/pulse/shield; add an empty installation landing platform and explicit delivery gate defaulting to open.
- [x] M02: Rosa at (1250,128), antenna platform (2100,328); hold 3 seconds with Rosa aboard, then return to base. Sentry at (1750,450), upper bypass at y=950.
- [x] M03: two rescue platforms at (1450,128) and (2700,428), one patrol and one sentry; lower shortcut and safe upper route at y=1050.
- [x] Run deterministic continuous-flight tests for both M03 orders and M02 activation.

### Task 3: Mission selection, briefing, result and radio
Files: TelaCampanha, TelaBriefingHistoria, TelaMissaoHistoria, TelaResultadoHistoria, MissaoMarteGame and UI tests.
- [x] Add failing tests for locked missions, selected briefing, next mission only after save, replay preservation and chapter-complete result.
- [x] Render three selectable cards, lock states, per-mission score and chapter completion; keyboard arrows/1–3 and touch select the same mission.
- [x] Generalize briefing and runtime objectives, guide antenna progress, show sentry warning and meaningful per-mission radio.
- [x] Result: next mission after successful save, separate replay action, chapter complete after M03. No future chapter advertised as playable.

### Task 4: Chapter art
Files: sentinela.py, antena_missao.py, two PNG sheets, SPECS/manifest/Folha/ORIGEM.
- [x] Produce headless Blender assets with distinct sentry silhouette and antenna off/activating/active cycles.
- [x] Verify dimensions, frame uniqueness, alpha and clipping; integrate in world render.

### Task 5: End-to-end validation and review
Files: validacao/historia/capitulo1, docs/evidencias/historia/capitulo-1.
- [x] Build APK/desktop and run all tests; scan minimum Android APIs.
- [x] Drive all three missions through normal UI, record video/screenshots, restart app, replay M02 and preserve whole chapter.
- [x] Review continuous runtime, collect fresh independent code review, fix blocking findings with regression tests.
- [x] Present exact completion/limits and ask for commit/push permission only after this stage is validated.
