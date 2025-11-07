# 🧠 AICodeEditor

**AICodeEditor** ist ein vollständig in **Kotlin Compose Multiplatform** entwickelter,
intelligenter Code-Editor mit nativer **AI-Integration** und **ohne BasicTextField**.

---

## 🚫 Warum kein BasicTextField

`BasicTextField` ist eine High-Level-Komponente von Compose, die auf TextInputService und
Plattform-IME aufbaut. Sie ist für einfache Editoren gedacht, **nicht** für
strukturierte, zeilenweise kontrollierte Editoren mit:

- eigener Zeichen- und Layoutlogik (Canvas / Paragraph)
- semantischem Cursor- und Selektionsmodell
- Syntax Highlighting auf Tokenbasis
- KI-Transaktionen (strukturierte EditPlans)
- Undo/Redo mit Transaktionsgruppen
- Millionen Zeilen ohne Reflow

### Probleme mit BasicTextField

1. **Unkontrollierbares Repaint** – ganze Textregion wird bei jedem Edit neu gezeichnet.  
2. **IME- und Clipboard-Bugs** – insbesondere bei Multiline und Undo.  
3. **Fehlende Transparenz** – Cursor-Position und Textlayout nur intern verfügbar.  
4. **Kein Token- oder Line-basiertes Rendering** – unbrauchbar für Syntax.  
5. **Performanceeinbruch** bei langen Dateien.  
6. **Ungeeignet für KI-Integration**, da Editorenoperationen nicht deterministisch steuerbar sind.

### Lösung: Canvas + Paragraph

AICodeEditor rendert jede Zeile manuell mit **Skia Paragraphs** auf einem **Canvas**.
Das erlaubt:

- **Volle Kontrolle** über Layout, Caching und Scrollverhalten.  
- **Viewport-basiertes Rendering** (nur sichtbare Zeilen).  
- **Eigener Cursor / Auswahl** mit Pixelgenauigkeit.  
- **AI-Overlays** für semantische Änderungen.  
- **Reproduzierbare Textoperationen** für KI-Agenten.

---

## 🎯 Vision

Ziel ist eine Umgebung, in der **Mensch und KI** denselben Editor benutzen können,
ohne Zwischenlayer wie HTML, DOM oder TextField-APIs.

- **Menschen** sehen eine native Compose-Oberfläche.
- **KI** steuert denselben Editor über JSON-RPC und strukturierte EditPlans.

Die Änderungen werden **atomar**, **parse-validiert** und **undo-fähig** ausgeführt.

---

## 🧩 System Overview

### 1. Core Engine
- `TextBuffer` (Piece Table oder Rope) für performantes Editing  
- `LineIndex` für Offset↔(line,col) Mapping  
- `UndoManager` für Transaktionen  
- `Parser` und `Tokenizer` (Monarch-ähnlich)  
- keine Abhängigkeit zu Compose

### 2. Rendering / UI
- Canvas-basiertes Rendering (Skia / Paragraph)  
- Viewport-only Darstellung  
- Syntax Highlighting  
- Caret + Selection Layer  
- AI-Overlays (Changes, Diagnostics, Suggestions)

### 3. AI Interface
- EditPlan API (strukturierte Operationen statt Diffs)  
- Commit/Rollback-Transaktionen  
- Constraints: `mustParse`, `forbidTouching`, `maxAddedBytes`  
- AutoHeal (Klammern, Quotes)  
- DryRun/Preview-Modus

### 4. UX Layer
- Command Palette  
- Timeline für KI-Aktionen  
- DiffPreview  
- Undo/Redo (Plan-basiert)

---

## ⚙️ Technische Umsetzung

### Rendering
- pro Zeile: `Paragraph` mit monospace Font
- Zeilenhöhe konstant (z. B. 20 px)
- horizontales Clipping statt Line-Wrap
- Paragraph-Cache mit Hash-Key `(lineHash, width, font)`

### Input
- Keyboard Handling über `onPreviewKeyEvent`
- eigene Keymap für Pfeiltasten, Enter, Backspace, Selection
- kein IME (später optional per Platform Bridge)

### Performance
- nur sichtbare Zeilen rendern (Viewport ± 2 Zeilen)
- incremental tokenization / lazy paragraph build
- Repaint auf Dirty Lines
- Speicher-Cache für 10 000 Zeilen unter 50 ms

---

## 🚀 Iterative Entwicklung (Claude / Codex / Cline)

### 🟢 Iteration 1 – Core
- `TextBuffer.kt` + `LineIndex.kt`
- Insert/Delete, Offset-Konvertierung, Tests

### 🟢 Iteration 2 – Canvas Rendering
- `EditorView.kt` + `TextPainter.kt`
- Paragraph-basiertes Zeichnen ohne BasicTextField

### 🟢 Iteration 3 – Keymap & Cursor
- `Keymap.kt` für Tastenlogik (ASCII)
- Cursorbewegung, Enter, Backspace, Insert

### 🟢 Iteration 4 – Selection & Highlight
- Auswahl mit Shift + Pfeilen
- Zeichnen der Markierung per Rect

### 🟢 Iteration 5 – Undo/Redo
- Transaktionales Edit-Log
- Coalescing von Tipparien

### 🟢 Iteration 6 – AI-Integration
- `EditPlan.kt`, `Selector.kt`, `EditExecutor.kt`
- JSON-RPC Schnittstelle

### 🟢 Iteration 7 – Diagnostics & LSP
- `LspClient.kt`
- Fehler- und Hover-Anzeige

---

## 🧱 Architekturübersicht

```
AICodeEditor
│
├── core/
│   ├─ TextBuffer.kt
│   ├─ LineIndex.kt
│   ├─ UndoManager.kt
│   └─ EditTransaction.kt
│
├── syntax/
│   ├─ TokenizerDSL.kt
│   └─ TokenCache.kt
│
├── ai/
│   ├─ EditPlan.kt
│   ├─ Selector.kt
│   ├─ EditExecutor.kt
│   ├─ JsonRpcServer.kt
│   └─ AutoHeal.kt
│
└── ui/
    ├─ EditorView.kt
    ├─ paint/TextPainter.kt
    ├─ input/Keymap.kt
    ├─ PlanTimeline.kt
    └─ DiffPreview.kt
```

---

## ✅ Ziel für Claude

Claude soll schrittweise:
1. **Core aufbauen** (TextBuffer + LineIndex)  
2. **Rendering umstellen** (Canvas/Paragraph)  
3. **Keymap hinzufügen**  
4. **Selection und Undo ergänzen**  
5. **AI-EditPlan-API implementieren**  

Jede Iteration liefert **kompilierbaren Code** ohne `BasicTextField`.

---

## 💬 Zusammenfassung

> **AICodeEditor ersetzt BasicTextField vollständig durch Canvas + Paragraph.**
>
> Dadurch wird der Editor deterministisch, performant und KI-fähig.
> Der gesamte Textfluss, das Layout und die Ereignissteuerung liegen vollständig in unserer Hand.

---

## 📦 Integration in dein Projekt

### 1. Installation via MavenLocal

Baue und installiere das Artefakt lokal:
```bash
./gradlew publishToMavenLocal
```

### 2. Abhängigkeit in deinem Projekt

Füge in deiner `build.gradle.kts` hinzu:
```kotlin
repositories {
    mavenLocal()
    // ggf. weitere Repositories
}

dependencies {
    implementation("ai.codeeditor:codeeditor:1.0.0")
}
```

### 3. EditorView verwenden

Beispiel für Compose Multiplatform:

```kotlin
import ai.codeeditor.core.EditorController
import ai.codeeditor.ui.EditorView

val buffer = SimpleTextBuffer("fun main() {\n    println(\"Hello World!\")\n}")
val controller = EditorController(buffer)

EditorView(controller = controller)
```

### 4. Desktop Main

```kotlin
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "AICodeEditor",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        val buffer = remember { SimpleTextBuffer("fun main() {\n    println(\"Hello World!\")\n}") }
        val controller = remember(buffer) { EditorController(buffer) }
        EditorView(controller = controller)
    }
}
```

### 5. Hinweise

- Die API ist Compose Multiplatform-kompatibel.
- Syntax Highlighting, Undo/Redo und Keymap sind integriert.
