## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- If `graphify-out/graph.json` is missing (fresh clone — the large graph.json/graph.html are not committed), rebuild it by installing graphify (`uv tool install graphifyy`) and running `/graphify .`. This is fast and free: code is re-extracted structurally (AST, no LLM) and doc/image semantics are restored from the committed `graphify-out/cache/semantic/`.

## Claude Code iş akışı

- Ortam: Claude Code web oturumlarında Android SDK ve Gradle dağıtımını `.claude/hooks/session-start.sh` (SessionStart hook) kurar. `ANDROID_HOME` gerekiyorsa `$HOME/android-sdk` kullan.
- Kod değişikliğinden sonra, commit'ten önce `/verify` çalıştır (proje adımları: `.claude/skills/verify/SKILL.md` — derleme, unit testler, lint).
- Push'tan önce `/code-review` ile diff'i gözden geçir.
- `supabase/` şeması, kimlik doğrulama/login akışı veya anahtar/secret yönetimine dokunan değişikliklerde `/security-review` çalıştır.
