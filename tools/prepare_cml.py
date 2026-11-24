"""Resolve only the pinned published CML distribution, with integrity and host checks."""
import argparse
import hashlib
from io import BytesIO
import json
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def verify(content, host, target):
    if hashlib.sha512(content).hexdigest() != host['sha512']:
        raise ValueError('Published CML JAR checksum mismatch: ' + host['filename'])
    with zipfile.ZipFile(BytesIO(content)) as jar:
        metadata = json.loads(jar.read('fabric.mod.json'))
        if (metadata.get('id') != 'bbs' or metadata.get('version') != host['version']
                or metadata.get('custom', {}).get('bbs:fork_id') != 'CML'):
            raise ValueError('Published artifact is not the pinned CML host')
        if 'mchorse/bbs_mod/api/BBSAddonMod.class' not in jar.namelist():
            raise ValueError('Published CML host does not expose the addon API')
        manifest = jar.read('META-INF/MANIFEST.MF').decode('utf-8')
        attributes = dict(line.split(': ', 1) for line in manifest.splitlines() if ': ' in line)
        if (attributes.get('Fabric-Minecraft-Version') != target['cml']
                or attributes.get('Fabric-Mapping-Namespace') != target['namespace']):
            raise ValueError('Published CML Minecraft version or mapping namespace mismatch')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mc', required=True)
    args = parser.parse_args()
    hosts = json.loads((ROOT / 'gradle/cml-hosts.json').read_text())
    targets = json.loads((ROOT / 'gradle/minecraft-targets.json').read_text())
    if args.mc not in hosts['targets'] or args.mc not in targets:
        parser.error('Unsupported CML target: ' + args.mc)
    host = hosts['targets'][args.mc]
    target = targets[args.mc]
    if not host['version'].startswith(hosts['release'] + '-'):
        raise ValueError('CML target does not match the pinned beta release')
    if not host['url'].startswith('https://cdn.modrinth.com/data/' + hosts['project'] + '/'):
        raise ValueError('CML artifact must come from the pinned Modrinth project')
    if Path(host['filename']).name != host['filename']:
        raise ValueError('Invalid CML artifact filename')
    jar = ROOT / 'build/cml-hosts' / args.mc / host['filename']
    if jar.is_file():
        content = jar.read_bytes()
        if hashlib.sha512(content).hexdigest() == host['sha512']:
            verify(content, host, target)
            print(jar.resolve())
            return
    request = urllib.request.Request(host['url'], headers={'User-Agent': 'BBS-addons/published-CML'})
    with urllib.request.urlopen(request, timeout=60) as response:
        content = response.read()
    verify(content, host, target)
    jar.parent.mkdir(parents=True, exist_ok=True)
    temporary = jar.with_suffix('.jar.download')
    temporary.write_bytes(content)
    temporary.replace(jar)
    print(jar.resolve())


if __name__ == '__main__':
    main()
