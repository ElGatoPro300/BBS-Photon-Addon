"""Compile the isolated host implementation with its own Gradle cache and outputs."""
import argparse
import os
import shutil
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mc', required=True)
    parser.add_argument('--host', choices=['fs', 'bbs2'], default='fs')
    args = parser.parse_args()
    subprocess.run([sys.executable, str(ROOT / 'tools/prepare_fs.py'), '--mc', args.mc, '--host', args.host], cwd=ROOT, check=True)
    cache = ROOT / f'build/{args.host}-compat' / args.mc / 'gradle-cache'
    tasks = ['compileJava']
    if (ROOT / 'src/client/java').exists():
        tasks.append('compileClientJava')
    java = str(Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if 'JAVA_HOME' in os.environ else shutil.which('java')
    command = [java, '-classpath', str(ROOT / 'gradle/wrapper/gradle-wrapper.jar'), 'org.gradle.wrapper.GradleWrapperMain', *tasks,
               '-Pmc=' + args.mc, '-PcompatHost=' + args.host, '--project-cache-dir', str(cache), '--console=plain']
    log_path = ROOT / f'build/{args.host}-compat' / args.mc / 'compiler.log'
    with log_path.open('w', encoding='utf-8') as log:
        result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode:
        print(log_path.read_text(encoding='utf-8', errors='replace'))
        raise SystemExit(result.returncode)
    print(args.host.upper() + ' adapter compiled for ' + args.mc)


if __name__ == '__main__':
    main()
