from pathlib import Path
import argparse
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--browser', action='store_true', help='also run the native Chromium reviewer-console suite')
args = parser.parse_args()

subprocess.run([sys.executable, str(ROOT / 'scripts/build.py')], check=True)
test_classes = ROOT / 'build/test-classes'
test_classes.mkdir(parents=True, exist_ok=True)
sources = sorted((ROOT / 'src/test/java').rglob('*.java'))
source_file = ROOT / 'build/test-sources.txt'
source_file.write_text('\n'.join(str(p) for p in sources), encoding='utf-8')
subprocess.run([
    'javac', '--release', '21', '-encoding', 'UTF-8', '-Xlint:all', '-Werror',
    '-cp', str(ROOT / 'build/classes'), '-d', str(test_classes), '@' + str(source_file)
], check=True, cwd=ROOT)
subprocess.run([
    'java', '-cp', str(ROOT / 'build/classes') + os.pathsep + str(test_classes),
    'io.ahad.mcpgateway.TestMain'
], check=True, cwd=ROOT)
subprocess.run([sys.executable, str(ROOT / 'tests/attack_matrix.py')], check=True, cwd=ROOT)
if args.browser:
    subprocess.run([sys.executable, str(ROOT / 'tests/browser_test.py')], check=True, cwd=ROOT)
subprocess.run([sys.executable, str(ROOT / 'scripts/repository_checks.py')], check=True, cwd=ROOT)
print('ALL_SELECTED_TESTS_PASSED')
