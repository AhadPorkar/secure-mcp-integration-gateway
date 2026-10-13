from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors = []
required = [
    'README.md', 'README.de.md', 'SECURITY.md', 'START-HERE.md',
    'docs/en/security.md', 'docs/de/security.md',
    '.github/workflows/ci.yml', '.github/workflows/browser-acceptance.yml', '.github/workflows/mcp-sdk-acceptance.yml', 'requirements-browser.txt', 'tests/browser_test.py',
]
for item in required:
    if not (ROOT / item).exists():
        errors.append('missing ' + item)

# English and German technical documents must have mirrored file names.
en = {p.name for p in (ROOT / 'docs/en').glob('*.md')}
de = {p.name for p in (ROOT / 'docs/de').glob('*.md')}
if en != de:
    errors.append(f'doc mirror mismatch en-only={sorted(en-de)} de-only={sorted(de-en)}')

# Local credentials and common private-key material must not be packaged.
for p in ROOT.rglob('*'):
    if not p.is_file() or 'build' in p.parts or 'target' in p.parts:
        continue
    rel = p.relative_to(ROOT).as_posix()
    if rel.startswith('.local/'):
        errors.append('packaged local secret file ' + rel)
        continue
    try:
        content = p.read_text(encoding='utf-8')
    except Exception:
        continue
    if p.name != 'repository_checks.py' and 'BEGIN PRIVATE KEY' in content:
        errors.append('private key material in ' + rel)

# Security project must not weaken browser CSP or persist reviewer tokens.
gateway = (ROOT / 'src/main/java/io/ahad/mcpgateway/GatewayServer.java').read_text(encoding='utf-8')
if "'unsafe-inline'" in gateway or "'unsafe-eval'" in gateway:
    errors.append('unsafe CSP directive present')
assets = (ROOT / 'src/main/java/io/ahad/mcpgateway/AdminAssets.java').read_text(encoding='utf-8')
if 'localStorage' in assets or 'sessionStorage' in assets:
    errors.append('admin token persistence API referenced')
if 'innerHTML' in assets:
    errors.append('admin dynamic innerHTML referenced')

# Release documentation must not claim the manual official-SDK workflow ran locally.
readme = (ROOT / 'README.md').read_text(encoding='utf-8')
if 'official mcp v2 acceptance: passed' in readme.lower():
    errors.append('unverified official SDK acceptance claim')

if errors:
    print('\n'.join('FAIL ' + e for e in errors))
    raise SystemExit(1)
print('REPOSITORY_CHECKS_PASSED=11')
