"""Verify isolated clients against a chosen published BBS host."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]
MARKER = '[Addon ports] REGISTRATION, EDITOR AND SERIALIZATION PASSED'
KIND = 'photon'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mc', action='append')
    parser.add_argument('--server', action='store_true', help='Verify common registration on an isolated dedicated server')
    parser.add_argument('--host', choices=['cml', 'fs', 'bbs2'], default='cml')
    args = parser.parse_args()
    side = 'server' if args.server else 'client'
    marker = '[Addon ports] DEDICATED SERVER PASSED' if args.server else MARKER
    specs = json.loads((ROOT / 'gradle/minecraft-targets.json').read_text())
    hosts = json.loads((ROOT / ('gradle/' + ('fs' if args.host == 'cml' else args.host) + '-hosts.json')).read_text())
    selected = args.mc or (hosts['targets'] if args.host != 'cml' else specs)
    java = str(Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if 'JAVA_HOME' in os.environ else shutil.which('java')
    results = []
    for mc in selected:
        if mc not in specs or (args.host != 'cml' and mc not in hosts['targets']):
            parser.error('Unsupported host/target: ' + args.host + '/' + mc)
        output = ROOT / 'build/validation' / mc
        source = output / (args.host + '-' + side + '-sources/ports/smoke/' + ('AddonServerSmoke.java' if args.server else 'AddonClientSmoke.java'))
        source.parent.mkdir(parents=True, exist_ok=True)
        text = (ROOT / ('tools/smoke/AddonServerSmoke.java' if args.server else 'tools/smoke/AddonClientSmoke.java')).read_text(encoding='utf-8')
        if args.host != 'cml':
            subprocess.run([shutil.which('python'), str(ROOT / 'tools/prepare_host_compat.py'), '--mc', mc, '--host', args.host], cwd=ROOT, check=True)
            text = text.replace(hosts['package'] + '.', hosts['package'] + '.' + args.host + '.')
        if KIND == 'photon' or mc.startswith('26.'):
            text = text.replace('client.currentScreen', 'client.screen').replace('client.scheduleStop()', 'client.stop()')
            text = text.replace('server.stop(false)', 'server.halt(false)')
        if args.host == 'bbs2' and not args.server:
            extra = (ROOT / 'tools/smoke/Bbs2ClientChecks.java').read_text(encoding='utf-8')
            extra = extra.replace(hosts['package'] + '.', hosts['package'] + '.bbs2.')
            source.with_name('Bbs2ClientChecks.java').write_text(extra, encoding='utf-8')
            text = text.replace('        require(UIFormEditor.createPanel(form)',
                                '        Bbs2ClientChecks.check(form);\n        require(UIFormEditor.createPanel(form)')
        if mc.startswith('26.') and not args.server:
            extra = (ROOT / 'tools/smoke/ModernPaletteChecks.java').read_text(encoding='utf-8')
            source.with_name('ModernPaletteChecks.java').write_text(extra, encoding='utf-8')
            text = text.replace('categories.setup();', 'ModernPaletteChecks.setup(categories);')
            text = text.replace('categories.getAllCategories()', 'ModernPaletteChecks.categories(categories)')
        source.write_text(text, encoding='utf-8')
        resources = output / (args.host + '-' + side + '-resources')
        resources.mkdir(parents=True, exist_ok=True)
        (resources / 'fabric.mod.json').write_text(json.dumps({'schemaVersion': 1, 'id': 'addon_port_smoke', 'version': '1.0.0',
            'environment': 'server' if args.server else 'client', 'entrypoints': {'main' if args.server else 'client': ['ports.smoke.AddonServerSmoke' if args.server else 'ports.smoke.AddonClientSmoke']}}), encoding='utf-8')
        command = [java, '-classpath', str(ROOT / 'gradle/wrapper/gradle-wrapper.jar'), 'org.gradle.wrapper.GradleWrapperMain',
                   'runServer' if args.server else 'runClient', '-PsmokeSide=' + side, '-Pmc=' + mc, '-PportSmoke', '-PsmokeHost=' + args.host,
                   '-Pgame_directory=' + str(output / (args.host + '-' + side)), '--console=plain']
        if KIND == 'aaa' and not mc.startswith('26.'):
            command.append('-PdistributionSmoke')
        jars = [p for p in (ROOT / 'build/targets' / mc / 'libs').glob('*.jar') if not any(s in p.name for s in ['-sources', '-dev', '-slim'])]
        if len(jars) != 1:
            parser.error('Build exactly one production release for ' + mc)
        production = ROOT / 'build/libs' / jars[0].name
        if not production.is_file():
            parser.error('Build the production release first: ' + str(production))
        before = hashlib.sha512(production.read_bytes()).hexdigest()
        cml_hosts = json.loads((ROOT / 'gradle/cml-hosts.json').read_text())
        host_spec = cml_hosts['targets'][mc] if args.host == 'cml' else hosts['targets'][mc]
        host_jar = ROOT / ('build/cml-hosts' if args.host == 'cml' else 'build/' + args.host + '-compat') / mc / host_spec['filename']
        if hashlib.sha512(host_jar.read_bytes()).hexdigest() != host_spec['sha512']:
            parser.error('Host distribution does not match its published SHA-512')
        if args.server:
            server_dir = output / (args.host + '-server')
            server_dir.mkdir(parents=True, exist_ok=True)
            (server_dir / 'eula.txt').write_text('eula=true\n')
            (server_dir / 'server.properties').write_text('online-mode=false\nserver-port=0\nlevel-type=minecraft:flat\nview-distance=2\nsimulation-distance=2\n')
        log_path = output / (args.host + '-' + side + '.log')
        print('Checking ' + side, args.host, mc, flush=True)
        with log_path.open('w', encoding='utf-8') as log:
            process = subprocess.Popen(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
            try:
                status = process.wait(timeout=360)
            except subprocess.TimeoutExpired:
                if os.name == 'nt':
                    subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
                else:
                    process.terminate()
                status = -1
        text = log_path.read_text(encoding='utf-8', errors='replace')
        passed = status == 0 and marker in text and 'BUILD SUCCESSFUL' in text[text.rfind(marker):]
        passed = passed and '[Addon host version] ' + host_spec['version'] in text
        passed = passed and hashlib.sha512(host_jar.read_bytes()).hexdigest() == host_spec['sha512']
        passed = passed and before == hashlib.sha512(production.read_bytes()).hexdigest()
        passed = passed and before == hashlib.sha512(jars[0].read_bytes()).hexdigest()
        results.append({'host': args.host, 'minecraft': mc, 'passed': passed, 'exit': status,
                        'production_sha512': before, 'host_version': host_spec['version'], 'host_sha512': host_spec['sha512'], 'log': str(log_path.relative_to(ROOT))})
        print('PASS' if passed else 'FAIL', args.host, mc, str(log_path), flush=True)
    (ROOT / ('build/validation/' + side + 's-' + args.host + '.json')).write_text(json.dumps(results, indent=4) + '\n', encoding='utf-8')
    if not all(r['passed'] for r in results):
        raise SystemExit(1)


if __name__ == '__main__':
    main()
