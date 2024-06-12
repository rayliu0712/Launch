#!/usr/bin/env python3
''' https://github.com/rayliu0712/Launch 2024/06/12 '''
from concurrent.futures import ThreadPoolExecutor
from adbutils.errors import AdbError
from adbutils import ShellReturn, adb
from typing import List
from tqdm import tqdm
import subprocess
import asyncio
import hashlib
import shutil
import time
import sys
import os, os.path as op
import re

arg = sys.argv[1] if len(sys.argv) - 1 else None
device = None
target_path: str | List[str]
target_size: int
def transfer_fun(): pass
def completed_fun(): pass


''' handy function '''
class ShResult:
    def __init__(self, sr: ShellReturn):
        self.fail = sr.returncode
        self.succeed = not sr.returncode
        self.output = sr.output

def sh(cmd: str) -> ShResult:
    return ShResult(device.shell2(cmd, rstrip=True))

def runas(cmd: str) -> ShResult:
    return sh(f'run-as rl.launch {cmd}')

def pwd(path: str) -> ShResult:
    return sh(f'cd "{path}" && pwd')

def sha256(msg: str) -> str:
    return hashlib.sha256(bytes(f'{msg}', 'utf-8')).hexdigest()

def size(path: str) -> int:
    if not op.exists(path):
        return 0
    
    if not op.isdir(path):
        return op.getsize(path)
    
    total_size = 0
    for r, _, fs in os.walk(path):
        total_size += sum(op.getsize(op.join(r, f)) for f in fs)
    return total_size

def monitor_ts() -> int:
    if isinstance(target_path, str):
        # sh always succeed here
        sr = sh("find '%s' -type f -exec du -cb {} + | grep total$ | awk '{print $1}'" % target_path) 
        try:
            return int(sr.output)
        except ValueError:
            return 0
    else:
        return sum(size(t) for t in target_path)

async def tq():
    counter = 0
    with tqdm(total=target_size, unit='B', unit_scale=True, unit_divisor=1024) as pbar:
        while counter < target_size:
            n = monitor_ts() - counter
            counter += n
            pbar.update(n)
''' handy function '''



''' start '''
while device is None:
    dl = adb.device_list()
    dl_len = len(dl)

    if dl_len == 1:
        device = dl[0]
        continue

    print(f'\n{dl_len} device(s)')
    for i, it in enumerate(dl):
        print(f'[{i}] {it.prop.model}')

    try:
        device = dl[int(input('> ').strip())]
    except ValueError:
        pass
    except IndexError:
        pass

if arg is None:
    launch_list = []
    while 1:
        sr = runas("cat ./files/launch.txt")
        if sr.succeed:
            print('\r', ' '*20, end='\r', flush=True)

            launch_list = sr.output.splitlines()
            target_size = int(launch_list.pop(0))
            target_path = [ it.split('/')[-1] for it in launch_list ]  # relative path

            runas('touch ./files/key_a')
            break
        else:
            print('Waiting For Launch \\', end='\r', flush=True)
            time.sleep(0.25)
            print('Waiting For Launch |', end='\r', flush=True)
            time.sleep(0.25)
            print('Waiting For Launch /', end='\r', flush=True)
            time.sleep(0.25)
            print('Waiting For Launch -', end='\r', flush=True)
            time.sleep(0.25)

    for i, it in enumerate(launch_list):
        pure, ext = op.splitext(target_path[i])
        clone = ''
        for j in range(1, sys.maxsize):
            if op.exists(f'{pure}{clone}{ext}'):
                clone = f' ({j})'
            else:
                break
        target_path[i] = f'{pure}{clone}{ext}'

    def transfer_fun():
        for i, it in enumerate(launch_list):
            device.sync.pull(it, target_path[i])

    def completed_fun():
        for it in target_path:
            for r, _, fs in os.walk(it):
                [ os.remove(op.join(r, f)) for f in fs if f.startswith('.trashed') ]
        runas('touch ./files/key_b')

else:
    if not op.exists(arg):
        raise FileNotFoundError(f'檔案 "{arg}" 不存在')

    print('選擇Push目的地')
    print('[ Enter ] ./Download')
    print('[   1   ] ./Documents')
    print('[   2   ] ./Pictures')
    print('[   3   ]   AstroDX')
    print('[   4   ]   Custom')
    sdcard = ''
    chosen = ''

    while sdcard == '':
        chosen = input('> ').strip()
        match (chosen):
            case '':
                sdcard = '/sdcard/Download'
            case '1':
                sdcard = '/sdcard/Documents'
            case '2':
                sdcard = '/sdcard/Pictures'
            case '3':
                sdcard = '/sdcard/Android/data/com.Reflektone.AstroDX/files/levels'
                if pwd(sdcard).fail:
                    sdcard = ''
                    print('"./Android/data/com.Reflektone.AstroDX/files/levels" 不存在')
                    continue
            case '4':
                sdcard = '/sdcard'
            case _:
                continue

    if chosen in ['3', '4']:
        print('\n"ok" 選擇當前目錄；可以使用shell命令')
        while 1:
            cmd = input(f'{sdcard} > ')
            special = cmd.strip().lower()

            if special == 'ok':
                hd = sha256(f'{sdcard}{time.time()}')
                if sh(f'cd "{sdcard}"; touch {hd}; rm {hd}').succeed:
                    break
                else:
                    print('只能Push至 "/sdcard/..."')

            elif cmd.endswith('\\'):
                print('命令不能以 "\\" 結尾')
            
            elif special in ['clear', 'cls']:
                os.system('cls' if os.name == 'nt' else 'clear')
            
            elif cmd.startswith('cd '):
                input_dir = re.sub('^cd ', '', cmd).strip('"').strip("'")

                if not input_dir.startswith('/'):
                    input_dir = f'{sdcard}/{input_dir}'

                sr = pwd(input_dir)
                if sr.succeed:
                    sdcard = sr.output.replace('//', '/')
                else:
                    print(sr.output)

            else:
                out = sh(f'cd "{sdcard}"; {cmd}; pwd').output.splitlines()
                sdcard = out.pop().replace('//', '/')
                for o in out:
                    print(o)


    basename = op.basename(arg)
    target_size = size(arg)
    if op.isdir(arg):
        hd = sha256(f'{arg}{time.time()}')
        target_path = f'/sdcard/Download/{hd}'
        os.mkdir(hd)
        shutil.move(arg, hd)

        def transfer_fun():
            subprocess.check_output(f'adb push {hd} {target_path}')
        
        def completed_fun():
            sh(f'cd {target_path}; mv * "{sdcard}"; rmdir ../{hd}')
            shutil.move(op.join(hd, basename), arg)
            os.rmdir(hd)
        
    else:
        pure, ext = op.splitext(basename)
        clone = ''
        for j in range(1, sys.maxsize):
            if sh(f'ls "{sdcard}/{pure}{clone}{ext}"').succeed:
                clone = f' ({j})'
            else:
                break
        target_path = f'{sdcard}/{pure}{clone}{ext}'

        def transfer_fun():
            device.sync.push(arg, target_path)


async def main():
    loop = asyncio.get_running_loop()

    with ThreadPoolExecutor() as pool:

        transfer_task = loop.run_in_executor(pool, transfer_fun)
        await asyncio.gather(transfer_task, tq())
        completed_fun()

        for i in range(10, 0, -1):
            print(f'Exit After {i}s ', end='\r')
            time.sleep(1)

asyncio.run(main())
