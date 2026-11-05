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
    parser.add_argument('--host', choices=['cml', 'fs'], default='cml')
    args = parser.parse_args()
    specs = json.loads((ROOT / 'gradle/minecraft-targets.json').read_text())
    hosts = json.loads((ROOT / 'gradle/fs-hosts.json').read_text())
    selected = args.mc or (hosts['targets'] if args.host == 'fs' else specs)
    java = str(Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if 'JAVA_HOME' in os.environ else shutil.which('java')
    results = []
    for mc in selected:
        if mc not in specs or (args.host == 'fs' and mc not in hosts['targets']):
            parser.error('Unsupported host/target: ' + args.host + '/' + mc)
        output = ROOT / 'build/validation' / mc
        source = output / 'sources/ports/smoke/AddonClientSmoke.java'
        source.parent.mkdir(parents=True, exist_ok=True)
        text = (ROOT / 'tools/smoke/AddonClientSmoke.java').read_text(encoding='utf-8')
        if args.host == 'fs':
            subprocess.run([shutil.which('python'), str(ROOT / 'tools/prepare_fs.py'), '--mc', mc], cwd=ROOT, check=True)
            text = text.replace(hosts['package'] + '.', hosts['package'] + '.fs.')
        if KIND == 'photon' or mc.startswith('26.'):
            text = text.replace('client.currentScreen', 'client.screen').replace('client.scheduleStop()', 'client.stop()')
        source.write_text(text, encoding='utf-8')
        resources = output / 'resources'
        resources.mkdir(parents=True, exist_ok=True)
        (resources / 'fabric.mod.json').write_text(json.dumps({'schemaVersion': 1, 'id': 'addon_port_smoke', 'version': '1.0.0',
            'environment': 'client', 'entrypoints': {'client': ['ports.smoke.AddonClientSmoke']}}), encoding='utf-8')
        command = [java, '-classpath', str(ROOT / 'gradle/wrapper/gradle-wrapper.jar'), 'org.gradle.wrapper.GradleWrapperMain',
                   'runClient', '-Pmc=' + mc, '-PportSmoke', '-PsmokeHost=' + args.host,
                   '-Pgame_directory=' + str(output / (args.host + '-client')), '--console=plain']
        if KIND == 'aaa' and not mc.startswith('26.'):
            command.append('-PdistributionSmoke')
        jars = [p for p in (ROOT / 'build/targets' / mc / 'libs').glob('*.jar') if not any(s in p.name for s in ['-sources', '-dev', '-slim'])]
        if len(jars) != 1:
            parser.error('Build exactly one production release for ' + mc)
        production = ROOT / 'build/libs' / jars[0].name
        if not production.is_file():
            parser.error('Build the production release first: ' + str(production))
        before = hashlib.sha512(production.read_bytes()).hexdigest()
        log_path = output / (args.host + '-client.log')
        print('Checking client', args.host, mc, flush=True)
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
        passed = status == 0 and MARKER in text and 'BUILD SUCCESSFUL' in text[text.rfind(MARKER):]
        passed = passed and before == hashlib.sha512(production.read_bytes()).hexdigest()
        passed = passed and before == hashlib.sha512(jars[0].read_bytes()).hexdigest()
        results.append({'host': args.host, 'minecraft': mc, 'passed': passed, 'exit': status,
                        'production_sha512': before, 'log': str(log_path.relative_to(ROOT))})
        print('PASS' if passed else 'FAIL', args.host, mc, str(log_path), flush=True)
    (ROOT / ('build/validation/clients-' + args.host + '.json')).write_text(json.dumps(results, indent=4) + '\n', encoding='utf-8')
    if not all(r['passed'] for r in results):
        raise SystemExit(1)


if __name__ == '__main__':
    main()
