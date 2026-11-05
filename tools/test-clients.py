"""Run isolated native client initialization checks for each configured target."""
import argparse
import json
import os
from pathlib import Path
import subprocess
ROOT = Path(__file__).resolve().parents[1]
MARKER = '[Addon ports] REGISTRATION, EDITOR AND SERIALIZATION PASSED'
KIND = 'photon'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mc', action='append')
    args = parser.parse_args()
    specs = json.loads((ROOT / 'gradle/minecraft-targets.json').read_text())
    results = []
    for mc in args.mc or specs:
        if mc not in specs:
            parser.error('Unknown target: ' + mc)
        output = ROOT / 'build/validation' / mc
        source = output / 'sources/ports/smoke/AddonClientSmoke.java'
        source.parent.mkdir(parents=True, exist_ok=True)
        text = (ROOT / 'tools/smoke/AddonClientSmoke.java').read_text(encoding='utf-8')
        if KIND == 'photon' or mc.startswith('26.'):
            text = text.replace('client.currentScreen', 'client.screen').replace('client.scheduleStop()', 'client.stop()')
        source.write_text(text, encoding='utf-8')
        resources = output / 'resources'
        resources.mkdir(parents=True, exist_ok=True)
        (resources / 'fabric.mod.json').write_text(json.dumps({'schemaVersion': 1, 'id': 'addon_port_smoke', 'version': '1.0.0',
            'environment': 'client', 'entrypoints': {'client': ['ports.smoke.AddonClientSmoke']}}), encoding='utf-8')
        command = [str(ROOT / ('gradlew.bat' if os.name == 'nt' else 'gradlew')), 'runClient', '-Pmc=' + mc, '-PportSmoke', '--console=plain']
        if KIND == 'photon':
            command.append('-Pgame_directory=' + str(output / 'client'))
        log_path = output / 'client.log'
        print('Checking client', mc, flush=True)
        with log_path.open('w', encoding='utf-8') as log:
            process = subprocess.Popen(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
            try:
                status = process.wait(timeout=300)
            except subprocess.TimeoutExpired:
                if os.name == 'nt':
                    subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
                else:
                    process.terminate()
                status = -1
        text = log_path.read_text(encoding='utf-8', errors='replace')
        passed = status == 0 and MARKER in text and 'BUILD SUCCESSFUL' in text[text.rfind(MARKER):]
        results.append({'minecraft': mc, 'passed': passed, 'exit': status, 'log': str(log_path.relative_to(ROOT))})
        print('PASS' if passed else 'FAIL', mc, str(log_path), flush=True)
    (ROOT / 'build/validation/clients.json').write_text(json.dumps(results, indent=4) + '\n', encoding='utf-8')
    if not all(r['passed'] for r in results):
        raise SystemExit(1)


if __name__ == '__main__':
    main()
