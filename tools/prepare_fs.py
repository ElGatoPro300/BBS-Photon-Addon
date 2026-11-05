"""Fetch the pinned published FS host and generate isolated FS adapter sources.

Java string literals, comments and serialized resource identifiers are preserved.
Only Java package/type tokens are relocated. Host overrides replace canonical files.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
TOKEN = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mc', required=True)
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'gradle/fs-hosts.json').read_text())
    host = manifest['targets'][args.mc]
    prefix = manifest['package']
    destination = ROOT / 'build/fs-compat' / args.mc
    destination.mkdir(parents=True, exist_ok=True)
    jar = destination / host['filename']
    if not jar.exists() or hashlib.sha512(jar.read_bytes()).hexdigest() != host['sha512']:
        request = urllib.request.Request(host['url'], headers={'User-Agent': 'CML-addons/FS-compatibility'})
        with urllib.request.urlopen(request, timeout=90) as response:
            content = response.read()
        if hashlib.sha512(content).hexdigest() != host['sha512']:
            raise SystemExit('Published FS JAR checksum mismatch')
        jar.write_bytes(content)

    adapter = json.loads((ROOT / 'gradle/minecraft-targets.json').read_text())[args.mc]['adapter']
    sources = {}
    roots = [ROOT / 'src/main/java', ROOT / 'src/client/java',
             ROOT / f'src/platform/{adapter}/java', ROOT / f'src/platform/{adapter}/client/java',
             ROOT / 'src/platform/previewLegacy/client/java']
    for source_root in roots:
        for path in sorted(source_root.rglob('*.java')):
            relative = path.relative_to(source_root)
            if '/mixin/' in '/' + relative.as_posix() or '/compat/host/' in '/' + relative.as_posix():
                continue
            group = 'client' if '/client/java' in source_root.as_posix() else 'main'
            sources[(group, relative)] = path
    for group in ('main', 'client'):
        override_root = ROOT / f'src/hosts/fs/{group}/java'
        for path in override_root.rglob('*.java'):
            sources[(group, path.relative_to(override_root))] = path

    def relocate(match):
        token = match.group()
        return prefix + '.fs' + token[len(prefix):] if token == prefix or token.startswith(prefix + '.') else token

    emitted = set()
    for (group, relative), source in sources.items():
        name = relative.as_posix().replace(prefix.replace('.', '/') + '/', prefix.replace('.', '/') + '/fs/', 1)
        output = destination / 'sources' / group / 'java' / name
        content = TOKEN.sub(relocate, source.read_text(encoding='utf-8'))
        output.parent.mkdir(parents=True, exist_ok=True)
        if not output.exists() or output.read_text(encoding='utf-8') != content:
            output.write_text(content, encoding='utf-8')
        emitted.add(output.resolve())
    for stale in (destination / 'sources').rglob('*.java'):
        if stale.resolve() not in emitted:
            stale.unlink()
    print(f'FS {host["version"]}: verified published JAR; generated {len(emitted)} isolated classes')


if __name__ == '__main__':
    main()
