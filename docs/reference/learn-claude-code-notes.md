# learn-claude-code (shareAI-lab): what we took from it

Source: https://github.com/shareAI-lab/learn-claude-code (MIT). It is a 17-lesson course on building an agent *harness* (the code around a model: tools, skills, memory, permissions, hooks) in Python with the Anthropic API. Its code is for people building their own agent, and running it needs an API key, so it is **not installed** here. Claude Code already has these mechanisms. What we applied to this workspace:

| Lesson | Idea | Where it lives in this workspace |
|---|---|---|
| s07 Skill Loading | Keep only a short catalog in context; load the full skill when needed | `.claude/skills/*/SKILL.md` (Claude Code does this natively) |
| s09 Memory | Typed records (user / feedback / project / reference). Memory is background, not a command. Consolidate stale facts instead of piling up contradictions. | `docs/memory-notes.md` reorganised by type; the stale FRJP `AS3` fact was fixed in CLAUDE.md and the skill references |
| s05 / s10 Todo and Task System | Goals persist outside one conversation, with status | `docs/tasks.md` |
| s17 Goal Loop | "Done" needs a checkable completion condition plus evidence, not a claim | Every task in `docs/tasks.md` has a completion condition; rule in CLAUDE.md |
| s03 / s04 Permissions and Hooks | Allow rules and lifecycle hooks around tool calls | Allow rules go in `.claude/settings.json` (the user adds them; Claude can't change its own permissions) |

Not adopted: subagent teams, cron, worktrees, MCP plugin building. They aren't needed for daily PMS reporting.
