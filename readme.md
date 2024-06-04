```powershell
start PowerShell 'Set-ExecutionPolicy RemoteSigned' -Verb RunAs
cmd /c 'ftype Microsoft.PowerShellScript.1="%SystemRoot%\system32\WindowsPowerShell\v1.0\powershell.exe" "%1"'
```
