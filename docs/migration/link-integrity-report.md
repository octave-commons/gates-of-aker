# Link Integrity Report (Task 7)

Date: 2026-02-10
Scope: `DOCS.md` and `docs/**/README.md`

## Deterministic Checks

### 1) Internal Link Validation (Wiki + Markdown)
Command:

```bash
python - <<'PY'
import re
from pathlib import Path
root=Path('/home/err/devel/orgs/octave-commons/gates-of-aker')
files=[root/'DOCS.md']+sorted((root/'docs').rglob('README.md'))
wiki_re=re.compile(r'\[\[([^\]]+)\]\]')
md_re=re.compile(r'\[[^\]]+\]\(([^)]+)\)')
unresolved=[]
checked=0
for f in files:
    txt=f.read_text()
    candidates=[]
    for m in wiki_re.finditer(txt):
        candidates.append(m.group(1).split('|',1)[0].strip())
    for m in md_re.finditer(txt):
        candidates.append(m.group(1).strip())
    for raw in candidates:
        if not raw or raw.startswith('http://') or raw.startswith('https://') or raw.startswith('#'):
            continue
        path=raw.split('#',1)[0]
        if not path or '://' in path or path.startswith('mailto:'):
            continue
        if path.startswith(('/', 'docs/', 'pseudo/', 'README.md', 'AGENTS.md', 'HACK.md', 'DOCS.md')):
            target=(root / path.lstrip('/')).resolve()
        else:
            target=(f.parent / path).resolve()
        checked+=1
        if not target.exists():
            unresolved.append((f.relative_to(root).as_posix(),raw))
print('files_checked',len(files))
print('links_checked',checked)
print('unresolved_count',len(unresolved))
for src,raw in unresolved:
    print('UNRESOLVED',src,'->',raw)
PY
```

Result:
- `files_checked=15`
- `links_checked=91`
- `unresolved_count=0`

### 2) Stale Legacy-Path Negative Check
Command:

```bash
python - <<'PY'
import re
from pathlib import Path
root=Path('/home/err/devel/orgs/octave-commons/gates-of-aker')
files=[root/'DOCS.md']+sorted((root/'docs').rglob('README.md'))
pat=re.compile(r'\[\[(docs/notes/|docs/tasks/|spec/)')
pat_md=re.compile(r'\]\((docs/notes/|docs/tasks/|spec/)')
violations=[]
for f in files:
    txt=f.read_text()
    for m in pat.finditer(txt):
        line=txt.count('\n',0,m.start())+1
        violations.append((f.relative_to(root).as_posix(),line,m.group(1)))
    for m in pat_md.finditer(txt):
        line=txt.count('\n',0,m.start())+1
        violations.append((f.relative_to(root).as_posix(),line,m.group(1)))
print('files_checked',len(files))
print('violation_count',len(violations))
for v in violations:
    print('VIOLATION',v[0],v[1],v[2])
PY
```

Result:
- `files_checked=15`
- `violation_count=0`

## Known Issues

- None in the checked index scope.
