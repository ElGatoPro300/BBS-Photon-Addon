"""Verify metadata, bytecode and dependency exclusion in collected addon JARs."""
import json
from pathlib import Path
import zipfile
import struct
ROOT = Path(__file__).resolve().parents[1]
specs = json.loads((ROOT / 'gradle/minecraft-targets.json').read_text())
hosts = json.loads((ROOT / 'gradle/fs-hosts.json').read_text())
for mc, spec in specs.items():
    jars = [p for p in (ROOT / 'build/targets' / mc / 'libs').glob('*.jar') if not any(s in p.name for s in ['-sources', '-dev', '-slim'])]
    assert len(jars) == 1, (mc, jars)
    jar = jars[0]
    assert jar.read_bytes() == (ROOT / 'build/libs' / jar.name).read_bytes(), mc
    with zipfile.ZipFile(jar) as z:
        meta = json.loads(z.read('fabric.mod.json'))
        assert meta['depends']['minecraft'] == '=' + mc, (mc, meta)
        assert meta['depends']['java'] == '>=' + str(spec['java'])
        names = z.namelist()
        prefix = hosts['package']
        fs = hosts['targets'].get(mc)
        assert meta['depends']['bbs'] == (['>=3.0-beta-7', '=' + fs['version']] if fs else '>=3.0-beta-7')
        for adapter in meta['languageAdapters'].values():
            assert adapter.replace('.', '/') + '.class' in names
        for key in ['bbs-addon', 'bbs-client-addon']:
            for entry in meta['entrypoints'][key]:
                assert entry['adapter'] in meta['languageAdapters']
                relocated = entry['value'].replace(prefix + '.', prefix + '.fs.', 1).replace('.', '/') + '.class'
                assert (relocated in names) == bool(fs), (mc, relocated)
        for name in names:
            if name.startswith(prefix.replace('.', '/') + '/fs/') and name.endswith('.class'):
                bytecode = z.read(name)
                for forbidden in [b'mchorse/bbs_mod/api/AddonContext', b'RegisterTrackPresentationEvent', b'api/replays/TrackPresentation']:
                    assert forbidden not in bytecode, (name, forbidden)
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
