# 🧠 AICodeEditor

**AICodeEditor** ist ein in **Kotlin Compose Multiplatform** entwickelter,  
intelligenter Code-Editor mit nativer **AI-Integration**.  
Er soll es ermöglichen, dass sowohl **Menschen** als auch **KI-Agenten**  
den gleichen Editor nahtlos bedienen können – ohne textbasierte Diff-Hacks  
oder unstrukturierte Patches.

---

## 🎯 Vision

AICodeEditor ist der erste **AI-native Code Editor**, der
strukturierte, transaktionale Änderungen unterstützt:

- **Menschen** bearbeiten Quelltext visuell und intuitiv.  
- **KI-Agenten** (z. B. über JSON-RPC / WebSocket) interagieren mit demselben Dokument,
  führen Operationen aus wie *InsertAfter(Class X)* oder *RenameSymbol(foo→bar)*  
  und committen diese Änderungen atomar.

Ziel ist eine Umgebung, in der KI nicht nur Code „vorschlägt“,  
sondern **real, sicher und semantisch korrekt** editieren kann.

---

## 🧩 System Overview

### 1. Core Engine
- **TextBuffer** (Piece Table oder Rope) für performantes Editing  
- **Cursor / Selection** Model  
- **Incremental Lexer + Parser** (line-basiert, state-aware)  
- **Undo / Redo** via Transaction-Log  

### 2. Rendering / UI
- **Compose Multiplatform + Skia**  
- **Viewport-Only Rendering** (10 000 Zeilen smooth)  
- **Syntax Highlighting** über Token Cache  
- **Line Numbers**, **Minimap**, **Gutter**, **Status Bar**

### 3. AI Interface
- **EditPlan API** (JSON / RPC)  
- **Atomic Transaction Model** (Commit / Rollback)  
- **Semantic Selectors** (AST, Regex, Symbol, Anchor)  
- **Constraint-System** (mustParse, forbidTouching, maxAddedBytes)  
- **Dry-Run / Preview Mode**  
- **Auto-Heal** (fehlende Klammern, Quotes etc.)

### 4. Human UX
- **Command Palette & Shortcuts**  
- **Timeline View** für AI-Operationen  
- **Animated Highlights** für geänderte Ranges  
- **Undo ganzer Plans**  
- **Diff Preview View**

### 5. Future Modules
- **Language Plugins** (Tokenizer / Parser DSL)  
- **LSP Integration** für Diagnostics & Completion  
- **Collaborative / Multi-Agent Editing**

---

## 🧱 Architektur

```
AICodeEditor
│
├── core/
│   ├─ TextBuffer.kt
│   ├─ Cursor.kt
│   ├─ EditTransaction.kt
│   └─ UndoManager.kt
│
├── syntax/
│   ├─ TokenizerDSL.kt
│   ├─ TokenCache.kt
│   └─ SyntaxHighlighter.kt
│
├── ai/
│   ├─ EditPlan.kt
│   ├─ Selector.kt
│   ├─ EditExecutor.kt
│   ├─ AutoHeal.kt
│   └─ JsonRpcServer.kt
│
├── parser/
│   ├─ IncrementalParser.kt
│   ├─ AstNode.kt
│   └─ SelectorResolver.kt
│
└── ui/
    ├─ EditorView.kt
    ├─ PlanTimeline.kt
    ├─ DiffPreview.kt
    ├─ CommandPalette.kt
    └─ RangeHighlightLayer.kt
```

---

## 🧮 Iterative Roadmap für GPT-5 Codex / Cline

Jede Iteration liefert einen funktionierenden Baustein.  
Codex soll pro Schritt kompilierbaren Code liefern, keine unreferenzierten Klassen,  
und jede Iteration abschließen mit „✅ Build passes, next goal: …“.

---

### 🟢 Iteration 1 – Core Text Engine
**Ziel:** Grundlegender TextBuffer mit Undo/Redo und Viewport-Rendering  

**Deliverables**
- `TextBuffer.kt` (PieceTable oder Rope)  
- `TextCursor.kt` (Position, Selection, Navigation)  
- `EditorState.kt` (immutable State für Compose)  
- `EditorView.kt` (Render nur sichtbare Zeilen)  
- `SyntaxHighlighter.kt` (Mock-Tokenizer)  
- Unit-Tests für Insert/Delete/Scroll/Undo  

---

### 🟢 Iteration 2 – Tokenization & Syntax
**Ziel:** Monarch-ähnlicher Tokenizer mit incremental Lexing  

**Deliverables**
- `TokenizerDSL.kt` (State + Regex-basierte Regeln)  
- `TokenCache.kt` (Line→Tokens + NextState)  
- Integration in `EditorView` für Syntaxfarben  
- Performance-Test (10 k Zeilen < 20 ms Repaint)

---

### 🟢 Iteration 3 – AI Edit Transaction API
**Ziel:** Strukturierte KI-Bearbeitung statt Diffs  

**Deliverables**
- `EditTransaction.kt` (Insert, Replace, Delete, Commit/Rollback)  
- `Selector.kt` (ByAst, ByRegex, BySymbol, ByAnchor)  
- `EditPlan.kt` (List of Ops + Constraints)  
- `EditExecutor.kt` (atomic apply + parser validation)  
- `JsonRpcServer.kt` (stdin/stdout oder WebSocket)  
- `AutoHeal.kt` (Einfache Klammer/Quote-Reparatur)

---

### 🟢 Iteration 4 – Semantic Selectors & Parser
**Ziel:** AST-basierte Selektion + Validierung  

**Deliverables**
- `IncrementalParser.kt` (better-parse oder eigene Implementierung)  
- `SelectorResolver.kt` (AST→Range)  
- `ConstraintChecker.kt` (Parse before commit)  
- `ParserDiagnostics.kt` (Error-Overlay im UI)

---

### 🟢 Iteration 5 – UX for AI and Human
**Ziel:** Interaktive Visualisierung der AI-Aktionen  

**Deliverables**
- `PlanTimeline.kt` (chronologische Ops-Darstellung)  
- `RangeHighlightLayer.kt` (Animierte Highlights)  
- `UndoManager.kt` (Plan-basiertes Undo)  
- `CommandPalette.kt` (Manuelles Auslösen von Ops)  
- `DiffPreview.kt` (Vor/Nach Vergleich)

---

### 🟢 Iteration 6 – LSP / Diagnostics Integration
**Ziel:** Syntaxprüfung und Completion über Language Server  

**Deliverables**
- `LspClient.kt` (LSP4J)  
- `DiagnosticsOverlay.kt` (Rote Wellenlinie + Tooltip)  
- `CompletionPopup.kt` (Autovervollständigung)

---

### 🟢 Iteration 7 – Plugin & Language System
**Ziel:** Externe Sprachen per DSL beschreibbar  

**Deliverables**
- `LanguagePlugin.kt` (SyntaxRules, Formatter, ParserFactory)  
- `LanguageRegistry.kt` (Register / Discover)  
- Beispiel-Plugin (z. B. für Kotlin-Lite)

---

## ⚙️ Design Guidelines für Codex (Cline)

- Eine Iteration = ein Pull Request / Codeblock  
- Code muss immer kompilierbar sein  
- Keine Referenzen auf nicht existierende Dateien  
- Am Ende jeder Iteration kurzer Status:  
  `✅ Build passes, next goal: <next iteration>`  

---

## 🧠 Langfristiges Ziel

AICodeEditor soll die Basis für zukünftige AI-Native Entwicklungsumgebungen werden,  
in denen KI und Mensch gemeinsam an Code, Logik und Kreativität arbeiten können.
