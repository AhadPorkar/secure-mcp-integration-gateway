from pathlib import Path
import subprocess, shutil
ROOT=Path(__file__).resolve().parents[1]
BUILD=ROOT/'build'; CLASSES=BUILD/'classes'
def main():
    if BUILD.exists(): shutil.rmtree(BUILD)
    CLASSES.mkdir(parents=True)
    sources=sorted((ROOT/'src/main/java').rglob('*.java'))
    srcfile=BUILD/'sources.txt'; srcfile.write_text('\n'.join(str(p) for p in sources),encoding='utf-8')
    subprocess.run(['javac','--release','21','-encoding','UTF-8','-Xlint:all','-Werror','-d',str(CLASSES),'@'+str(srcfile)],check=True,cwd=ROOT)
    print(f'Built {len(sources)} Java source files.')
if __name__=='__main__': main()
