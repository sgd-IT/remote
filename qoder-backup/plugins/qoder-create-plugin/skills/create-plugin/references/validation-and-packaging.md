# Validation And Packaging

Try to validate generated plugins offline first. Use Qoder runtime checks only
when they add the proof the user asked for.

## Offline Validator

The loaded `SKILL.md` supplies the validator's absolute path after Cosy resolves
the directory containing that `SKILL.md`. Reuse that exact path; do not derive
it from the current working directory or search other plugin caches. Run:

```bash
<python-command> "<absolute-validator-script>" --json "<plugin-root-or-zip>"
```

Try `python3`, `python`, then Windows `py -3`, stopping at the first candidate
that starts and reports Python 3.7 or newer:

```bash
<python-command> -c "import sys; print('.'.join(map(str, sys.version_info[:3])))"
```

Do not install, upgrade, or download Python; modify `PATH`; create symlinks; or
search known installation directories. Record the selected command source and
version in the final summary.

The validator checks:

- `.qoder-plugin/plugin.json` exists and is valid JSON.
- Required manifest fields `name` and `version` exist.
- Manifest component paths stay inside the plugin root.
- Declared `skills`, `commands`, `mcpServers`, `hooks`, `rules`, `agents`, and
  `logo` point to existing files or directories.
- Every declared `SKILL.md` has frontmatter with `name` and `description`.
- Command sources exist and use Markdown files.
- MCP JSON exists and contains server definitions.
- Plugin zip roots contain `.qoder-plugin/plugin.json` directly.
- Local runtime state and non-Qoder manifest directories are not packaged.

If the bundled script is missing, distinguish that from a missing or unsupported
Python runtime. Follow the bounded structural review in `SKILL.md`; do not load
references, assets, scripts, or complete Skill bodies unless an observed
structural error requires them. Report the precise skip reason and state that
the deterministic offline validator was not run.

If the validator starts and reports plugin errors, fix them and rerun it. A
plugin validation error is not an environment failure and must not be silently
downgraded. Parse the JSON response: errors fail validation, while warnings keep
the exit status successful and must be reported as `passed with N warnings`.

Do not describe a later Qoder import as full runtime or content validation. A
successful JB import proves archive safety and basic manifest validity; it does
not validate every component declaration, Skill body, MCP/Hook behavior, or the
plugin's runtime behavior.

## Optional qodercli Compatibility Check

If `qodercli` is available, this is an additional compatibility check:

```bash
qodercli plugin validate "$(pwd)/<plugin-name>"
```

To verify project discovery from the plugin root before installation:

```bash
qodercli --cwd "$(pwd)/<plugin-name>" --setting-sources project skills list
```

The success signal is that each converted skill appears in the list.

Treat `qodercli plugin validate` as compatibility evidence, not as the package
quality gate. It may accept convention-only directories that do not contain
`.qoder-plugin/plugin.json`, and local install may still produce no discoverable
skills. The offline validator is intentionally stricter for generated packages:
it requires the Qoder manifest, declared component paths, and real `SKILL.md`
frontmatter before a plugin is considered ready.

## Optional Install Smoke Test

Run install smoke only when the user asks for install verification, trigger
verification, MCP/command discovery, or end-to-end proof.

Use a temporary cwd and local scope by default:

```bash
PLUGIN_ROOT="$(pwd)/<plugin-name>"
SMOKE_CWD="$(mktemp -d)"
qodercli --cwd "$SMOKE_CWD" plugin install --scope local "$PLUGIN_ROOT"
qodercli --cwd "$SMOKE_CWD" plugin list --json
```

Confirm:

- plugin name matches `<plugin-name>`
- status is enabled
- installed scope matches the requested scope
- version matches `.qoder-plugin/plugin.json`

Validate declared components:

```bash
# Skills
qodercli --cwd "$SMOKE_CWD" skills list

# MCP servers, only when plugin.json declares mcpServers and mcp.json exists
qodercli --cwd "$SMOKE_CWD" mcp list

# External commands, only when plugin.json declares commands and commands exist
qodercli --cwd "$SMOKE_CWD" external list
```

Success signals:

- Each copied skill appears in `qodercli skills list`.
- Each declared MCP server appears in `qodercli mcp list`.
- Each declared command appears in `qodercli external list`.

If an MCP server requires credentials or network access, discovery is enough
unless the user explicitly asks for invocation. If a command mutates files,
deploys, publishes, deletes, or spends credits, do not run it without explicit
approval.

Clean up temporary installs:

```bash
qodercli --cwd "$SMOKE_CWD" plugin uninstall --scope local <plugin-name>
```

Only leave the plugin installed when the user asked to keep it installed.

## Zip Packaging

Zip only when the user asks for a distributable archive:

```bash
cd <plugin-name>
zip -r "../<plugin-name>.zip" . \
  -x "**/.DS_Store" "__MACOSX/*" ".qoder/settings.local.json" ".qoder/**"
```

The zip root must contain `.qoder-plugin/plugin.json` directly.
Keep the version in the manifest, not the archive name. JB derives a custom
plugin's installation identity from the archive filename, so a stable
`<plugin-name>.zip` allows a later import to update the same plugin entry.

Try to validate the zip after creating it with the same resolved runtime and
absolute validator path. If it cannot run, report the exact skip reason without
promising that a later import will perform equivalent content validation:

```bash
<python-command> "<absolute-validator-script>" --json "<plugin-name>.zip"
```
