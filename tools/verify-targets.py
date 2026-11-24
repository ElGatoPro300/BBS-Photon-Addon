"""Verify metadata, bytecode and dependency exclusion in collected addon JARs."""
import json
from pathlib import Path
import zipfile
import struct
from prepare_cml import verify
ROOT = Path(__file__).resolve().parents[1]
cml = json.loads((ROOT / 'gradle/cml-hosts.json').read_text())
specs = json.loads((ROOT / 'gradle/minecraft-targets.json').read_text())
hosts = {host: json.loads((ROOT / f'gradle/{host}-hosts.json').read_text()) for host in ('fs', 'bbs2')}
for mc, spec in specs.items():
    published = cml['targets'][mc]
    verify((ROOT / 'build/cml-hosts' / mc / published['filename']).read_bytes(), published, spec)
    jars = [p for p in (ROOT / 'build/targets' / mc / 'libs').glob('*.jar') if not any(s in p.name for s in ['-sources', '-dev', '-slim'])]
    assert len(jars) == 1, (mc, jars)
    jar = jars[0]
    assert jar.read_bytes() == (ROOT / 'build/libs' / jar.name).read_bytes(), mc
    with zipfile.ZipFile(jar) as z:
        meta = json.loads(z.read('fabric.mod.json'))
        assert meta['depends']['minecraft'] == '=' + mc, (mc, meta)
        assert meta['depends']['java'] == '>=' + str(spec['java'])
        names = z.namelist()
        prefix = hosts['fs']['package']
        supported = {host: manifest['targets'][mc] for host, manifest in hosts.items() if mc in manifest['targets']}
        expected = ['>=' + cml['release']] + ['=' + spec['version'] for spec in supported.values()]
        assert meta['depends']['bbs'] == (expected if supported else '>=' + cml['release'])
        for adapter in meta['languageAdapters'].values():
            assert adapter.replace('.', '/') + '.class' in names
        for key in ['bbs-addon', 'bbs-client-addon']:
            for entry in meta['entrypoints'][key]:
                assert entry['adapter'] in meta['languageAdapters']
                for host in hosts:
                    relocated = entry['value'].replace(prefix + '.', prefix + '.' + host + '.', 1).replace('.', '/') + '.class'
                    assert (relocated in names) == (host in supported), (mc, relocated)
        for name in names:
            if name.startswith(prefix.replace('.', '/') + '/fs/') and name.endswith('.class'):
                bytecode = z.read(name)
                for forbidden in [b'mchorse/bbs_mod/api/AddonContext', b'RegisterTrackPresentationEvent', b'api/replays/TrackPresentation']:
                    assert forbidden not in bytecode, (name, forbidden)
            if name.startswith(prefix.replace('.', '/') + '/bbs2/') and name.endswith('.class'):
                assert b'mchorse/bbs_mod/api/' not in z.read(name), name
        assert len(names) == len(set(names))
        assert not any(n.startswith(('net/minecraft/', 'mchorse/', 'mod/chloeprime/', 'com/lowdragmc/', 'ports/smoke/', 'META-INF/jars/')) for n in names)
        for group in meta.get('entrypoints', {}).values():
            for entry in group:
                classname = entry if isinstance(entry, str) else entry['value']
                assert classname.replace('.', '/') + '.class' in names, classname
        for mixin in meta.get('mixins', []):
            path = mixin if isinstance(mixin, str) else mixin['config']
            config = json.loads(z.read(path))
            for classname in config.get('mixins', []) + config.get('client', []) + config.get('server', []):
                assert (config['package'] + '.' + classname).replace('.', '/') + '.class' in names, classname
        for name in names:
            source = ROOT / 'src/main/resources' / name
            if name.startswith('assets/') and source.is_file():
                assert z.read(name) == source.read_bytes(), name
            if name.endswith('.class'):
                assert struct.unpack('>H', z.read(name)[6:8])[0] == spec['java'] + 44, name
    print('PASS production artifact', mc, jar.name)
